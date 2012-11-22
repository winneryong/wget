package com.github.axet.wget;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class RetryFactory {

    public interface WrapReturn<T> {
        public void notifyRetry(int delay, Throwable e);

        public void notifyDownloading();

        public T run() throws IOException;
    }

    public interface Wrap {
        public void notifyRetry(int delay, Throwable e);

        public void notifyDownloading();

        public void run() throws IOException;
    }

    public static final int RETRY_DELAY = 10;

    static <T> void retry(AtomicBoolean stop, WrapReturn<T> r, RuntimeException e) throws InterruptedException {
        for (int i = RETRY_DELAY; i > 0; i--) {
            r.notifyRetry(i, e);

            if (stop.get())
                throw new InterruptedException("stop");

            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException("interrrupted");

            Thread.sleep(1000);
        }
    }

    public static <T> T run(AtomicBoolean stop, WrapReturn<T> r) throws InterruptedException {
        while (true) {
            if (stop.get())
                throw new InterruptedException("stop");
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException("interrupted");

            try {
                try {
                    r.notifyDownloading();

                    T t = r.run();

                    return t;
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
                    // all other io excetption including FileNotFoundException
                    // should stop downloading.
                    throw new DownloadError(e);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (DownloadRetry e) {
                retry(stop, r, e);
            }
        }
    }

    public static <T> T wrap(AtomicBoolean stop, WrapReturn<T> r) throws InterruptedException {
        return RetryFactory.run(stop, r);
    }

    public static void wrap(AtomicBoolean stop, final Wrap r) throws InterruptedException {
        WrapReturn<Object> rr = new WrapReturn<Object>() {

            @Override
            public Object run() throws IOException {
                r.run();

                return null;
            }

            @Override
            public void notifyRetry(int delay, Throwable e) {
                r.notifyRetry(delay, e);
            }

            @Override
            public void notifyDownloading() {
                r.notifyDownloading();
            }
        };

        RetryFactory.run(stop, rr);
    }
}
