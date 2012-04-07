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
class YTDownloadThread extends Thread {

    VideoQuality max;
    YTD2Base base;
    String url;
    String target;

    boolean join = false;
    Exception e;

    Object statsLock = new Object();
    YouTubeInfo ei;
    YouTubeDownload d;

    public YTDownloadThread(YTD2Base base, String url, String target) {
        this.base = base;
        this.url = url;
        this.target = target;

        final YTDownloadThread that = this;
        ei = new YouTubeInfo(base, url, max);
        d = new YouTubeDownload(ei, target, new Runnable() {
            @Override
            public void run() {
                that.base.changed();
            }
        });
    }

    public void setMaxQuality(VideoQuality max) {
        this.max = max;
    }

    public String getTitle() {
        synchronized (statsLock) {
            return ei.getTitle();
        }
    }

    public long getTotal() {
        synchronized (statsLock) {
            return ei.total;
        }
    }

    public long getCount() {
        synchronized (statsLock) {
            return ei.count;
        }
    }

    public VideoQuality getVideoQuality() {
        synchronized (statsLock) {
            return ei.vq;
        }
    }

    public String getFileName() {
        synchronized (statsLock) {
            return d.getFileName();
        }
    }

    public String getInput() {
        synchronized (statsLock) {
            return ei.input;
        }
    }

    public void setFileName(String file) {
        synchronized (statsLock) {
            d.setFileName(file);
        }
    }

    @Override
    public void run() {
        try {
            ei.extract();
            d.download();
        } catch (Exception e) {
            synchronized (statsLock) {
                this.e = e;
            }
            base.changed();
        }

        synchronized (statsLock) {
            join = true;
        }
        base.changed();
    }
}