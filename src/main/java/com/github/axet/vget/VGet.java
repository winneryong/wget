package com.github.axet.vget;

import java.io.File;
import java.util.ArrayList;

import com.github.axet.vget.VGetInfo.VideoQuality;

public class VGet extends VGetBase {

    ArrayList<Listener> list = new ArrayList<VGet.Listener>();
    String source;
    String target;

    String targetForce;

    public static interface Listener {
        public void changed();
    }

    void changed() {
        for (Listener l : list) {
            l.changed();
        }
    }

    public VGet(String source, String target) {
        super();

        this.source = source.trim();
        this.target = target.trim();
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

        download(source, target);

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
            return t1.canJoin;
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
        return t1.getInput();
    }

    /**
     * get output file on local file system
     * 
     * @return
     */
    public String getOutput() {
        return t1.getFileName();
    }

    /**
     * get bytes downloaded
     * 
     * @return
     */
    public long getBytes() {
        return t1.getCount();
    }

    /**
     * get total size of youtube movie
     * 
     * @return
     */
    public long getTotal() {
        return t1.getTotal();
    }

    /**
     * get youtube title
     * 
     * @return
     */
    public String getTitle() {
        return t1.getTitle();
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
        return t1.getVideoQuality();
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
        // 120p test
        // YTD2 y = new YTD2("http://www.youtube.com/watch?v=OY7fmYkpsRs",
        // "/Users/axet/Downloads");

        // age restriction test
        // YTD2 y = new
        // YTD2("http://www.youtube.com/watch?v=QoTWRHheshw&feature=youtube_gdata",
        // "/Users/axet/Downloads");

        // user page test
        // YTD2 y = new YTD2(
        // "http://www.youtube.com/user/cubert01?v=gidumziw4JE&feature=pyv&ad=8307058643&kw=youtube%20download",
        // "/Users/axet/Downloads");

        // hd test
        // VGet y = new VGet("http://www.youtube.com/watch?v=rRS6xL1B8ig",
        // "/Users/axet/Downloads");

        VGet y = new VGet("http://vimeo.com/39289096", "/Users/axet/Downloads");

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
