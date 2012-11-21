package com.github.axet.wget.info.ex;

/**
 * Multithread downloads can (and will) receive multiple IOExceptions such as
 * UnknownHostException, Socket Timeout and others. If it happens to the one
 * thread it will happens to all other threads. So App should parse the list of
 * the exceptions and deside what to do.
 * 
 * For example if all exceptions was DownloadRetry then just Retry downlod. If
 * here in the list any Fatal or runtime exception you shall stop the
 * application or stop the download
 * 
 * @author axet
 * 
 */
public class DownloadRetry extends RuntimeException {
    private static final long serialVersionUID = 1L;

    Throwable e;

    public DownloadRetry(Throwable e) {
        this.e = e;
    }

    public DownloadRetry(String msg) {
        super(msg);
    }

    public Throwable getE() {
        return e;
    }
}
