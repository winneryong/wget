package com.github.axet.vget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class YouTubeInfo implements VGetInfo {

    public static class AgeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public AgeException() {
            super("Age restriction, account required");
        }
    }

    HashMap<VideoQuality, String> sNextVideoURL = new HashMap<VideoQuality, String>();
    String sTitle = null;

    String s403VideoURL = null;

    int iRecursionCount = -1;
    VGetBase ytd2;

    String source;
    String sVideoURL = null;

    public YouTubeInfo(VGetBase ytd2, String input) {
        this.ytd2 = ytd2;
        this.source = input;
    }

    public static boolean probe(String url) {
        return url.contains("youtube.com");
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
        HttpURLConnection con;
        con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(VGetThread.CONNECT_TIMEOUT);
        con.setReadTimeout(VGetThread.READ_TIMEOUT);

        if (!(rc = con.getResponseCode() == 200) & !(rc204 = con.getResponseCode() == 204)
                & !(rc302 = con.getResponseCode() == 302) & !(rc403 = con.getResponseCode() == 403)) {
            return (rc & rc204 & rc302);
        }
        if (rc204) {
            rc = downloadone(this.sNextVideoURL.values().iterator().next());
            return (rc);
        }
        if (rc403) {
            rc = downloadone(this.s403VideoURL);
        }

        {
            // test if we got a webpage
            String sContentType = null;
            sContentType = con.getContentType().toLowerCase();
            if (sContentType.matches("^text/html(.*)")) {
                String html;
                html = readHtml(con);
                extractHtmlInfo(html);
            } else { // content-type is not video/
                rc = false;
                this.sVideoURL = null;
            }
        }

        return (rc);

    }

    String readHtml(HttpURLConnection con) {
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
     */
    void addVideo(VideoQuality vd, String s) {
        sNextVideoURL.put(vd, s);
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

        Pattern encod = Pattern.compile("\"url_encoded_fmt_stream_map\": \"([^\"]*)\"");
        Matcher encodMatch = encod.matcher(html);
        if (this.iRecursionCount == 0 && encodMatch.find()) {

            HashMap<String, String> ssourcecodevideourls = new HashMap<String, String>();

            String sline = encodMatch.group(1);

            String[] urlStrings = sline.split("url=");

            for (String urlString : urlStrings) {
                urlString = StringEscapeUtils.unescapeJava(urlString);

                Pattern link = Pattern.compile("(.*)&quality=(.*)&fallback_host=(.*)&type=(.*)itag=(\\d+)");
                Matcher linkMatch = link.matcher(urlString);
                if (linkMatch.find()) {

                    String url = linkMatch.group(1);
                    String itag = linkMatch.group(5);

                    url = URLDecoder.decode(url, "UTF-8");

                    ssourcecodevideourls.put(itag, url);
                }
            }

            // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
            switch (VideoQuality.p1080) {
            case p1080:
                // 37|22 - better quality first
                addVideo(VideoQuality.p1080, ssourcecodevideourls.get("37"));
                addVideo(VideoQuality.p1080, ssourcecodevideourls.get("46"));
            case p720:
                addVideo(VideoQuality.p720, ssourcecodevideourls.get("22"));
            case p480:
                // 35|34
                addVideo(VideoQuality.p480, ssourcecodevideourls.get("35"));
            case p360:
                addVideo(VideoQuality.p360, ssourcecodevideourls.get("34"));
            case p240:
                // 18|5
                addVideo(VideoQuality.p240, ssourcecodevideourls.get("18"));
            case p120:
                addVideo(VideoQuality.p120, ssourcecodevideourls.get("5"));
                break;
            default:
                this.sNextVideoURL = null;
                this.sVideoURL = null;
                break;
            }
        }

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

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getTitle() {
        return sTitle;
    }

    @Override
    public VideoURL getVideo() {
        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p1080, VideoQuality.p720, VideoQuality.p480,
                VideoQuality.p360, VideoQuality.p240, VideoQuality.p120 };

        for (int i = 0; i < avail.length; i++) {
            if (sNextVideoURL.containsKey(avail[i]))
                return new VideoURL(avail[i], sNextVideoURL.get(avail[i]));
        }

        throw new RuntimeException("no video with required quality found");
    }

}
