/**
 *  This file is part of ytd2
 *
 *  ytd2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ytd2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with ytd2.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.google.code.mircle.ytd2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.Vector;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;

import com.google.code.mircle.ytd2.YTD2.VideoQuality;

/**
 * knoedel@section60:~/YouTube Downloads$ url=`wget --save-cookies
 * savecookies.txt --keep-session-cookies --output-document=-
 * http://www.youtube.com/watch?v=9QFK1cLhytY 2>/dev/null | grep
 * --after-context=6 --max-count=1 yt.preload.start | grep img.src | sed -e
 * 's/img.src =//' -e 's/generate_204/videoplayback/' -e 's/\\\u0026/\&/g' -e
 * 's/\\\//g' -e 's/;//g' -e "s/'//g" -e 's/ //g' -e 's/"//g' ` && wget
 * --load-cookies=savecookies.txt -O videofile.flv ${url} && echo ok || echo nok
 * 
 * works without cookies as well
 * 
 */
class YouTubeDownload {

    public boolean bDEBUG;

    boolean bNODOWNLOAD;

    static int iThreadcount = 0;
    int iThreadNo = YouTubeDownload.iThreadcount++; // every download thread
                                                    // get its own number

    final String ssourcecodeurl = "http://";
    final String ssourcecodeuri = "[a-zA-Z0-9%&=\\.]";

    String sFilenameResPart = null; // can contain a string that prepends the
                                    // filename
    String sVideoURL = null; // one video web resource
    String s403VideoURL = null; // the video URL which we can use as fallback to
                                // my wget call
    Vector<String> sNextVideoURL = new Vector<String>(); // list of URLs from
                                                         // webpage source
    String sFileName = null; // contains the absolute filename
    // CookieStore bcs = null; // contains cookies after first HTTP GET
    boolean bisinterrupted = false; // basically the same as
                                    // Thread.isInterrupted()
    int iRecursionCount = -1; // counted in downloadone() for the 3 webrequest
                              // to one video

    String sContentType = null;
    BufferedReader textreader = null;
    BufferedInputStream binaryreader = null;
    String sdirectorychoosed;
    YTD2Base ytd2;
    VideoQuality max;

    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 5000;

    Object statsLock = new Object();
    String input;
    long count = 0;
    long total = 0;
    boolean join = false;
    VideoQuality vq;
    Exception e;

    YouTubeInfo ei;
    Runnable notify;

    public YouTubeDownload(YTD2Base base, YouTubeInfo e, String sdirectorychoosed, Runnable notify) {
        this.ei = e;
        this.sdirectorychoosed = sdirectorychoosed;
        this.notify = notify;
        this.ytd2 = base;
    } // YTDownloadThread()

    void reportheaderinfo() {
        this.sVideoURL = null;
    } // reportheaderinfo()

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

    void savebinarydata(String sdirectorychoosed) {
        FileOutputStream fos = null;
        try {
            File f;
            if (getFileName() == null) {
                Integer idupcount = 0;
                String sfilename = ei.sTitle/* .replaceAll(" ", "_") */.concat(this.sFilenameResPart == null ? ""
                        : this.sFilenameResPart);

                sfilename = replaceBadChars(sfilename);

                do {
                    f = new File(sdirectorychoosed, sfilename
                            .concat((idupcount > 0 ? " (".concat(idupcount.toString()).concat(")") : "")).concat(".")
                            .concat(ei.sContentType.replaceFirst("video/", "").replaceAll("x-", "")));
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

            synchronized (statsLock) {
                total = ei.iBytesMax;
            }

            byte[] bytes = new byte[4096];
            Integer iBytesRead = 1;

            // adjust blocks of percentage to output - larger files are shown
            // with smaller pieces
            Integer iblocks = 10;
            if (ei.iBytesMax > 20 * 1024 * 1024)
                iblocks = 4;
            if (ei.iBytesMax > 32 * 1024 * 1024)
                iblocks = 2;
            if (ei.iBytesMax > 56 * 1024 * 1024)
                iblocks = 1;
            
            URL url = new URL(ei.sVideoURL);
            this.binaryreader = new BufferedInputStream(url.openStream());

            while (!ei.bisinterrupted && iBytesRead > 0) {
                iBytesRead = this.binaryreader.read(bytes);
                if (iBytesRead > 0) {
                    iBytesReadSum += iBytesRead;

                    synchronized (statsLock) {
                        count = iBytesReadSum;
                    }
                }

                notify.run();

                // drop a line every x% of the download
                if ((((iBytesReadSum * 100 / ei.iBytesMax) / iblocks) * iblocks) > iPercentage) {
                    iPercentage = (((iBytesReadSum * 100 / ei.iBytesMax) / iblocks) * iblocks);
                }
                if (iBytesRead > 0)
                    fos.write(bytes, 0, iBytesRead);
                this.bisinterrupted = ytd2.getbQuitrequested(); // try to get
                                                                // information
                                                                // about
                                                                // application
                                                                // shutdown
            } // while

            // rename files if download was interrupted before completion of
            // download
            if (this.bisinterrupted && iBytesReadSum < ei.iBytesMax) {
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
                this.textreader.close();
            } catch (Exception e) {
            }
            try {
                this.binaryreader.close();
            } catch (Exception e) {
            }
        } // try
    } // savebinarydata()

    void changeFileNamewith(String string) {
        File f = null;
        Integer idupcount = 0;
        String sfilesep = System.getProperty("file.separator");
        if (sfilesep.equals("\\"))
            sfilesep += sfilesep; // on m$-windows we need to escape the \

        String sdirectorychoosed = "";
        String[] srenfilename = this.getFileName().split(sfilesep);

        try {
            for (int i = 0; i < srenfilename.length - 1; i++) {
                sdirectorychoosed += srenfilename[i].concat((i < srenfilename.length - 1) ? sfilesep : ""); // constructing
                                                                                                            // folder
                                                                                                            // where
                                                                                                            // file
                                                                                                            // is
                                                                                                            // saved
                                                                                                            // now
                                                                                                            // (could
                                                                                                            // be
                                                                                                            // changed
                                                                                                            // in
                                                                                                            // GUI
                                                                                                            // already)
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
        }

        String sfilename = srenfilename[srenfilename.length - 1];

        do {
            // filename will be prepended with a parameter string and possibly a
            // duplicate counter
            f = new File(sdirectorychoosed, string.concat(
                    (idupcount > 0 ? "(".concat(idupcount.toString()).concat(")") : "")).concat(sfilename));
            idupcount += 1;
        } while (f.exists());

        this.setFileName(f.getAbsolutePath());

        notify.run();
    } // changeFileNamewith

    String getProxy() {
        String sproxy = YTD2.sproxy;
        if (sproxy == null)
            return ("");
        else
            return (sproxy);
    } // getProxy()

    String getURI(String sURL) {
        String suri = "/".concat(sURL.replaceFirst(YTD2.szYTHOSTREGEX, ""));
        return (suri);
    } // getURI

    String getHost(String sURL) {
        String shost = sURL.replaceFirst(YTD2.szYTHOSTREGEX, "");
        shost = sURL.substring(0, sURL.length() - shost.length());
        shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
        return (shost);
    } // gethost

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

    String getMyName() {
        return this.getClass().getName().concat(Integer.toString(this.iThreadNo));
    } // getMyName()

    public void setbDEBUG(boolean bDEBUG) {
        this.bDEBUG = bDEBUG;
    } // setbDEBUG

    public void download() {
        savebinarydata(sdirectorychoosed);
    } // run()

} // class YTDownloadThread