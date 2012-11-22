package com.github.axet.wget;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadIOError;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class RetryWrap {

    public static final int RETRY_DELAY = 10;

    public interface WrapReturn<T> {
        public void retry(int delay, Throwable e);

        public T download() throws IOException;
    }

    public interface Wrap {
        public void retry(int delay, Throwable e);

        public void download() throws IOException;
    }

    static <T> void retry(AtomicBoolean stop, WrapReturn<T> r, RuntimeException e) {
        for (int i = RETRY_DELAY; i >= 0; i--) {
            r.retry(i, e);

            if (stop.get())
                throw new DownloadInterruptedError("stop");

            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedError("interrrupted");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                throw new DownloadInterruptedError(e1);
            }
        }
    }

    public static <T> T run(AtomicBoolean stop, WrapReturn<T> r) {
        while (true) {
            if (stop.get())
                throw new DownloadInterruptedError("stop");
            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedError("interrupted");

            try {
                try {
                    T t = r.download();

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
                } catch (FileNotFoundException e) {
                    throw new DownloadError(e);
                } catch (RuntimeException e) {
                    throw e;
                } catch (IOException e) {
                    throw new DownloadIOError(e);
                }
            } catch (DownloadRetry e) {
                retry(stop, r, e);
            }
        }
    }

    public static <T> T wrap(AtomicBoolean stop, WrapReturn<T> r) {
        return RetryWrap.run(stop, r);
    }

    public static void wrap(AtomicBoolean stop, final Wrap r) {
        WrapReturn<Object> rr = new WrapReturn<Object>() {

            @Override
            public Object download() throws IOException {
                r.download();

                return null;
            }

            @Override
            public void retry(int delay, Throwable e) {
                r.retry(delay, e);
            }
        };

        RetryWrap.run(stop, rr);
    }
}
