package com.github.axet.vget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import com.github.axet.vget.info.VGetInfo;

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

            max = ei.getVideo();
            URL url = new URL(max.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(VGetBase.CONNECT_TIMEOUT);
            conn.setReadTimeout(VGetBase.READ_TIMEOUT);

            String sContentType = conn.getContentType();

            if (!sContentType.contains("video/")) {
                throw new RuntimeException("unable to download video");
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

            byte[] bytes = new byte[4096];
            Integer iBytesRead = 1;

            BufferedInputStream binaryreader = null;
            binaryreader = new BufferedInputStream(conn.getInputStream());

            total = conn.getContentLength();

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

    public void download() {
        savebinarydata();
    }

}
