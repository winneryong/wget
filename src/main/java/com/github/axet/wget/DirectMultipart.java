package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadInterrupted;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class DirectMultipart extends Direct {

    static public final int THREAD_COUNT = 3;
    static public final int RETRY_DELAY = 10;

    static class BlockUntilFree implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                throw new DownloadInterrupted(e);
            }
        }
    }

    static class LimitDownloader extends ThreadPoolExecutor {
        boolean fatal = false;

        Object lock = new Object();

        public LimitDownloader() {
            super(THREAD_COUNT, THREAD_COUNT, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    new BlockUntilFree());
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
            return getActiveCount() > 0;
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
    void download(Part part) throws IOException {
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

    }

    void downloadWorker(final Part p) {
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RetryFactory.wrap(stop, new RetryFactory.RetryWrapper() {

                        @Override
                        public void run() throws IOException {
                            download(p);
                        }

                        @Override
                        public void notifyRetry(int delay, Throwable e) {
                            p.setState(States.RETRYING, e);
                            p.setDelay(delay);
                            notify.run();
                        }

                        @Override
                        public void notifyDownloading() {
                            p.setState(States.DOWNLOADING);
                            notify.run();
                        }
                    });
                    p.setState(States.DONE);
                    notify.run();
                } catch (RuntimeException e) {
                    p.setState(States.ERROR, e);
                    notify.run();
                    synchronized (worker.lock) {
                        worker.fatal = true;
                    }
                }
            }
        });
        
        p.setState(States.DOWNLOADING);
    }

    /**
     * return next part to download. ensure this part is not done() and not
     * currently downloading
     * 
     * @return
     */
    Part getPart() {
        for (Part p : info.getParts()) {
            if (!p.getState().equals(States.QUEUED))
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
            p.setState(States.QUEUED);
        }
        info.setState(URLInfo.States.DOWNLOADING);
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

            info.setState(URLInfo.States.DONE);
            notify.run();
        } finally {
            worker.shutdown();
        }
    }
}
