package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class DirectMultipart extends Direct {

    static public final int THREAD_COUNT = 3;
    static public final int RETRY_DELAY = 10;

    LimitThreadPool worker = new LimitThreadPool(THREAD_COUNT);

    boolean fatal = false;

    Object lock = new Object();

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
    public DirectMultipart(DownloadInfo info, File target) {
        super(info, target);
    }

    /**
     * download part.
     * 
     * if returns normally - part is fully donwloaded. other wise - it throws
     * RuntimeException or DownloadRetry or DownloadError
     * 
     * @param part
     */
    void download(Part part, AtomicBoolean stop, Runnable notify) throws IOException {
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

    boolean fatal() {
        synchronized (lock) {
            return fatal;
        }
    }

    void fatal(boolean b) {
        synchronized (lock) {
            fatal = b;
        }
    }

    void downloadWorker(final Part p, final AtomicBoolean stop, final Runnable notify) throws InterruptedException {
        worker.blockExecute(new Runnable() {
            @Override
            public void run() {
                try {
                    RetryFactory.wrap(stop, new RetryFactory.Wrap() {

                        @Override
                        public void run() throws IOException {
                            download(p, stop, notify);
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

                    fatal(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
     * @throws InterruptedException
     */
    boolean done(AtomicBoolean stop) throws InterruptedException {
        if (stop.get())
            throw new InterruptedException();
        if (Thread.interrupted())
            throw new InterruptedException();
        if (worker.active())
            return false;
        if (getPart() != null)
            return false;

        return true;
    }

    @Override
    public void download(AtomicBoolean stop, Runnable notify) throws InterruptedException {
        for (Part p : info.getParts()) {
            p.setState(States.QUEUED);
        }
        info.setState(URLInfo.States.DOWNLOADING);
        notify.run();

        try {
            while (!done(stop)) {
                Part p = getPart();
                if (p != null) {
                    downloadWorker(p, stop, notify);
                } else {
                    // we have no parts left.
                    //
                    // wait until task ends and check again if we have to retry.
                    // we have to check if last part back to queue in case of
                    // RETRY state
                    worker.waitUntilNextTaskEnds();
                }

                // if we start to receive errors. stop add new tasks and wait
                // until all active tasks be emptied
                if (fatal()) {
                    worker.waitUntilTermination();

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
