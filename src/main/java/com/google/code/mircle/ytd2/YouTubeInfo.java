/**
 *  This file is part of ytd2
 *
 *  ytd2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ytd2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with ytd2.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.google.code.mircle.ytd2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.code.mircle.ytd2.YTD2.VideoQuality;

/**
 * knoedel@section60:~/YouTube Downloads$ url=`wget --save-cookies
 * savecookies.txt --keep-session-cookies --output-document=-
 * http://www.youtube.com/watch?v=9QFK1cLhytY 2>/dev/null | grep
 * --after-context=6 --max-count=1 yt.preload.start | grep img.src | sed -e
 * 's/img.src =//' -e 's/generate_204/videoplayback/' -e 's/\\\u0026/\&/g' -e
 * 's/\\\//g' -e 's/;//g' -e "s/'//g" -e 's/ //g' -e 's/"//g' ` && wget
 * --load-cookies=savecookies.txt -O videofile.flv ${url} && echo ok || echo nok
 * 
 * works without cookies as well
 * 
 */
public class YouTubeInfo {

    // info values
    long iBytesMax = 0;
    // basically the same as
    // Thread.isInterrupted()
    boolean bisinterrupted = false;
    // one video web resource
    String sVideoURL = null;
    // will be used as filename
    String sTitle = null;
    String sContentType = null;

    boolean bDEBUG;
    boolean bNODOWNLOAD;
    static int iThreadcount = 0;
    // every download thread
    // get its own number
    int iThreadNo = YouTubeInfo.iThreadcount++;
    final String ssourcecodeurl = "http://";
    final String ssourcecodeuri = "[a-zA-Z0-9%&=\\.]";
    // main URL (youtube start web page)
    String sURL = null;
    // can contain a string that prepends the
    // filename
    String sFilenameResPart = null;
    // the video URL which we can use as fallback to
    // my wget call
    String s403VideoURL = null;
    // list of URLs from
    // webpage source
    // CookieStore bcs = null; // contains cookies after first HTTP GET
    Vector<String> sNextVideoURL = new Vector<String>();
    // counted in downloadone() for the 3 webrequest
    // to one video
    int iRecursionCount = -1;
    String html;
    BufferedReader textreader = null;
    YTD2Base ytd2;
    VideoQuality max;

    String input;
    boolean join = false;
    VideoQuality vq;

    HttpURLConnection con;

    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 5000;

    public YouTubeInfo(YTD2Base ytd2, String input, VideoQuality max) {
        this.bDEBUG = false;
        this.ytd2 = ytd2;
        this.input = input;
        this.max = max;
    }

    boolean downloadone(String sURL, VideoQuality vd) throws Exception {
        boolean rc = false;
        boolean rc204 = false;
        boolean rc302 = false;
        boolean rc403 = false;

        this.iRecursionCount++;

        // stop recursion
        try {
            if (sURL.equals(""))
                return (false);
        } catch (NullPointerException npe) {
            return (false);
        }
        if (ytd2.getbQuitrequested())
            return (false);

        URL url = new URL(sURL);
        con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(CONNECT_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);

        try {
            if (!(rc = this.con.getResponseCode() == 200) & !(rc204 = this.con.getResponseCode() == 204)
                    & !(rc302 = this.con.getResponseCode() == 302) & !(rc403 = this.con.getResponseCode() == 403)) {
                return (rc & rc204 & rc302);
            }
            if (rc204) {
                rc = downloadone(this.sNextVideoURL.get(0), vd);
                return (rc);
            }
            if (rc403) {
                this.sFilenameResPart = null;
                rc = downloadone(this.s403VideoURL, vd);
            }
        } catch (NullPointerException npe) {
            // if an IllegalStateException was catched while calling
            // httpclient.execute(httpget) a NPE is caught here because
            // response.getStatusLine() == null
            this.sVideoURL = null;
        }

        InputStream entity = null;
        entity = this.con.getInputStream();

        // try to read HTTP response body
        if (entity != null) {
            {
                if (this.con.getContentType().toLowerCase().matches("^text/html(.*)")) {
                    this.textreader = new BufferedReader(new InputStreamReader(entity,
                            con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding()));
                }
            }

            {
                // test if we got a webpage
                this.sContentType = this.con.getContentType().toLowerCase();
                if (this.sContentType.matches("^text/html(.*)")) {
                    this.html = readHtml();
                    extractHtmlInfo(html, vd);
                    // test if we got the binary content
                } else if (this.sContentType.matches("video/(.)*")) {
                    reportheaderinfo();
                    return false;
                } else { // content-type is not video/
                    rc = false;
                    this.sVideoURL = null;
                }
            }
        }

        rc = downloadone(this.sVideoURL, vd);

        return (rc);

    }

    String readHtml() {
        StringBuilder contents = new StringBuilder();
        String line = "";
        while (line != null) {
            try {
                line = this.textreader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            contents.append(line + "\n");
        }
        return contents.toString();
    }

    void reportheaderinfo() {
        iBytesMax = this.con.getContentLength();
    }

    /**
     * Add resolution video for specific youtube link.
     * 
     * @param s
     *            download source url
     * @return
     */
    boolean addVideo(String s) {
        if (s != null) {
            sNextVideoURL.add(s);
            return true;
        }

        return false;
    }

    void extractHtmlInfo(String html, VideoQuality vd) throws IOException {
        Pattern age = Pattern.compile("(verify_age)");
        Matcher ageMatch = age.matcher(html);
        if (ageMatch.find())
            throw new RuntimeException("Age restriction, account required");

        Pattern gen = Pattern.compile("\"(http(.*)generate_204(.*))\"");
        Matcher genMatch = gen.matcher(html);
        if (this.iRecursionCount == 0 && genMatch.find()) {
            String sline = genMatch.group(1);

            sline = StringEscapeUtils.unescapeJava(sline);

            this.s403VideoURL = sline.replaceFirst("generate_204", "videoplayback");
            this.sVideoURL = sline;
        }

        // 2011-03-08 - source code changed from "var swfHTML" to
        // "var swfConfig"
        // 2011-07-30 - source code changed from "var swfConfig"
        // something else .. we now use fmt_url_map as there are the
        // URLs to vidoes with formatstrings
        // 2011-08-20 - source code changed from "fmt_url_map": to
        // "url_encoded_fmt_stream_map":
        Pattern encod = Pattern.compile("\"url_encoded_fmt_stream_map\": \"([^\"]*)\"");
        Matcher encodMatch = encod.matcher(html);
        if (this.iRecursionCount == 0 && encodMatch.find()) {

            HashMap<String, String> ssourcecodevideourls = new HashMap<String, String>();

            String sline = encodMatch.group(1);
            // by anonymous

            String[] urlStrings = sline.split(",");

            for (String urlString : urlStrings) {
                urlString = StringEscapeUtils.unescapeJava(urlString);
                urlString = URLDecoder.decode(urlString, "UTF-8");
                urlString = StringUtils.removeStart(urlString, "url=");

                // 2011-08-20
                // \\|
                String[] fmtUrlPair = urlString.split("&itag=");
                // 2011-08-20
                // key-value
                // exchanged
                fmtUrlPair[0] = fmtUrlPair[0].replaceFirst("&quality=.*", "");
                try {
                    // save
                    // that
                    // URL
                    ssourcecodevideourls.put(fmtUrlPair[1], fmtUrlPair[0]);
                    // TODO add unknown resolutions (43-45,84?)
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    // TODO there is a new problem with itag=84 (not
                    // &itag=84)
                }
            }

            // figure out what resolution-button is pressed now and fill
            // list with possible URLs
            switch (vd) {
            case p1080:
                // 37|22 - better quality first
                if (this.addVideo(ssourcecodevideourls.get("37"))) {
                    vq = VideoQuality.p1080;
                    break;
                }
            case p720:
                if (this.addVideo(ssourcecodevideourls.get("22"))) {
                    vq = VideoQuality.p720;
                    break;
                }
            case p480:
                // 35|34
                if (this.addVideo(ssourcecodevideourls.get("35"))) {
                    vq = VideoQuality.p480;
                    break;
                }
            case p360:
                if (this.addVideo(ssourcecodevideourls.get("34"))) {
                    vq = VideoQuality.p360;
                    break;
                }
            case p240:
                // 18|5
                if (this.addVideo(ssourcecodevideourls.get("18"))) {
                    vq = VideoQuality.p240;
                    break;
                }
            case p120:
                if (this.addVideo(ssourcecodevideourls.get("5"))) {
                    vq = VideoQuality.p120;
                    break;
                }
                break;
            default:
                this.sNextVideoURL = null;
                this.sVideoURL = null;
                this.sFilenameResPart = null;
                break;
            }

            // remove null entries in list - we later try to download
            // the first (index 0) and if it fails the next one (at
            // index 1) and so on
            for (int x = this.sNextVideoURL.size() - 1; x >= 0; x--) {
                if (this.sNextVideoURL.get(x) == null)
                    this.sNextVideoURL.remove(x);
            }

            // 2011-03-08 new - skip generate_204
            this.sVideoURL = this.sNextVideoURL.get(0);
        }

        // TODO exchange HTML characters to UTF-8 =
        // http://sourceforge.net/projects/htmlparser/
        Pattern title = Pattern.compile("<meta name=\"title\" content=(.*)");
        Matcher titleMatch = title.matcher(html);
        if (this.iRecursionCount == 0 && titleMatch.find()) {
            String sline = titleMatch.group(1);
            String name = sline.replaceFirst("<meta name=\"title\" content=", "").trim();
            name = StringUtils.strip(name, "\">");
            name = StringEscapeUtils.unescapeHtml4(name);
            this.sTitle = name;
        }
    }

    String getProxy() {
        String sproxy = YTD2.sproxy;
        if (sproxy == null)
            return ("");
        else
            return (sproxy);
    }

    String getHost(String sURL) {
        String shost = sURL.replaceFirst(YTD2.szYTHOSTREGEX, "");
        shost = sURL.substring(0, sURL.length() - shost.length());
        shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
        return (shost);
    }

    String getMyName() {
        return this.getClass().getName().concat(Integer.toString(this.iThreadNo));
    }

    public void setbDEBUG(boolean bDEBUG) {
        this.bDEBUG = bDEBUG;
    }

    public void extract() {
        try {
            // TODO check what kind of website the URL is from - this class
            // can only handle YouTube-URLs ... we add other video sources
            // later
            this.sURL = input;

            // copy ndl-state
            // because this thread
            // should end with a
            // complete file (and
            // report so) even if
            // someone switches to
            // nodl before this
            // thread is finished
            this.bNODOWNLOAD = ytd2.getbNODOWNLOAD();

            // download one webresource and show result
            downloadone(this.sURL, max);
            this.iRecursionCount = -1;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        join = true;
    }

}
