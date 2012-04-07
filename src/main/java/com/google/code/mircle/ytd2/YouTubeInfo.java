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
import java.io.UnsupportedEncodingException;
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

public class YouTubeInfo {

    public static class AgeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public AgeException() {
            super("Age restriction, account required");
        }
    }

    HashMap<VideoQuality, String> sNextVideoURL = new HashMap<VideoQuality, String>();
    String sTitle = null;

    // can contain a string that prepends the
    // filename
    String sFilenameResPart = null;
    // the video URL which we can use as fallback to
    // my wget call
    String s403VideoURL = null;
    // counted in downloadone() for the 3 webrequest
    // to one video
    int iRecursionCount = -1;
    String html;
    YTD2Base ytd2;

    String source;
    String sVideoURL = null;

    HttpURLConnection con;

    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 5000;

    public YouTubeInfo(YTD2Base ytd2, String input) {
        this.ytd2 = ytd2;
        this.source = input;
    }

    boolean downloadone(String sURL) throws Exception {
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
                rc = downloadone(this.sNextVideoURL.values().iterator().next());
                return (rc);
            }
            if (rc403) {
                this.sFilenameResPart = null;
                rc = downloadone(this.s403VideoURL);
            }
        } catch (NullPointerException npe) {
            // if an IllegalStateException was catched while calling
            // httpclient.execute(httpget) a NPE is caught here because
            // response.getStatusLine() == null
            this.sVideoURL = null;
        }

        {
            if (this.con.getContentType().toLowerCase().matches("^text/html(.*)")) {
            }
        }

        {
            // test if we got a webpage
            String sContentType = null;
            sContentType = this.con.getContentType().toLowerCase();
            if (sContentType.matches("^text/html(.*)")) {
                this.html = readHtml();
                extractHtmlInfo(html);
                // test if we got the binary content
            } else { // content-type is not video/
                rc = false;
                this.sVideoURL = null;
            }
        }

        return (rc);

    }

    String readHtml() {
        BufferedReader textreader = null;
        try {
            textreader = new BufferedReader(new InputStreamReader(con.getInputStream(),
                    con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding()));
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }

        StringBuilder contents = new StringBuilder();
        String line = "";
        while (line != null) {
            try {
                line = textreader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            contents.append(line + "\n");
        }
        return contents.toString();
    }

    /**
     * Add resolution video for specific youtube link.
     * 
     * @param s
     *            download source url
     * @return
     */
    boolean addVideo(VideoQuality vd, String s) {
        if (s != null) {
            sNextVideoURL.put(vd, s);
            return true;
        }

        return false;
    }

    void extractHtmlInfo(String html) throws IOException {
        Pattern age = Pattern.compile("(verify_age)");
        Matcher ageMatch = age.matcher(html);
        if (ageMatch.find())
            throw new AgeException();

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

                String[] fmtUrlPair = urlString.split("&itag=");
                String url = fmtUrlPair[0];
                String itag = fmtUrlPair[1];

                url = StringUtils.removeStart(url, "url=");
                url = URLDecoder.decode(url, "UTF-8");

                // 2011-08-20
                // key-value
                // exchanged
                url = url.replaceFirst("&quality=.*", "");
                // save
                // that
                // URL
                ssourcecodevideourls.put(itag, url);
            }

            // figure out what resolution-button is pressed now and fill
            // list with possible URLs
            switch (VideoQuality.p1080) {
            case p1080:
                // 37|22 - better quality first
                if (this.addVideo(VideoQuality.p1080, ssourcecodevideourls.get("37"))) {
                    break;
                }
            case p720:
                if (this.addVideo(VideoQuality.p720, ssourcecodevideourls.get("22"))) {
                    break;
                }
            case p480:
                // 35|34
                if (this.addVideo(VideoQuality.p480, ssourcecodevideourls.get("35"))) {
                    break;
                }
            case p360:
                if (this.addVideo(VideoQuality.p360, ssourcecodevideourls.get("34"))) {
                    break;
                }
            case p240:
                // 18|5
                if (this.addVideo(VideoQuality.p240, ssourcecodevideourls.get("18"))) {
                    break;
                }
            case p120:
                if (this.addVideo(VideoQuality.p120, ssourcecodevideourls.get("5"))) {
                    break;
                }
                break;
            default:
                this.sNextVideoURL = null;
                this.sVideoURL = null;
                this.sFilenameResPart = null;
                break;
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

    public void extract() {
        try {
            downloadone(source);
            this.iRecursionCount = -1;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
