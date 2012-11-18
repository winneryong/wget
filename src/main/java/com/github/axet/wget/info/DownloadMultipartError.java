package com.github.axet.wget.info;

import java.util.List;

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
public class DownloadMultipartError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    List<Throwable> e;

    public DownloadMultipartError(List<Throwable> e) {
        this.e = e;
    }

    /**
     * List of multipart threads errors. Can be one of the following types:
     * DownloadError, DownloadRetry, RuntimeException
     * 
     * @return
     */
    public List<Throwable> getList() {
        return e;
    }

}
