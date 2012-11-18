package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import com.github.axet.wget.info.DownloadError;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;

public class DirectRange extends Direct {

    public DirectRange(DownloadInfo info, File target, AtomicBoolean stop, Runnable notify) {
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
        } catch (SocketException e) {
            throw new DownloadRetry(e);
        } catch (ProtocolException e) {
            throw new DownloadRetry(e);
        } catch (HttpRetryException e) {
            throw new DownloadRetry(e);
        } catch (InterruptedIOException e) {
            throw new DownloadRetry(e);
        } catch (UnknownHostException e) {
            throw new DownloadRetry(e);
        } catch (IOException e) {
            // all other io excetption including FileNotFoundException should
            // stop downloading.
            throw new DownloadError(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
