package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadError;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.PartState;
import com.github.axet.wget.info.DownloadMultipartError;
import com.github.axet.wget.info.DownloadRetry;

public class DirectMultipart extends Direct {

    static public final int THREAD_COUNT = 3;
    static public final int RETRY_DELAY = 10;

    class LimitDownloader extends ThreadPoolExecutor {

        Object lock = new Object();

        boolean fatal = false;

        public LimitDownloader() {
            super(THREAD_COUNT, THREAD_COUNT, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            synchronized (lock) {
                lock.notifyAll();
            }
        }

        /**
         * downloader working if here any getTasks() > 0
         * 
         * @return
         */
        public boolean active() {
            return getTasks() > 0;
        }

        /**
         * wait until current task ends. if here is no tasks exit immidiatly.
         * 
         */
        public void waitUntilNextTaskEnds() {
            synchronized (lock) {
                if (getActiveCount() > 0) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        /**
         * get active threads and queue size as current tasks count
         * 
         * @return
         */
        int getTasks() {
            return getActiveCount() + getQueue().size();
        }

        /**
         * blocks if here is too many working thread active
         * 
         */
        @Override
        public void execute(Runnable command) {
            // do not allow to put more tasks then threads.
            // if happens - wait until thread ends.
            synchronized (lock) {
                if (getTasks() >= THREAD_COUNT) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (stop.get())
                    return;
            }

            super.execute(command);
        }

        /**
         * returns true if here was a DownloadError or RuntimeException in one
         * of the downloading threads
         * 
         * @return
         */
        public boolean fatal() {
            synchronized (lock) {
                return fatal;
            }
        }
    }

    LimitDownloader worker = new LimitDownloader();

    /**
     * 
     * @param info
     *            download file information
     * @param target
     *            target file
     * @param stop
     *            multithread stop command
     * @param notify
     *            progress notify call
     */
    public DirectMultipart(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
        super(info, target, stop, notify);
    }

    /**
     * download part.
     * 
     * if returns normally - part is fully donwloaded. other wise - it throws
     * RuntimeException or DownloadRetry or DownloadError
     * 
     * @param part
     */
    void download(Part part) {
        part.setState(PartState.DOWNLOADING);
        part.setE(null);
        notify.run();

        try {
            RandomAccessFile fos = null;
            BufferedInputStream binaryreader = null;

            try {
                URL url = info.getSource();

                HttpURLConnection conn;
                conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);

                File f = target;

                fos = new RandomAccessFile(f, "rw");

                long start = part.getStart() + part.getCount();
                long end = part.getEnd();

                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                fos.seek(start);

                byte[] bytes = new byte[BUF_SIZE];
                int read = 0;

                binaryreader = new BufferedInputStream(conn.getInputStream());

                boolean localStop = false;

                while ((read = binaryreader.read(bytes)) > 0) {
                    // ensure we do not download more then part size.
                    // if so cut bytes and stop download
                    long partEnd = part.getLength() - part.getCount();
                    if (read > partEnd) {
                        read = (int) partEnd;
                        localStop = true;
                    }

                    fos.write(bytes, 0, read);
                    part.setCount(part.getCount() + read);
                    info.calculate();
                    notify.run();

                    if (stop.get())
                        return;
                    if (localStop)
                        return;
                }

                if (part.getCount() != part.getLength())
                    throw new DownloadRetry("EOF before end of part");
            } finally {
                if (fos != null)
                    fos.close();
                if (binaryreader != null)
                    binaryreader.close();
            }
        } catch (SocketException e) {
            // enumerate all retry exceptions
            throw new DownloadRetry(e);
        } catch (ProtocolException e) {
            // enumerate all retry exceptions
            throw new DownloadRetry(e);
        } catch (HttpRetryException e) {
            // enumerate all retry exceptions
            throw new DownloadRetry(e);
        } catch (InterruptedIOException e) {
            // enumerate all retry exceptions
            throw new DownloadRetry(e);
        } catch (UnknownHostException e) {
            // enumerate all retry exceptions
            throw new DownloadRetry(e);
        } catch (IOException e) {
            // all other io excetption including FileNotFoundException should
            // stop downloading.
            throw new DownloadError(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void retry(Part p, RuntimeException e) {
        p.setState(PartState.RETRYING);
        p.setE(e);

        for (int i = RETRY_DELAY; i > 0; i--) {
            p.setDelay(i);
            notify.run();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void downloadRetry(Part p) {
        // retry download part until it returns without an exception
        while (!stop.get()) {
            try {
                download(p);

                p.setState(PartState.DONE);
                notify.run();

                return;
            } catch (DownloadRetry e) {
                retry(p, e);
            }
        }
    }

    void downloadWorker(final Part p) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadRetry(p);
                } catch (RuntimeException e) {
                    p.setState(PartState.ERROR, e);
                    notify.run();
                    synchronized (worker.lock) {
                        worker.fatal = true;
                    }
                }
            }
        });
    }

    /**
     * return next part to download. ensure this part is not done() and not
     * currently downloading
     * 
     * @return
     */
    Part getPart() {
        for (Part p : info.getParts()) {
            if (!p.getState().equals(PartState.QUEUED))
                continue;

            return p;
        }

        return null;
    }

    /**
     * return true, when thread pool empty, and here is no unfinished parts to
     * download
     * 
     * @return true - done. false - not done yet
     */
    boolean done() {
        if (stop.get())
            return true;
        if (worker.active())
            return false;
        if (getPart() != null)
            return false;

        return true;
    }

    public void download() {
        for (Part p : info.getParts()) {
            p.setState(PartState.QUEUED);
        }
        notify.run();

        try {
            while (!done()) {
                Part p = getPart();
                if (p != null) {
                    downloadWorker(p);
                } else {
                    worker.waitUntilNextTaskEnds();
                }

                // if we start to receive errors. stop add new tasks and wait
                // until all active tasks && queue will be emptied
                if (worker.fatal()) {
                    while (worker.active()) {
                        worker.waitUntilNextTaskEnds();
                    }

                    // ok all thread stopped. now throw the exception and let
                    // app deal with the errors
                    throw new DownloadMultipartError(info);
                }
            }
        } finally {
            worker.shutdown();
        }
    }
}
