package com.github.axet.wget.info;

/**
 * We shall stop download. And only manual action (redownload) shall continue
 * 
 * @author axet
 * 
 */
public class DownloadError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DownloadError(String msg) {
        super(msg);
    }
}
