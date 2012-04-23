package com.github.axet.vget.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.github.axet.vget.VGetBase;
import com.github.axet.vget.info.VGetInfo.VideoQuality;

public class YouTubeInfo implements VGetInfo {

    public static class AgeException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public AgeException() {
            super("Age restriction, account required");
        }
    }

    public static class EmbeddingDisabled extends DownloadError {
        private static final long serialVersionUID = 1L;

        public EmbeddingDisabled(String msg) {
            super(msg);
        }
    }

    HashMap<VideoQuality, String> sNextVideoURL = new HashMap<VideoQuality, String>();

    String sTitle = null;

    VGetBase ytd2;

    String source;

    public YouTubeInfo(VGetBase ytd2, String input) {
        this.ytd2 = ytd2;
        this.source = input;
    }

    public static boolean probe(String url) {
        return url.contains("youtube.com");
    }

    void downloadone(String sURL) throws Exception {
        try {
            extractEmbedded();
        } catch (EmbeddingDisabled e) {
            streamCpature(sURL);
        }
    }

    void streamCpature(String sURL) throws Exception {
        URL url = new URL(sURL);
        HttpURLConnection con;
        con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(VGetBase.CONNECT_TIMEOUT);
        con.setReadTimeout(VGetBase.READ_TIMEOUT);

        String sContentType = null;
        sContentType = con.getContentType().toLowerCase();
        if (sContentType.matches("^text/html(.*)")) {
            String html;
            html = readHtml(con);
            extractHtmlInfo(html);
        }
    }

    String readHtml(HttpURLConnection con) {
        BufferedReader textreader = null;
        try {
            textreader = new BufferedReader(new InputStreamReader(con.getInputStream(),
                    con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding()));
        } catch (IOException e1) {
            throw new DownloadRetry(e1);
        }

        StringBuilder contents = new StringBuilder();
        String line = "";

        try {
            while ((line = textreader.readLine()) != null) {
                contents.append(line + "\n");
            }
        } catch (IOException e) {
            throw new DownloadRetry(e);
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
        if (s != null)
            sNextVideoURL.put(vd, s);
    }

    void addVideo(Integer itag, String url) {
        switch (itag) {
        case 38:
            // mp4
            addVideo(VideoQuality.p1080, url);
            break;
        case 37:
            // mp4
            addVideo(VideoQuality.p1080, url);
            break;
        case 46:
            // webm
            addVideo(VideoQuality.p1080, url);
            break;
        case 22:
            // mp4
            addVideo(VideoQuality.p720, url);
            break;
        case 45:
            // webm
            addVideo(VideoQuality.p720, url);
            break;
        case 35:
            // mp4
            addVideo(VideoQuality.p480, url);
            break;
        case 44:
            // webm
            addVideo(VideoQuality.p480, url);
            break;
        case 18:
            // mp4
            addVideo(VideoQuality.p360, url);
            break;
        case 34:
            // flv
            addVideo(VideoQuality.p360, url);
            break;
        case 43:
            // webm
            addVideo(VideoQuality.p360, url);
            break;
        case 6:
            // flv
            addVideo(VideoQuality.p270, url);
            break;
        case 5:
            addVideo(VideoQuality.p224, url);
            break;
        }
    }

    void extractEmbedded() throws Exception {
        Pattern u = Pattern.compile("youtube.com/.*v=([^&]*)");
        Matcher um = u.matcher(source);
        if (!um.find()) {
            throw new RuntimeException("unknown url");
        }
        String id = um.group(1);

        String get = String
                .format("http://www.youtube.com/get_video_info?video_id=%s&el=embedded&ps=default&eurl=", id);

        URL url = new URL(get);
        HttpURLConnection con;
        con = (HttpURLConnection) url.openConnection();

        con.setConnectTimeout(VGetBase.CONNECT_TIMEOUT);
        con.setReadTimeout(VGetBase.READ_TIMEOUT);

        String qs = readHtml(con);
        Map<String, String> map = getQueryMap(qs);
        if (map.get("status").equals("fail"))
            throw new EmbeddingDisabled(URLDecoder.decode(map.get("reason"), "UTF-8"));

        sTitle = URLDecoder.decode(map.get("title"), "UTF-8");
        // String fmt_list = URLDecoder.decode(map.get("fmt_list"), "UTF-8");
        // String[] fmts = fmt_list.split(",");
        String url_encoded_fmt_stream_map = URLDecoder.decode(map.get("url_encoded_fmt_stream_map"), "UTF-8");

        extractUrlEncodedVideos(url_encoded_fmt_stream_map);
    }

    public static Map<String, String> getQueryMap(String qs) {
        try {
            qs = qs.trim();
            List<NameValuePair> list;
            list = URLEncodedUtils.parse(new URI(null, null, null, 0, null, qs, null), "UTF-8");
            HashMap<String, String> map = new HashMap<String, String>();
            for (NameValuePair p : list) {
                map.put(p.getName(), p.getValue());
            }
            return map;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    void extractHtmlInfo(String html) throws Exception {
        Pattern age = Pattern.compile("(verify_age)");
        Matcher ageMatch = age.matcher(html);
        if (ageMatch.find())
            throw new AgeException();

        Pattern gen = Pattern.compile("\"(http(.*)generate_204(.*))\"");
        Matcher genMatch = gen.matcher(html);
        if (genMatch.find()) {
            String sline = genMatch.group(1);

            sline = StringEscapeUtils.unescapeJava(sline);
        }

        // normal embedded video, unable to grab age restricted videos
        Pattern encod = Pattern.compile("\"url_encoded_fmt_stream_map\": \"url=([^\"]*)\"");
        Matcher encodMatch = encod.matcher(html);
        if (encodMatch.find()) {
            String sline = encodMatch.group(1);

            extractUrlEncodedVideos(sline);
        }

        // stream video
        Pattern encodStream = Pattern.compile("\"url_encoded_fmt_stream_map\": \"stream=([^\"]*)\"");
        Matcher encodStreamMatch = encodStream.matcher(html);
        if (encodStreamMatch.find()) {
            String sline = encodStreamMatch.group(1);

            String[] urlStrings = sline.split("stream=");

            for (String urlString : urlStrings) {
                urlString = StringEscapeUtils.unescapeJava(urlString);

                Pattern link = Pattern.compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),");
                Matcher linkMatch = link.matcher(urlString);
                if (linkMatch.find()) {

                    String sparams = linkMatch.group(1);
                    String itag = linkMatch.group(2);
                    String url = linkMatch.group(3);

                    url = "http" + url + "?" + sparams;

                    url = URLDecoder.decode(url, "UTF-8");

                    addVideo(Integer.decode(itag), url);
                }
            }
        }

        Pattern title = Pattern.compile("<meta name=\"title\" content=(.*)");
        Matcher titleMatch = title.matcher(html);
        if (titleMatch.find()) {
            String sline = titleMatch.group(1);
            String name = sline.replaceFirst("<meta name=\"title\" content=", "").trim();
            name = StringUtils.strip(name, "\">");
            name = StringEscapeUtils.unescapeHtml4(name);
            this.sTitle = name;
        }
    }

    void extractUrlEncodedVideos(String sline) throws Exception {
        String[] urlStrings = sline.split("url=");

        for (String urlString : urlStrings) {
            urlString = StringEscapeUtils.unescapeJava(urlString);

            Pattern link = Pattern.compile("(.*)&quality=(.*)&fallback_host=(.*)&type=(.*)itag=(\\d+)");
            Matcher linkMatch = link.matcher(urlString);
            if (linkMatch.find()) {
                String url = linkMatch.group(1);
                String itag = linkMatch.group(5);

                url = URLDecoder.decode(url, "UTF-8");

                addVideo(Integer.decode(itag), url);
            }
        }
    }

    public void extract() {
        try {
            downloadone(source);
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
    public Map<VideoQuality, String> getVideos() {
        return sNextVideoURL;
    }

}
