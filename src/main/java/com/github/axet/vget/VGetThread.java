package com.github.axet.vget;

import com.github.axet.vget.info.VGetInfo;
import com.github.axet.vget.info.VGetInfo.VideoQuality;
import com.github.axet.vget.info.VimeoInfo;
import com.github.axet.vget.info.YouTubeInfo;

class VGetThread extends Thread {

    // is the main thread done working?
    boolean canJoin = false;

    // exception druning execution
    Exception e;

    Object statsLock = new Object();
    VGetInfo ei;
    VGetDownload d;

    Runnable notify;

    public VGetThread(final VGetBase base, String url, String target) {

        notify = new Runnable() {
            @Override
            public void run() {
                base.changed();
            }
        };

        if (YouTubeInfo.probe(url))
            ei = new YouTubeInfo(base, url);

        if (VimeoInfo.probe(url))
            ei = new VimeoInfo(base, url);

        if (ei == null)
            throw new RuntimeException("unsupported web site");

        d = new VGetDownload(base, ei, target, notify);
    }

    public String getTitle() {
        synchronized (statsLock) {
            return ei.getTitle();
        }
    }

    public long getTotal() {
        synchronized (statsLock) {
            return d.total;
        }
    }

    public long getCount() {
        synchronized (statsLock) {
            return d.count;
        }
    }

    public VideoQuality getVideoQuality() {
        synchronized (statsLock) {
            return d.max != null ? d.max.vq : null;
        }
    }

    public String getFileName() {
        synchronized (statsLock) {
            return d.target;
        }
    }

    public String getInput() {
        synchronized (statsLock) {
            return ei.getSource();
        }
    }

    public void setFileName(String file) {
        synchronized (statsLock) {
            d.target = file;
        }
    }

    @Override
    public void run() {
        try {
            ei.extract();
            d.download();
        } catch (Exception e) {
            synchronized (statsLock) {
                this.e = e;
            }
            notify.run();
        }

        synchronized (statsLock) {
            canJoin = true;
        }
        notify.run();
    }
}