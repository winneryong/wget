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

    synchronized public void reset() {
        setCount(0);

        if (parts != null) {
            for (Part p : parts) {
                p.setCount(0);
            }
        }
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

    /**
     * Check if we can continue download a file from new source. Check if new
     * souce has the same file length, title
     * 
     * @param info
     *            - new source
     * @return true - possible to resume from new location
     */
    synchronized public boolean resume(DownloadInfo info) {
        if (info.getContentFilename() != null && this.getContentFilename() != null) {
            if (!info.getContentFilename().equals(this.getContentFilename()))
                // one source has different name
                return false;
        } else if (info.getContentFilename() != null || this.getContentFilename() != null) {
            // one source has a have old is not
            return false;
        }

        if (info.getLength() != null && this.getLength() != null) {
            if (!info.getLength().equals(this.getLength()))
                // one source has different length
                return false;
        } else if (info.getLength() != null || this.getLength() != null) {
            // one source has length, other is not
            return false;
        }

        if (info.getContentType() != null && this.getContentType() != null) {
            if (!info.getContentType().equals(this.getContentType()))
                // one source has different getContentType
                return false;
        } else if (info.getContentType() != null || this.getContentType() != null) {
            // one source has a have old is not
            return false;
        }

        return true;
    }
}
