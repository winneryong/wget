package com.github.axet.wget.examples;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;

public class ApplicationManagedDownload {

    public static void main(String[] args) {
        try {

            // create or restore info from a inputstream or xml by
            // de-serialization
            final DownloadInfo info = new DownloadInfo(new URL(
                    "http://www.dd-wrt.com/routerdb/de/download/D-Link/DIR-300/A1/ap61.ram/2049"));
            info.extract();

            Runnable notify = new Runnable() {
                @Override
                public void run() {
                    float progress = info.getCount() / info.getLength();
                    System.out.println("progress " + progress);
                }
            };

            AtomicBoolean stop = new AtomicBoolean(false);

            WGet w = new WGet(info, new File("/Users/axet/Downloads/"), stop, notify);

            // single thread download. will return here only when file download
            // is complete (or error raised)
            w.download();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RuntimeException allDownloadExceptions) {
            allDownloadExceptions.printStackTrace();
        }
    }

}
