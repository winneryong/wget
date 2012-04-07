/**
 *  This file is part of ytd2
 *
 *  ytd2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ytd2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with ytd2.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.google.code.mircle.ytd2;

import com.google.code.mircle.ytd2.YTD2.VideoQuality;

class YTDownloadThread extends Thread {

    VideoQuality max;
    YTD2Base base;
    String url;
    String target;

    boolean join = false;
    Exception e;

    Object statsLock = new Object();
    YouTubeInfo ei;
    YouTubeDownload d;

    final static VideoQuality DEFAULT_QUALITY = VideoQuality.p1080;

    public YTDownloadThread(YTD2Base base, String url, String target) {
        this.base = base;
        this.url = url;
        this.target = target;

        final YTDownloadThread that = this;

        Runnable notify = new Runnable() {
            @Override
            public void run() {
                that.base.changed();
            }
        };

        ei = new YouTubeInfo(base, url, DEFAULT_QUALITY);
        d = new YouTubeDownload(base, ei, target, notify);
    }

    public void setMaxQuality(VideoQuality max) {
        this.max = max;

        ei.vq = max;
    }

    public String getTitle() {
        synchronized (statsLock) {
            return ei.getTitle();
        }
    }

    public long getTotal() {
        synchronized (statsLock) {
            return ei.iBytesMax;
        }
    }

    public long getCount() {
        synchronized (statsLock) {
            return d.count;
        }
    }

    public VideoQuality getVideoQuality() {
        synchronized (statsLock) {
            return ei.vq;
        }
    }

    public String getFileName() {
        synchronized (statsLock) {
            return d.getFileName();
        }
    }

    public String getInput() {
        synchronized (statsLock) {
            return ei.input;
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
            base.changed();
        }

        synchronized (statsLock) {
            join = true;
        }
        base.changed();
    }
}