package com.github.axet.wget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.github.axet.wget.info.DownloadError;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;
import com.github.axet.wget.info.DownloadInfo.Part;

public class WGet {

    private DownloadInfo info;

    Direct d;

    File targetFile;

    // size of part if downloaded as multipart
    static public final int CHUNK_SIZE = 1 * 1024 * 1024;

    /**
     * simple download file.
     * 
     * @param source
     * @param target
     */
    public WGet(URL source, File target) {
        create(source, target, new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    /**
     * download with events control.
     * 
     * @param source
     * @param target
     * @param stop
     * @param notify
     */
    public WGet(URL source, File target, AtomicBoolean stop, Runnable notify) {
        create(source, target, stop, notify);
    }

    /**
     * application controlled download / resume
     * 
     * @param info
     * @param targetFile
     * @param stop
     * @param notify
     */
    public WGet(DownloadInfo info, File targetFile, AtomicBoolean stop, Runnable notify) {
        this.info = info;
        this.targetFile = targetFile;
        create(stop, notify);
    }

    void create(URL source, File target, AtomicBoolean stop, Runnable notify) {
        info = new DownloadInfo(source);
        info.extract();
        create(target, stop, notify);
    }

    void create(File target, AtomicBoolean stop, Runnable notify) {
        targetFile = calcName(info, target);
        info.setParts(calcParts());
        create(stop, notify);
    }

    void create(AtomicBoolean stop, Runnable notify) {
        d = createDirect(stop, notify);
    }

    Direct createDirect(AtomicBoolean stop, Runnable notify) {
        // if (info.multipart()) {
        // return new DirectMultipart(info, targetFile, stop, notify);
        // } else
        if (info.range()) {
            return new DirectRange(info, targetFile, stop, notify);
        } else {
            return new DirectSingle(info, targetFile, stop, notify);
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

    ArrayList<DownloadInfo.Part> calcParts() {
        if (!info.range())
            return null;

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
            return parts;
        }

        return null;
    }

    public void download() {
        d.download();
    }

    public DownloadInfo getInfo() {
        return info;
    }

    public static String getHtml(URL source) {
        return getHtml(source, new AtomicBoolean(false));
    }

    public static String getHtml(URL source, AtomicBoolean stop) {
        try {
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
        } catch (FileNotFoundException e) {
            throw new DownloadError(e);
        } catch (IOException e) {
            throw new DownloadRetry(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
