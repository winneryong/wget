package com.github.axet.wget.info;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.axet.wget.WGet;

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
        try {
            extractRange();
        } catch (RuntimeException e) {
            extractNormal();
        }
    }

    // if range failed - do plain download with no retrys's
    protected void extractRange() {
        try {
            URL url = source;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(WGet.CONNECT_TIMEOUT);
            conn.setReadTimeout(WGet.READ_TIMEOUT);

            conn.setRequestProperty("Range", "bytes=" + 0 + "-" + 0);

            String range = conn.getHeaderField("Content-Range");

            Pattern p = Pattern.compile("bytes \\d+-\\d+/(\\d+)");

            Matcher m = p.matcher(range);
            if (m.find()) {
                length = new Long(m.group(1));
            } else {
                throw new RuntimeException("not supported");
            }

            contentType = conn.getContentType();

            this.range = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // if range failed - do plain download with no retrys's
    protected void extractNormal() {
        try {
            URL url = source;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(WGet.CONNECT_TIMEOUT);
            conn.setReadTimeout(WGet.READ_TIMEOUT);

            contentType = conn.getContentType();
            int len = conn.getContentLength();
            if (len >= 0) {
                length = new Long(len);
            }
            range = false;
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

}
