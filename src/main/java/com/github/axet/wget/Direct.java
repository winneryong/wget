package com.github.axet.wget;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;

public abstract class Direct {

    File target = null;

    DownloadInfo info;

    /**
     * connect socket timeout
     */
    static public final int CONNECT_TIMEOUT = 10000;

    /**
     * read socket timeout
     */
    static public final int READ_TIMEOUT = 10000;

    /**
     * size of read buffer
     */
    static public final int BUF_SIZE = 4 * 1024;

    /**
     * fake user agent for Vimeo.
     */
    static public final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11";

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
    public Direct(DownloadInfo info, File target) {
        this.target = target;
        this.info = info;
    }

    abstract public void download(AtomicBoolean stop, Runnable notify);

}
