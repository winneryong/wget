package com.github.axet.wget;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;

public class WGet {

    File target = null;

    DownloadInfo info;

    Runnable notify;

    AtomicBoolean stop;

    static public final int CONNECT_TIMEOUT = 5000;

    static public final int READ_TIMEOUT = 5000;

    // size of read buffer
    static public final int BUF_SIZE = 4 * 1024;

    // size of part if downloaded as multipart
    static public final int CHUNK_SIZE = 1 * 1024 * 1024;

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
    public WGet(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
        this.target = target;
        this.info = info;
        this.notify = notify;
        this.stop = stop;

        calc();
    }

    void calc() {
        if (!info.range())
            return;

        int count = (int) (info.getLength() / CHUNK_SIZE) + 1;
        if (count > 2) {
            ArrayList<DownloadInfo.Part> parts = new ArrayList<DownloadInfo.Part>();
            int start = 0;
            for (int i = 0; i < count; i++) {
                Part part = new Part();
                part.setStart(start);
                part.setEnd(start + CHUNK_SIZE);
                if (part.getEnd() > info.getLength())
                    part.setEnd(info.getLength());
                parts.add(part);

                start += CHUNK_SIZE;
            }
        }
    }

    public void download() {
        if (info.multipart()) {
            DirectMultipart d = new DirectMultipart(info, target, stop, notify);
            d.download();
        } else if (info.range()) {
            DirectRange d = new DirectRange(info, target, stop, notify);
            d.download();
        } else {
            DirectSingle d = new DirectSingle(info, target, stop, notify);
            d.download();
        }
    }

}
