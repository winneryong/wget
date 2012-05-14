package com.github.axet.wget.info;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.axet.wget.Direct;

/**
 * URLInfo - keep all information about source in one place. Thread safe.
 * 
 * @author axet
 * 
 */
public class URLInfo {

    // source url
    private URL source;

    // null if size is unknown, which means we unable to restore downloads or do
    // multi thread downlaods
    private Long length;

    // does server support for the range param?
    protected boolean range;

    // null if here is no such file or other error
    private String contentType;

    // come from Content-Disposition: attachment; filename="fname.ext"
    private String contentFilename;

    public URLInfo(URL source) {
        this.source = source;
    }

    /**
     * does range supported?
     * 
     * @return
     */
    synchronized public boolean range() {
        if (getLength() == null)
            return false;
        return range;
    }

    synchronized public void extract() {
        HttpURLConnection conn;
        try {
            conn = extractRange();
        } catch (RuntimeException e) {
            conn = extractNormal();
        }

        contentType = conn.getContentType();

        String contentDisposition = conn.getHeaderField("Content-Disposition");
        if (contentDisposition != null) {
            // support for two forms with and without quotes:
            // 1) contentDisposition="attachment;filename="ap61.ram"";
            // 2) contentDisposition="attachment;filename=ap61.ram";

            Pattern cp = Pattern.compile("filename=[\"]*([^\"]*)[\"]*");
            Matcher cm = cp.matcher(contentDisposition);
            if (cm.find())
                contentFilename = cm.group(1);
        }
    }

    // if range failed - do plain download with no retrys's
    protected HttpURLConnection extractRange() {
        try {
            URL url = source;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
            conn.setReadTimeout(Direct.READ_TIMEOUT);

            // may raise an exception if not supported by server
            conn.setRequestProperty("Range", "bytes=" + 0 + "-" + 0);

            String range = conn.getHeaderField("Content-Range");
            if (range == null)
                throw new RuntimeException("range not supported");

            Pattern p = Pattern.compile("bytes \\d+-\\d+/(\\d+)");
            Matcher m = p.matcher(range);
            if (m.find()) {
                length = new Long(m.group(1));
            } else {
                throw new RuntimeException("range not supported");
            }

            this.range = true;

            return conn;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // if range failed - do plain download with no retrys's
    protected HttpURLConnection extractNormal() {
        try {
            URL url = source;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
            conn.setReadTimeout(Direct.READ_TIMEOUT);

            range = false;

            int len = conn.getContentLength();
            if (len >= 0) {
                length = new Long(len);
            }

            return conn;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized public String getContentType() {
        return contentType;
    }

    synchronized public Long getLength() {
        return length;
    }

    synchronized public URL getSource() {
        return source;
    }

    public String getContentFilename() {
        return contentFilename;
    }

}
