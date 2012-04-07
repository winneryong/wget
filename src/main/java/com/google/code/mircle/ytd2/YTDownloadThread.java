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

    // is the main thread done working?
    boolean canJoin = false;
    Exception e;

    Object statsLock = new Object();
    YouTubeInfo ei;
    YouTubeDownload d;

    Runnable notify;

    public YTDownloadThread(final YTD2Base base, String url, String target) {

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
            notify.run();
        }

        synchronized (statsLock) {
            canJoin = true;
        }
        notify.run();
    }
}