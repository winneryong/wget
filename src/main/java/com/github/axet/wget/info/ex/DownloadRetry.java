package com.github.axet.wget.info.ex;

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
