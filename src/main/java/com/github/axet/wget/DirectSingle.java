package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;

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
    public DirectSingle(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
        super(info, target, stop, notify);
    }

    public void download() {
        try {
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
