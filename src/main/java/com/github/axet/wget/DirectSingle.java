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
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;

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

    void downloadPart(Part part, AtomicBoolean stop, Runnable notify) throws IOException {
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

            while ((read = binaryreader.read(bytes)) > 0) {
                fos.write(bytes, 0, read);

                part.setCount(part.getCount() + read);
                info.calculate();
                notify.run();

                if (stop.get())
                    throw new DownloadInterruptedError("stop");
                if (Thread.interrupted())
                    throw new DownloadInterruptedError("interrupted");
            }

            binaryreader.close();
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    @Override
    public void download(final AtomicBoolean stop, final Runnable notify) {
        info.setState(URLInfo.States.DOWNLOADING);
        notify.run();

        List<Part> list = info.getParts();
        final Part p = list.get(0);

        try {
            RetryWrap.wrap(stop, new RetryWrap.Wrap() {
                @Override
                public void download() throws IOException {
                    p.setState(States.DOWNLOADING);
                    info.setState(URLInfo.States.DOWNLOADING);
                    notify.run();

                    downloadPart(p, stop, notify);
                }

                @Override
                public void retry(int delay, Throwable e) {
                    p.setDelay(delay, e);
                    info.setDelay(delay, e);
                    notify.run();
                }
            });

            p.setState(States.DONE);
            info.setState(URLInfo.States.DONE);
            notify.run();
        } catch (DownloadInterruptedError e) {
            p.setState(States.STOP);
            info.setState(URLInfo.States.STOP);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            p.setState(States.ERROR);
            info.setState(URLInfo.States.ERROR);
            notify.run();

            throw e;
        }
    }
}
