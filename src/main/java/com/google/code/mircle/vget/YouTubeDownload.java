package com.google.code.mircle.vget;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;

import com.google.code.mircle.vget.VGet.VideoQuality;

class YouTubeDownload {

    String sFilenameResPart = null; // can contain a string that prepends the
                                    // filename
    String sVideoURL = null; // one video web resource
    String s403VideoURL = null; // the video URL which we can use as fallback to
                                // my wget call
    Vector<String> sNextVideoURL = new Vector<String>(); // list of URLs from
                                                         // webpage source
    String sFileName = null; // contains the absolute filename

    BufferedInputStream binaryreader = null;
    String target;
    VGetBase ytd2;

    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 5000;

    Object statsLock = new Object();
    String input;
    long count = 0;

    YouTubeInfo ei;
    Runnable notify;

    long iBytesMax;

    final static VideoQuality DEFAULT_QUALITY = VideoQuality.p1080;

    VideoQuality max = DEFAULT_QUALITY;

    public YouTubeDownload(VGetBase base, YouTubeInfo e, String sdirectorychoosed, Runnable notify) {
        this.ei = e;
        this.target = sdirectorychoosed;
        this.notify = notify;
        this.ytd2 = base;
    }

    boolean addVideo(String s) {
        if (s != null) {
            sNextVideoURL.add(s);
            return true;
        }

        return false;
    }

    /**
     * Drop all foribiden characters from filename
     * 
     * @param sfilename
     *            input file name
     * @return normalized file name
     */
    static String replaceBadChars(String sfilename) {
        String replace = " ";
        sfilename = sfilename.replaceAll("/", replace);
        sfilename = sfilename.replaceAll("\\\\", replace);
        sfilename = sfilename.replaceAll(":", replace);
        sfilename = sfilename.replaceAll("\\?", replace);
        sfilename = sfilename.replaceAll("\\\"", replace);
        sfilename = sfilename.replaceAll("\\*", replace);
        sfilename = sfilename.replaceAll("<", replace);
        sfilename = sfilename.replaceAll(">", replace);
        sfilename = sfilename.replaceAll("\\|", replace);
        sfilename = sfilename.replaceAll("  ", " ");
        sfilename = sfilename.trim();
        sfilename = StringUtils.removeEnd(sfilename, ".");
        sfilename = sfilename.trim();

        return sfilename;
    }

    void savebinarydata() {
        FileOutputStream fos = null;
        try {
            VideoQuality vq = getVideoUrl();
            max = vq;

            URL url = new URL(ei.sNextVideoURL.get(vq));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            String sContentType = conn.getContentType();

            File f;
            if (getFileName() == null) {
                Integer idupcount = 0;
                String sfilename = ei.sTitle/* .replaceAll(" ", "_") */.concat(this.sFilenameResPart == null ? ""
                        : this.sFilenameResPart);

                sfilename = replaceBadChars(sfilename);

                do {
                    f = new File(target, sfilename
                            .concat((idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "")).concat(".")
                            .concat(sContentType.replaceFirst("video/", "").replaceAll("x-", "")));
                    idupcount += 1;
                } while (f.exists());
                this.setFileName(f.getAbsolutePath());
            } else {
                f = new File(getFileName());
            }

            Long iBytesReadSum = (long) 0;
            Long iPercentage = (long) -1;

            // does reader + yotube know how to skip?
            // binaryreader.skip(f.length());
            f.delete();

            fos = new FileOutputStream(f);

            byte[] bytes = new byte[4096];
            Integer iBytesRead = 1;

            this.binaryreader = new BufferedInputStream(conn.getInputStream());

            iBytesMax = conn.getContentLength();

            // adjust blocks of percentage to output - larger files are shown
            // with smaller pieces
            Integer iblocks = 10;
            if (iBytesMax > 20 * 1024 * 1024)
                iblocks = 4;
            if (iBytesMax > 32 * 1024 * 1024)
                iblocks = 2;
            if (iBytesMax > 56 * 1024 * 1024)
                iblocks = 1;

            while (!ytd2.getbQuitrequested() && iBytesRead > 0) {
                iBytesRead = this.binaryreader.read(bytes);
                if (iBytesRead > 0) {
                    iBytesReadSum += iBytesRead;

                    synchronized (statsLock) {
                        count = iBytesReadSum;
                    }
                }

                notify.run();

                // drop a line every x% of the download
                if ((((iBytesReadSum * 100 / iBytesMax) / iblocks) * iblocks) > iPercentage) {
                    iPercentage = (((iBytesReadSum * 100 / iBytesMax) / iblocks) * iblocks);
                }
                if (iBytesRead > 0)
                    fos.write(bytes, 0, iBytesRead);
            }

            // rename files if download was interrupted before completion of
            // download
            if (ytd2.getbQuitrequested() && iBytesReadSum < iBytesMax) {
                try {
                    // this part is especially for our M$-Windows users because
                    // of the different behavior of File.renameTo() in contrast
                    // to non-windows
                    // see
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298
                    // and others
                    // even with Java 1.6.0_22 the renameTo() does not work
                    // directly on M$-Windows!
                    fos.close();
                } catch (Exception e) {
                }
                // System.gc(); // we don't have to do this but to be sure the
                // file handle gets released we do a thread sleep
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.sVideoURL = null;
            try {
                fos.close();
            } catch (Exception e) {
            }
            try {
                this.binaryreader.close();
            } catch (Exception e) {
            }
        }
    }

    String getFileName() {
        synchronized (statsLock) {
            if (this.sFileName != null)
                return this.sFileName;
            else
                return null;
        }
    }

    void setFileName(String sFileName) {
        synchronized (statsLock) {
            this.sFileName = sFileName;
        }
    }

    public void download() {
        savebinarydata();
    }

    VideoQuality getVideoUrl() {
        VideoQuality[] avail = new VideoQuality[] { VideoQuality.p1080, VideoQuality.p720, VideoQuality.p480,
                VideoQuality.p360, VideoQuality.p240, VideoQuality.p120 };

        int i = 0;
        for (; i < avail.length; i++) {
            if (avail[i] == max)
                break;
        }

        for (; i < avail.length; i++) {
            if (ei.sNextVideoURL.containsKey(avail[i]))
                return avail[i];
        }

        throw new RuntimeException("no video with required quality found");
    }
}
