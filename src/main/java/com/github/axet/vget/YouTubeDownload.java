package com.github.axet.vget;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.github.axet.vget.VGet.VideoQuality;

class YouTubeDownload {

    String sVideoURL = null; // one video web resource
    String s403VideoURL = null; // the video URL which we can use as fallback to
                                // my wget call
    Vector<String> sNextVideoURL = new Vector<String>(); // list of URLs from
                                                         // webpage source
    String sFileName = null; // contains the absolute filename

    String target;
    VGetBase ytd2;

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
        try {
            FileOutputStream fos = null;

            max = getVideoUrl();
            URL url = new URL(ei.sNextVideoURL.get(max));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(VGetThread.CONNECT_TIMEOUT);
            conn.setReadTimeout(VGetThread.READ_TIMEOUT);

            String sContentType = conn.getContentType();

            if (!sContentType.contains("video/")) {
                throw new RuntimeException("unable to download video");
            }

            File f;
            if (getFileName() == null) {
                Integer idupcount = 0;

                String sfilename = replaceBadChars(ei.sTitle);
                String ext = sContentType.replaceFirst("video/", "").replaceAll("x-", "");

                do {
                    String add = idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "";

                    f = new File(target, sfilename + add + "." + ext);
                    idupcount += 1;
                } while (f.exists());
                this.setFileName(f.getAbsolutePath());
            } else {
                f = new File(getFileName());
                f.delete();
            }

            fos = new FileOutputStream(f);

            byte[] bytes = new byte[4096];
            Integer iBytesRead = 1;

            BufferedInputStream binaryreader = null;
            binaryreader = new BufferedInputStream(conn.getInputStream());

            iBytesMax = conn.getContentLength();

            while (!ytd2.getbQuitrequested() && iBytesRead > 0) {
                iBytesRead = binaryreader.read(bytes);
                if (iBytesRead > 0) {
                    count += iBytesRead;
                }

                if (iBytesRead > 0)
                    fos.write(bytes, 0, iBytesRead);

                notify.run();
            }

            fos.close();
            binaryreader.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String getFileName() {
        return this.sFileName;
    }

    void setFileName(String sFileName) {
        this.sFileName = sFileName;
    }

    public void download() {
        savebinarydata();
    }

    String readHtml(HttpURLConnection conn) {
        BufferedReader textreader = null;
        try {
            textreader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                    conn.getContentEncoding() == null ? "UTF-8" : conn.getContentEncoding()));
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }

        StringBuilder contents = new StringBuilder();
        String line = "";
        while (line != null) {
            try {
                line = textreader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            contents.append(line + "\n");
        }
        return contents.toString();
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
