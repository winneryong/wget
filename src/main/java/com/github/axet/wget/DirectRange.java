package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;

public class DirectRange extends Direct {

    public DirectRange(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
        super(info, target, stop, notify);
    }

    public void download(Part part) throws IOException {
        RandomAccessFile fos = null;
        BufferedInputStream binaryreader = null;

        try {
            URL url = info.getSource();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            File f = target;
            if (!f.exists())
                f.createNewFile();
            info.setCount(FileUtils.sizeOf(f));

            if (info.getCount() >= info.getLength()) {
                notify.run();
                return;
            }

            fos = new RandomAccessFile(f, "rw");

            if (info.getCount() > 0) {
                conn.setRequestProperty("Range", "bytes=" + info.getCount() + "-");
                fos.seek(info.getCount());
            }

            byte[] bytes = new byte[BUF_SIZE];
            int read = 0;

            binaryreader = new BufferedInputStream(conn.getInputStream());

            while (!stop.get() && (read = binaryreader.read(bytes)) > 0) {
                fos.write(bytes, 0, read);

                part.setCount(part.getCount() + read);
                info.calculate();
                notify.run();
            }

        } finally {
            if (fos != null)
                fos.close();
            if (binaryreader != null)
                binaryreader.close();
        }
    }

    public void download() {
        List<Part> list = info.getParts();
        final Part p = list.get(0);

        RetryFactory.wrap(stop, new RetryFactory.RetryWrapper() {

            @Override
            public void run() throws IOException {
                download(p);
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
