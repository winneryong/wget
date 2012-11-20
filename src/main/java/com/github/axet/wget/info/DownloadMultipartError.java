package com.github.axet.wget.info;

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

    DownloadInfo info;

    public DownloadMultipartError(DownloadInfo info) {
        this.info = info;
    }

    public DownloadInfo getInfo() {
        return info;
    }
}
