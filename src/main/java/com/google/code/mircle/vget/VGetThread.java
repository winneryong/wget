package com.google.code.mircle.vget;

import com.google.code.mircle.vget.VGet.VideoQuality;

class VGetThread extends Thread {

    // is the main thread done working?
    boolean canJoin = false;
    Exception e;

    Object statsLock = new Object();
    YouTubeInfo ei;
    YouTubeDownload d;

    Runnable notify;

    public VGetThread(final VGetBase base, String url, String target) {

        notify = new Runnable() {
            @Override
            public void run() {
                base.changed();
            }
        };

        ei = new YouTubeInfo(base, url);
        d = new YouTubeDownload(base, ei, target, notify);
    }

    public void setMaxQuality(VideoQuality max) {
        d.max = max;
    }

    public String getTitle() {
        synchronized (statsLock) {
            return ei.sTitle;
        }
    }

    public long getTotal() {
        synchronized (statsLock) {
            return d.iBytesMax;
        }
    }

    public long getCount() {
        synchronized (statsLock) {
            return d.count;
        }
    }

    public VideoQuality getVideoQuality() {
        synchronized (statsLock) {
            return d.max;
        }
    }

    public String getFileName() {
        synchronized (statsLock) {
            return d.getFileName();
        }
    }

    public String getInput() {
        synchronized (statsLock) {
            return ei.source;
        }
    }

    public void setFileName(String file) {
        synchronized (statsLock) {
            d.setFileName(file);
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