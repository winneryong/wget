package com.github.axet.wget;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;

public class Direct {

    File target = null;

    DownloadInfo info;

    Runnable notify;

    AtomicBoolean stop;

    static public final int CONNECT_TIMEOUT = 5000;

    static public final int READ_TIMEOUT = 5000;

    // size of read buffer
    static public final int BUF_SIZE = 4 * 1024;

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
    public Direct(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
        this.target = target;
        this.info = info;
        this.notify = notify;
        this.stop = stop;
    }

    public void download() {
    }

}
