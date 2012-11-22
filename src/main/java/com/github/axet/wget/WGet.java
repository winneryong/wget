package com.github.axet.wget;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadError;

public class WGet {

    private DownloadInfo info;

    Direct d;

    File targetFile;

    public interface HtmlLoader {
        public void notifyRetry(int delay, Throwable e);

        public void notifyDownloading();
    }

    /**
     * download with events control.
     * 
     * @param source
     * @param target
     */
    public WGet(URL source, File target) {
        create(source, target);
    }

    /**
     * application controlled download / resume. you should specify targetfile
     * name exactly. since you are choise resume / multipart download.
     * application unable to control file name choise / creation.
     * 
     * @param info
     * @param targetFile
     * @param stop
     * @param notify
     */
    public WGet(DownloadInfo info, File targetFile) {
        this.info = info;
        this.targetFile = targetFile;
        create();
    }

    void create(URL source, File target) {
        info = new DownloadInfo(source);
        info.extract();
        create(target);
    }

    void create(File target) {
        targetFile = calcName(info, target);
        create();
    }

    void create() {
        d = createDirect();
    }

    Direct createDirect() {
        if (info.multipart()) {
            return new DirectMultipart(info, targetFile);
        } else if (info.range()) {
            return new DirectRange(info, targetFile);
        } else {
            return new DirectSingle(info, targetFile);
        }
    }

    public static File calcName(URL source, File target) {
        DownloadInfo info = new DownloadInfo(source);
        info.extract();

        return calcName(info, target);
    }

    public static File calcName(DownloadInfo info, File target) {
        // target -
        // 1) can point to directory.
        // - generate exclusive (1) name.
        // 2) to exisiting file
        // 3) to non existing file

        String name = null;

        name = info.getContentFilename();

        if (name == null)
            name = new File(info.getSource().getPath()).getName();

        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String nameNoExt = FilenameUtils.removeExtension(name);
        String ext = FilenameUtils.getExtension(name);

        File targetFile = null;

        if (target.isDirectory()) {
            targetFile = FileUtils.getFile(target, name);
            int i = 1;
            while (targetFile.exists()) {
                targetFile = FileUtils.getFile(target, nameNoExt + " (" + i + ")." + ext);
                i++;
            }
        } else {
            try {
                FileUtils.forceMkdir(new File(target.getParent()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            targetFile = target;
        }

        return targetFile;
    }

    public void download() {
        try {
            download(new AtomicBoolean(false), new Runnable() {
                @Override
                public void run() {
                }
            });
        } catch (InterruptedException e) {
            throw new DownloadError(e);
        }
    }

    public void download(AtomicBoolean stop, Runnable notify) throws InterruptedException {
        try {
            d.download(stop, notify);
        } catch (InterruptedException e) {
            info.setState(URLInfo.States.STOPPED);
            notify.run();
            throw e;
        } catch (RuntimeException e) {
            info.setState(URLInfo.States.ERROR);
            notify.run();
            throw e;
        }
    }

    public DownloadInfo getInfo() {
        return info;
    }

    public static String getHtml(URL source) {
        try {
            return getHtml(source, new HtmlLoader() {

                @Override
                public void notifyRetry(int delay, Throwable e) {
                }

                @Override
                public void notifyDownloading() {
                }
            }, new AtomicBoolean(false));
        } catch (InterruptedException e) {
            throw new DownloadError(e);
        }
    }

    public static String getHtml(final URL source, final HtmlLoader load, final AtomicBoolean stop)
            throws InterruptedException {
        String html = RetryFactory.wrap(stop, new RetryFactory.WrapReturn<String>() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
                load.notifyRetry(delay, e);
            }

            @Override
            public void notifyDownloading() {
                load.notifyDownloading();
            }

            @Override
            public String run() throws IOException {
                URL u = source;
                HttpURLConnection con = (HttpURLConnection) u.openConnection();
                con.setConnectTimeout(Direct.CONNECT_TIMEOUT);
                con.setReadTimeout(Direct.READ_TIMEOUT);
                InputStream is = con.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                String line = null;

                StringBuilder contents = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    contents.append(line);
                    contents.append("\n");

                    if (stop.get())
                        return null;
                }

                return contents.toString();
            }
        });

        return html;
    }
}
