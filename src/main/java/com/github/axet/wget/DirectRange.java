package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;

public class DirectRange implements Direct {

    File target = null;

    DownloadInfo info;

    Runnable notify;

    AtomicBoolean stop;

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
    public DirectRange(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
        this.target = target;
        this.info = info;
        this.notify = notify;
        this.stop = stop;
    }

    public void download() {
        try {
            RandomAccessFile fos = null;

            try {
                URL url = info.getSource();

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(WGet.CONNECT_TIMEOUT);
                conn.setReadTimeout(WGet.READ_TIMEOUT);

                File f = target;
                if (!f.exists())
                    f.createNewFile();
                info.setCount(FileUtils.sizeOf(f));

                fos = new RandomAccessFile(f, "rw");

                if (info.getCount() > 0) {
                    conn.setRequestProperty("Range", "bytes=" + info.getCount() + "-");
                    fos.seek(info.getCount());
                }

                byte[] bytes = new byte[WGet.BUF_SIZE];
                int read = 0;

                BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

                while (!stop.get() && (read = binaryreader.read(bytes)) > 0) {

                    info.setCount(info.getCount() + read);
                    fos.write(bytes, 0, read);

                    notify.run();
                }

                binaryreader.close();
            } finally {
                if (fos != null)
                    fos.close();
            }
        } catch (IOException e) {
            throw new DownloadRetry(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
