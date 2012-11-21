package com.github.axet.wget.info.ex;

public class DownloadInterrupted extends RuntimeException {
    private static final long serialVersionUID = 1L;

    Throwable e;

    public DownloadInterrupted(Throwable e) {
        this.e = e;
    }

    public DownloadInterrupted(String msg) {
        super(msg);
    }

    public DownloadInterrupted() {
    }

    public Throwable getE() {
        return e;
    }
}
