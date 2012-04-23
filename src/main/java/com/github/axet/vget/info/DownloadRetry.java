package com.github.axet.vget.info;

/**
 * We shall retry download after application delay (10 secs?)
 * 
 * @author axet
 * 
 */
public class DownloadRetry extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DownloadRetry(Throwable e) {
        super(e);
    }

    public DownloadRetry(String msg) {
        super(msg);
    }
}
