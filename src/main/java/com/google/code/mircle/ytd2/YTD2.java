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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class YTD2 extends YTD2Base {

    ArrayList<Listener> list = new ArrayList<YTD2.Listener>();
    String source;
    String target;

    String targetForce;

    VideoQuality max = VideoQuality.p1080;

    public static interface Listener {
        public void changed();
    }

    public enum VideoQuality {
        p1080, p720, p480, p360, p240, p120
    }

    void changed() {
        for (Listener l : list) {
            l.changed();
        }
    }

    public YTD2(String source, String target) {
        super();

        this.source = source;
        this.target = target;
    }

    public YTD2(String source, String target, VideoQuality max) {
        super();

        this.source = source;
        this.target = target;

        this.max = max;
    }

    public void setTarget(File path) {
        targetForce = path.toString();
    }

    /**
     * ask thread to start work
     */
    public void start() {
        if (t1 != null && isActive())
            throw new RuntimeException("already started");

        String oldpath = null;
        if (t1 != null)
            oldpath = t1.getFileName();

        if (targetForce != null)
            oldpath = targetForce;

        create();
        checkInputFieldforYTURLs(source, target, max);

        t1.setFileName(oldpath);

        setbQuitrequested(false);
        t1.start();
    }

    /**
     * ask thread to stop working. and wait for change event.
     * 
     */
    public void stop() {
        setbQuitrequested(true);
    }

    /**
     * if working thread is active.
     * 
     * @return
     */
    public boolean isActive() {
        return t1.isAlive();
    }

    /**
     * check if working thread has send the last possible event. so we can join.
     * 
     * @return true - we can join
     */
    public boolean isJoin() {
        synchronized (t1.statsLock) {
            return t1.join;
        }
    }

    /**
     * Join to working thread and wait until it done
     */
    public void join() {
        try {
            t1.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * get exception.
     * 
     * @return
     */
    public Exception getException() {
        synchronized (t1.statsLock) {
            return t1.e;
        }
    }

    /**
     * wait until thread ends and close it. do before you exit app.
     */
    public void close() {
        shutdownAppl();
    }

    /**
     * get input url name
     * 
     * @return
     */
    public String getInput() {
        synchronized (t1.statsLock) {
            return t1.input;
        }
    }

    /**
     * get output file on local file system
     * 
     * @return
     */
    public String getOutput() {
        synchronized (t1.statsLock) {
            return t1.sFileName;
        }
    }

    /**
     * get bytes downloaded
     * 
     * @return
     */
    public long getBytes() {
        synchronized (t1.statsLock) {
            return t1.count;
        }
    }

    /**
     * get total size of youtube movie
     * 
     * @return
     */
    public long getTotal() {
        synchronized (t1.statsLock) {
            return t1.total;
        }
    }

    /**
     * get youtube title
     * 
     * @return
     */
    public String getTitle() {
        synchronized (t1.statsLock) {
            return t1.getTitle();
        }
    }

    /**
     * is everyting downloaded ok?
     * 
     * @return true if true
     */
    public boolean done() {
        return getBytes() >= getTotal();
    }

    public boolean canceled() {
        return getbQuitrequested();
    }

    public VideoQuality getVideoQuality() {
        return t1.vq;
    }

    /**
     * Please not by using listener you agree to handle multithread calls. I
     * suggest if you do SwingUtils.invokeLater (or your current thread manager)
     * for each 'Listener.changed' event.
     * 
     * @param l
     *            listenrer
     */
    public void addListener(Listener l) {
        list.add(l);
    }

    public void removeListener(Listener l) {
        list.remove(l);
    }

    public static void main(String[] args) {
        YTD2 y = new YTD2("http://www.youtube.com/watch?v=OY7fmYkpsRs", "/Users/axet/Downloads");
        y.start();

        System.out.println("input: " + y.getInput());

        while (y.isActive()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

            System.out.println("title: " + y.getTitle() + ", Quality: " + y.getVideoQuality() + ", bytes: "
                    + y.getBytes() + ", total: " + y.getTotal());
        }

        if (y.isJoin())
            y.join();

        y.close();

        if (y.getException() != null)
            y.getException().printStackTrace();
    }
}
