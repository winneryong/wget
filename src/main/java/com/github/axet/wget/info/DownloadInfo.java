package com.github.axet.wget.info;

import java.net.URL;
import java.util.List;

/**
 * DownloadInfo class. Keep part information. We need to serialize this class
 * bettwen application restart. Thread safe.
 * 
 * @author axet
 * 
 */
public class DownloadInfo extends URLInfo {

    public static class Part {
        // start offset
        private long start;
        // end offset
        private long end;
        // amount of bytes downloaded
        private long count;

        /**
         * is done?
         * 
         * @return true. part fully downloaded
         */
        synchronized public boolean done() {
            return getEnd() - getStart() == getCount();
        }

        synchronized public long getStart() {
            return start;
        }

        synchronized public void setStart(long start) {
            this.start = start;
        }

        synchronized public long getEnd() {
            return end;
        }

        synchronized public void setEnd(long end) {
            this.end = end;
        }

        synchronized public long getCount() {
            return count;
        }

        synchronized public void setCount(long count) {
            this.count = count;
        }
    }

    // part we are going to download. partList == null. we are doing one thread
    // download
    private List<Part> parts;

    // total bytes downloaded. for chunk download progress info. for one thread
    // count - also local file size;
    private long count;

    public DownloadInfo(URL source) {
        super(source);
    }

    /**
     * is it a multipart download?
     * 
     * @return
     */
    synchronized public boolean multipart() {
        if (!range())
            return false;

        return getParts() != null;
    }

    synchronized public long getCount() {
        return count;
    }

    synchronized public void setCount(long count) {
        this.count = count;
    }

    synchronized public List<Part> getParts() {
        return parts;
    }

    synchronized public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}
