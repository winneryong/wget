package com.github.axet.wget.info;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * DownloadInfo class. Keep part information. We need to serialize this class
 * bettwen application restart. Thread safe.
 * 
 * @author axet
 * 
 */
@XStreamAlias("DownloadInfo")
public class DownloadInfo extends URLInfo {

    public final static long PART_LENGTH = 10 * 1024 * 1024;

    @XStreamAlias("DownloadInfoPart")
    public static class Part {
        // start offset [start, end]
        private long start;
        // end offset [start, end]
        private long end;
        // amount of bytes downloaded
        private long count;
        // part number
        private long number;

        /**
         * is done?
         * 
         * @return true. part fully downloaded
         */
        synchronized public boolean done() {
            return getCount() == getLength();
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

        synchronized public long getLength() {
            return end - start + 1;
        }

        synchronized public long getCount() {
            return count;
        }

        synchronized public void setCount(long count) {
            this.count = count;
        }

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
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

    /**
     * for multi part download, call every time when we need to know totol
     * download progress
     */
    synchronized public void calculate() {
        count = 0;

        for (Part p : getParts())
            count += p.getCount();
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

    synchronized public void enableMultipart() {
        if (empty())
            throw new RuntimeException("Empty Download info, cant set multipart");

        if (!range())
            throw new RuntimeException("Server does not support RANGE, cant set multipart");

        parts = new ArrayList<Part>();

        long count = getLength() / PART_LENGTH + 1;

        if (count > 2) {
            int start = 0;
            for (int i = 0; i < count; i++) {
                Part part = new Part();
                part.setNumber(i);
                part.setStart(start);
                part.setEnd(part.getStart() + PART_LENGTH - 1);
                if (part.getEnd() > getLength() - 1)
                    part.setEnd(getLength() - 1);
                parts.add(part);

                start += PART_LENGTH;
            }
        }
    }

    /**
     * Check if we can continue download a file from new source. Check if new
     * souce has the same file length, title. and supports for range
     * 
     * @param newSource
     *            new source
     * @return true - possible to resume from new location
     */
    synchronized public boolean resume(DownloadInfo newSource) {
        if (!newSource.range())
            return false;

        if (newSource.getContentFilename() != null && this.getContentFilename() != null) {
            if (!newSource.getContentFilename().equals(this.getContentFilename()))
                // one source has different name
                return false;
        } else if (newSource.getContentFilename() != null || this.getContentFilename() != null) {
            // one source has a have old is not
            return false;
        }

        if (newSource.getLength() != null && this.getLength() != null) {
            if (!newSource.getLength().equals(this.getLength()))
                // one source has different length
                return false;
        } else if (newSource.getLength() != null || this.getLength() != null) {
            // one source has length, other is not
            return false;
        }

        if (newSource.getContentType() != null && this.getContentType() != null) {
            if (!newSource.getContentType().equals(this.getContentType()))
                // one source has different getContentType
                return false;
        } else if (newSource.getContentType() != null || this.getContentType() != null) {
            // one source has a have old is not
            return false;
        }

        return true;
    }

    /**
     * copy resume data from oldSource;
     */
    synchronized public void copy(DownloadInfo oldSource) {
        count = oldSource.count;
        parts = oldSource.parts;
    }
}
