package com.github.axet.vget.info;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.github.axet.vget.VGetBase;

public class VimeoInfo implements VGetInfo {

    HashMap<VideoQuality, String> sNextVideoURL = new HashMap<VideoQuality, String>();
    String sTitle = null;

    VGetBase ytd2;

    String source;

    public VimeoInfo(VGetBase ytd2, String input) {
        this.ytd2 = ytd2;
        this.source = input;
    }

    public static boolean probe(String url) {
        return url.contains("vimeo.com");
    }

    void downloadone(String sURL) throws Exception {
        Pattern u = Pattern.compile("vimeo.com/(\\d+)");
        Matcher um = u.matcher(sURL);
        if (!um.find()) {
            throw new RuntimeException("unknown url");
        }
        String id = um.group(1);
        String clip = "http://www.vimeo.com/moogaloop/load/clip:" + id;

        URL url = new URL(clip);
        HttpURLConnection con;
        con = (HttpURLConnection) url.openConnection();

        con.setConnectTimeout(VGetBase.CONNECT_TIMEOUT);
        con.setReadTimeout(VGetBase.READ_TIMEOUT);

        String xml = readHtml(con);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
        String sig = doc.getElementsByTagName("request_signature").item(0).getTextContent();
        String exp = doc.getElementsByTagName("request_signature_expires").item(0).getTextContent();

        sTitle = doc.getElementsByTagName("caption").item(0).getTextContent();

        String get = String.format("http://www.vimeo.com/moogaloop/play/clip:%s/%s/%s/?q=", id, sig, exp);
        String hd = get + "hd";
        String sd = get + "sd";

        sNextVideoURL.put(VideoQuality.p1080, hd);
        sNextVideoURL.put(VideoQuality.p480, sd);
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
        try {
            while ((line = textreader.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
