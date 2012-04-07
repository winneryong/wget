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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

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
    HttpGet httpget = null;
    HttpClient httpclient = null;
    HttpHost proxy = null;
    HttpHost target = null;
    HttpContext localContext = null;
    HttpResponse response = null;
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

    boolean downloadone(String sURL, String sdirectorychoosed, VideoQuality vd) throws Exception {
        boolean rc = false;
        boolean rc204 = false;
        boolean rc302 = false;
        boolean rc403 = false;

        this.iRecursionCount++;

        // stop recursion
        try {
            if (sURL.equals(""))
                return (false);
        } catch (NullPointerException npe) {
            return (false);
        }
        if (ytd2.getbQuitrequested())
            return (false); // try to get information about application shutdown

        // TODO GUI option for proxy?

        // http://www.youtube.com/watch?v=Mt7zsortIXs&feature=related 1080p !!
        // "Lady Java" is cool, Oracle is not .. hopefully OpenOffice and Java
        // stay open and free

        // http://www.youtube.com/watch?v=WowZLe95WDY&feature=related Tom Petty
        // And the Heartbreakers - Learning to Fly (wih lyrics)
        // http://www.youtube.com/watch?v=86OfBExGSE0&feature=related URZ 720p
        // http://www.youtube.com/watch?v=cNOP2t9FObw Blade 360 - 480
        // http://www.youtube.com/watch?v=HvQBrM_i8bU MZ 1000 Street Fighter

        // http://wiki.squid-cache.org/ConfigExamples/DynamicContent/YouTube
        // using local squid to save download time for tests

        try {
            // determine http_proxy environment variable
            if (!this.getProxy().equals("")) {

                String sproxy = YTD2.sproxy.toLowerCase().replaceFirst("http://", "");
                this.proxy = new HttpHost(sproxy.replaceFirst(":(.*)", ""), Integer.parseInt(sproxy.replaceFirst(
                        "(.*):", "")), "http");

                SchemeRegistry supportedSchemes = new SchemeRegistry();
                supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

                HttpParams params = new BasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, "UTF-8");
                HttpProtocolParams.setUseExpectContinue(params, true);

                HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT);
                HttpConnectionParams.setSoTimeout(params, READ_TIMEOUT);

                ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);

                // with proxy
                this.httpclient = new DefaultHttpClient(ccm, params);
                this.httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, this.proxy);
                this.httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
            } else {
                // without proxy

                HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT);
                HttpConnectionParams.setSoTimeout(params, READ_TIMEOUT);

                this.httpclient = new DefaultHttpClient(params);
                this.httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
            }
            this.httpget = new HttpGet(getURI(sURL));
            this.target = new HttpHost(getHost(sURL), 80, "http");
        } catch (Exception e) {
            synchronized (statsLock) {
                this.e = e;
            }
            notify.run();
        }

        // we dont need cookies at all because the download runs even without it
        // (like my wget does) - in fact it blocks downloading videos from
        // different webpages, because we do not handle the bcs for every URL
        // (downloading of one video with different resolutions does work)
        /*
         * this.localContext = new BasicHttpContext(); if (this.bcs == null)
         * this.bcs = new BasicCookieStore(); // make cookies persistent,
         * otherwise they would be stored in a HttpContext but get lost after
         * calling
         * org.apache.http.impl.client.AbstractHttpClient.execute(HttpHost
         * target, HttpRequest request, HttpContext context)
         * ((DefaultHttpClient) httpclient).setCookieStore(this.bcs); // cast to
         * AbstractHttpclient would be best match because DefaultHttpClass is a
         * subclass of AbstractHttpClient
         */

        // TODO maybe we save the video IDs+res that were downloaded to avoid
        // downloading the same video again?

        this.response = this.httpclient.execute(this.target, this.httpget, this.localContext);

        try {
            // for (int i = 0; i < response.getAllHeaders().length; i++) {
            // debugoutput(response.getAllHeaders()[i].getName().concat("=").concat(response.getAllHeaders()[i].getValue()));
            // }
            // TODO youtube sends a "HTTP/1.1 303 See Other" response if you try
            // to open a webpage that does not exist

            // the second request of a browser is with an URL containing
            // generate_204 which leads to an HTTP response code of (guess) 204!
            // the next query is the same URL with videoplayback instead of
            // generate_204 which leads to an HTTP response code of (guess
            // again) .. no not 200! but 302 and in that response header there
            // is a field Location with a different (host) which we can now
            // request with HTTP GET and then we get a response of (guess :) yes
            // .. 200 and the video resource in the body - whatever the
            // girlsnboys at google had in mind developing this ping pong -
            // we'll never now.
            // but because all nessesary URLs are provided in the source code we
            // dont have to do the same requests as web-browsers do
            // abort if HTTP response code is != 200, != 302 and !=204 - wrong
            // URL?
            // make one exception for 403 - switch to old method of videplayback
            // instead of generate_204
            if (!(rc = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)200(.*)"))
                    & !(rc204 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)204(.*)"))
                    & !(rc302 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)302(.*)"))
                    & !(rc403 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)403(.*)"))) {
                return (rc & rc204 & rc302);
            }
            if (rc204) {
                rc = downloadone(this.sNextVideoURL.get(0), sdirectorychoosed, vd);
                return (rc);
            }
            if (rc403) {
                this.sFilenameResPart = null;
                rc = downloadone(this.s403VideoURL, sdirectorychoosed, vd);
            }
        } catch (NullPointerException npe) {
            // if an IllegalStateException was catched while calling
            // httpclient.execute(httpget) a NPE is caught here because
            // response.getStatusLine() == null
            this.sVideoURL = null;
        }

        HttpEntity entity = null;
        try {
            entity = this.response.getEntity();
        } catch (NullPointerException npe) {
        }

        // try to read HTTP response body
        if (entity != null) {
            {
                if (this.response.getFirstHeader("Content-Type").getValue().toLowerCase().matches("^text/html(.*)"))
                    this.textreader = new BufferedReader(new InputStreamReader(entity.getContent(),
                            EntityUtils.getContentCharSet(entity)));
                else
                    this.binaryreader = new BufferedInputStream(entity.getContent());
            }

            {
                // test if we got a webpage
                this.sContentType = this.response.getFirstHeader("Content-Type").getValue().toLowerCase();
                if (this.sContentType.matches("video/(.)*")) {
                    savebinarydata(sdirectorychoosed);
                } else { // content-type is not video/
                    rc = false;
                    this.sVideoURL = null;
                }
            }
        } // if (entity != null)

        this.httpclient.getConnectionManager().shutdown();

        try {
            rc = downloadone(this.sVideoURL, sdirectorychoosed, vd);
            this.sVideoURL = null;
        } catch (NullPointerException npe) {
        }

        return (rc);

    } // downloadone()

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
                String sfilename = ei.getTitle()/* .replaceAll(" ", "_") */.concat(
                        this.sFilenameResPart == null ? "" : this.sFilenameResPart);

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
            while (!ei.bisinterrupted && iBytesRead > 0) {
                iBytesRead = ei.binaryreader.read(bytes);
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

                // this part runs on *ix platforms without closing the
                // FileOutputStream explicitly
                this.httpclient.getConnectionManager().shutdown(); // otherwise
                                                                   // binaryreader.close()
                                                                   // would
                                                                   // cause the
                                                                   // entire
                                                                   // datastream
                                                                   // to be
                                                                   // transmitted
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