package com.github.axet.wget.examples;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.github.axet.wget.WGet;

public class SimpleDownload {

    public static void main(String[] args) {
        try {
            // direct file download. one thread. single thread.
            WGet w = new WGet(new URL("http://www.dd-wrt.com/routerdb/de/download/D-Link/DIR-300/A1/ap61.ram/2049"),
                    new File("/Users/axet/Downloads/"));

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
