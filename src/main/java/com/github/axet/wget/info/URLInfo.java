package com.github.axet.wget.info;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.axet.wget.Direct;
import com.github.axet.wget.RetryWrap;
import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadRetry;

/**
 * URLInfo - keep all information about source in one place. Thread safe.
 * 
 * @author axet
 * 
 */
public class URLInfo {
    /**
     * source url
     */
    private URL source;

    /**
     * have been extracted?
     */
    private boolean extract = false;

    /**
     * null if size is unknown, which means we unable to restore downloads or do
     * multi thread downlaods
     */
    private Long length;

    /**
     * does server support for the range param?
     */
    protected boolean range;

    /**
     * null if here is no such file or other error
     */
    private String contentType;

    /**
     * come from Content-Disposition: attachment; filename="fname.ext"
     */
    private String contentFilename;

    /**
     * Notify States
     */
    public enum States {
        EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, STOPPED, ERROR, DONE;
    }

    /**
     * download state
     */
    private States state;
    /**
     * downloading error / retry error
     */
    private Throwable exception;
    /**
     * retrying delay;
     */
    private int delay;

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
            extract(new AtomicBoolean(false), new Runnable() {
                @Override
                public void run() {
                }
            });
        } catch (InterruptedException e) {
            throw new DownloadError(e);
        }
    }

    synchronized public void extract(final AtomicBoolean stop, final Runnable notify) throws InterruptedException {
        HttpURLConnection conn;

        conn = RetryWrap.wrap(stop, new RetryWrap.WrapReturn<HttpURLConnection>() {
            @Override
            public HttpURLConnection run() throws IOException {
                try {
                    return extractRange();
                } catch (DownloadRetry e) {
                    throw e;
                } catch (RuntimeException e) {
                    return extractNormal();
                }
            }

            @Override
            public void notifyRetry(int d, Throwable ee) {
                setState(States.RETRYING, ee);
                setDelay(d);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                setState(States.EXTRACTING);
                notify.run();
            }
        });

        contentType = conn.getContentType();

        String contentDisposition = conn.getHeaderField("Content-Disposition");
        if (contentDisposition != null) {
            // i support for two forms with and without quotes:
            //
            // 1) contentDisposition="attachment;filename="ap61.ram"";
            // 2) contentDisposition="attachment;filename=ap61.ram";

            Pattern cp = Pattern.compile("filename=[\"]*([^\"]*)[\"]*");
            Matcher cm = cp.matcher(contentDisposition);
            if (cm.find())
                contentFilename = cm.group(1);
        }

        extract = true;

        setState(States.EXTRACTING_DONE);
        notify.run();
    }

    synchronized public boolean empty() {
        return !extract;
    }

    // if range failed - do plain download with no retrys's
    protected HttpURLConnection extractRange() throws IOException {
        URL url = source;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
        conn.setReadTimeout(Direct.READ_TIMEOUT);

        // may raise an exception if not supported by server
        conn.setRequestProperty("Range", "bytes=" + 0 + "-" + 0);

        int code = conn.getResponseCode();
        switch (code) {
        case HttpURLConnection.HTTP_OK:
        case HttpURLConnection.HTTP_PARTIAL:
            break;
        default:
            throw new DownloadError(conn.getResponseMessage());
        }

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
    }

    // if range failed - do plain download with no retrys's
    protected HttpURLConnection extractNormal() throws IOException {
        URL url = source;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(Direct.CONNECT_TIMEOUT);
        conn.setReadTimeout(Direct.READ_TIMEOUT);

        range = false;

        int code = conn.getResponseCode();
        switch (code) {
        case HttpURLConnection.HTTP_OK:
        case HttpURLConnection.HTTP_PARTIAL:
            break;
        default:
            throw new DownloadError(conn.getResponseMessage());
        }

        int len = conn.getContentLength();
        if (len >= 0) {
            length = new Long(len);
        }

        return conn;
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

    synchronized public String getContentFilename() {
        return contentFilename;
    }

    synchronized public States getState() {
        return state;
    }

    synchronized public void setState(States state) {
        this.state = state;
        this.exception = null;
    }

    synchronized public void setState(States state, Throwable e) {
        this.state = state;
        this.exception = e;
    }

    synchronized public Throwable getException() {
        return exception;
    }

    synchronized protected void setException(Throwable exception) {
        this.exception = exception;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public void setDelay(int delay) {
        this.delay = delay;
    }

}
