package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;

public class DirectSingle extends Direct {

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
    public DirectSingle(DownloadInfo info, File target) {
        super(info, target);
    }

    void download(Part part, AtomicBoolean stop, Runnable notify) throws IOException {
        RandomAccessFile fos = null;

        try {
            URL url = info.getSource();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            File f = target;
            info.setCount(0);
            f.createNewFile();

            fos = new RandomAccessFile(f, "rw");

            byte[] bytes = new byte[BUF_SIZE];
            int read = 0;

            BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

            while (!stop.get() && (read = binaryreader.read(bytes)) > 0) {
                fos.write(bytes, 0, read);

                part.setCount(part.getCount() + read);
                info.calculate();
                notify.run();
            }

            binaryreader.close();
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    @Override
    public void download(final AtomicBoolean stop, final Runnable notify) throws InterruptedException {
        List<Part> list = info.getParts();
        final Part p = list.get(0);

        RetryFactory.wrap(stop, new RetryFactory.RetryWrapper() {

            @Override
            public void run() throws IOException {
                download(p, stop, notify);
            }

            @Override
            public void notifyRetry(int delay, Throwable e) {
                p.setState(States.RETRYING, e);
                p.setDelay(delay);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                p.setState(States.DOWNLOADING);
                notify.run();
            }
        });

        p.setState(States.DONE);
        notify.run();
    }
}
