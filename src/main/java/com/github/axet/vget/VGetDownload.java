package com.github.axet.vget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.axet.vget.info.DownloadError;
import com.github.axet.vget.info.DownloadRetry;
import com.github.axet.vget.info.VGetInfo;
import com.github.axet.vget.info.VGetInfo.VideoQuality;
import com.github.axet.vget.info.VGetInfo.VideoURL;

class VGetDownload {

    String targetDir;

    String target = null;
    VGetBase ytd2;

    String input;

    // total bytes
    long total = 0;
    // downloaded bytes
    long count = 0;

    VGetInfo ei;
    Runnable notify;

    VGetInfo.VideoURL max;

    public VGetDownload(VGetBase base, VGetInfo e, String sdirectorychoosed, Runnable notify) {
        this.ei = e;
        this.targetDir = sdirectorychoosed;
        this.notify = notify;
        this.ytd2 = base;
    }

    /**
     * Drop all foribiden characters from filename
     * 
     * @param f
     *            input file name
     * @return normalized file name
     */
    static String replaceBadChars(String f) {
        String replace = " ";
        f = f.replaceAll("/", replace);
        f = f.replaceAll("\\\\", replace);
        f = f.replaceAll(":", replace);
        f = f.replaceAll("\\?", replace);
        f = f.replaceAll("\\\"", replace);
        f = f.replaceAll("\\*", replace);
        f = f.replaceAll("<", replace);
        f = f.replaceAll(">", replace);
        f = f.replaceAll("\\|", replace);
        f = f.trim();
        f = StringUtils.removeEnd(f, ".");
        f = f.trim();

        String ff;
        while (!(ff = f.replaceAll("  ", " ")).equals(f)) {
            f = ff;
        }

        return f;
    }

    void savebinarydata() {
        FileOutputStream fos = null;

        try {
            try {
                max = getVideo();
                URL url = new URL(max.url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(VGetBase.CONNECT_TIMEOUT);
                conn.setReadTimeout(VGetBase.READ_TIMEOUT);

                String sContentType = conn.getContentType();

                if (sContentType == null || !sContentType.contains("video/")) {
                    throw new DownloadRetry("unable to download video, bad content");
                }

                File f;
                if (target == null) {
                    Integer idupcount = 0;

                    String sfilename = replaceBadChars(ei.getTitle());
                    String ext = sContentType.replaceFirst("video/", "").replaceAll("x-", "");

                    do {
                        String add = idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "";

                        f = new File(targetDir, sfilename + add + "." + ext);
                        idupcount += 1;
                    } while (f.exists());
                    this.target = f.getAbsolutePath();
                } else {
                    f = new File(target);
                    f.delete();
                }

                fos = new FileOutputStream(f);

                byte[] bytes = new byte[4 * 1024];
                Integer iBytesRead = 1;

                BufferedInputStream binaryreader = null;
                binaryreader = new BufferedInputStream(conn.getInputStream());

                total = conn.getContentLength();

                while (!ytd2.getbQuitrequested() && iBytesRead > 0) {
                    iBytesRead = binaryreader.read(bytes);

                    if (iBytesRead > 0) {
                        count += iBytesRead;
                        fos.write(bytes, 0, iBytesRead);
                    }

                    notify.run();
                }
                binaryreader.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (fos != null)
                    fos.close();
            }
        } catch (IOException e) {
            throw new DownloadRetry(e);
        }
    }

    public void download() {
        savebinarydata();
    }

    public VideoURL getVideo() {
        Map<VideoQuality, String> sNextVideoURL = ei.getVideos();

        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p2304, VideoQuality.p1080, VideoQuality.p720,
                VideoQuality.p480, VideoQuality.p360, VideoQuality.p270, VideoQuality.p224 };

        for (int i = 0; i < avail.length; i++) {
            if (sNextVideoURL.containsKey(avail[i]))
                return new VideoURL(avail[i], sNextVideoURL.get(avail[i]));
        }

        throw new DownloadError("no video with required quality found");
    }
}
