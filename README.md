# wget

wget direct / multithread / singlethread java download library.

Support single thread, single thread with download continue / resume, and multithread downloads.

## Exceptions

Here is a three kind of exceptions.

1) Fatal exception. all RuntimeException's
  We shall stop application

2) DownloadError (extends RuntimeException)
  We unable to process following url and shall stop to download it. It may be rised by problem with local file.

3) ExtractError (extends RuntimeException)
  We unable to extract information from the source URL. Shall stop downloading.
  
4) DownloadInterrupted (extends RuntimeException)
  App or Library interrupted the downloading thread

## Example direct download

    package com.github.axet.wget;
    
    import java.io.File;
    import java.net.MalformedURLException;
    import java.net.URL;
    
    import com.github.axet.wget.WGet;
    
    public class Example {
    
        public static void main(String[] args) {
            try {
                // choise internet url (ftp, http)
                URL url = new URL("http://www.dd-wrt.com/routerdb/de/download/D-Link/DIR-300/A1/ap61.ram/2049");
                // choise target folder or filename "/Users/axet/Downloads/ap61.ram"
                File target = new File("/Users/axet/Downloads/");
                // initialize wget object
                WGet w = new WGet(url, target);
                // single thread download. will return here only when file download
                // is complete (or error raised).
                w.download();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (RuntimeException allDownloadExceptions) {
                allDownloadExceptions.printStackTrace();
            }
        }
    }

## Application Managed Download

    package com.github.axet.wget;
    
    import java.io.File;
    import java.net.URL;
    import java.util.concurrent.atomic.AtomicBoolean;
    
    import com.github.axet.wget.info.DownloadInfo;
    import com.github.axet.wget.info.DownloadInfo.Part;
    import com.github.axet.wget.info.ex.DownloadMultipartError;
    
    public class Example {
    
        AtomicBoolean stop = new AtomicBoolean(false);
        DownloadInfo info;
        long last;
    
        public void run() {
            try {
                Runnable notify = new Runnable() {
                    @Override
                    public void run() {
                        // notify app or save download state
                        // you can extract information from DownloadInfo info;
                        switch (info.getState()) {
                        case EXTRACTING:
                        case EXTRACTING_DONE:
                        case DONE:
                            System.out.println(info.getState());
                            break;
                        case RETRYING:
                            System.out.println(info.getDelay());
                            break;
                        case DOWNLOADING:
                            long now = System.currentTimeMillis();
                            if (now - 1000 > last) {
                                last = now;
                                System.out.println(String.format("%.2f", info.getCount() / (float) info.getLength()));
                            }
                            break;
                        default:
                            break;
                        }
                    }
                };
    
                // choise file
                URL url = new URL("http://download.virtualbox.org/virtualbox/4.2.4/VirtualBox-4.2.4-81684-OSX.dmg");
                // initialize url information object
                info = new DownloadInfo(url, stop, notify);
                // extract infromation from the web
                info.extract();
                // enable multipart donwload
                info.enableMultipart();
                // Choise target file
                File target = new File("/Users/axet/Downloads/VirtualBox-4.2.4-81684-OSX.dmg");
                // create wget downloader
                WGet w = new WGet(info, target, stop, notify);
                // will blocks until download finishes
                w.download();
            } catch (DownloadMultipartError e) {
                for (Part p : e.getInfo().getParts()) {
                    Throwable ee = p.getException();
                    if (ee != null)
                        ee.printStackTrace();
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    
        public static void main(String[] args) {
            Example e = new Example();
            e.run();
        }
    
    }

## Example Multithread Download

    package com.github.axet.wget;
    
    import java.io.File;
    import java.net.URL;
    import java.util.concurrent.atomic.AtomicBoolean;
    
    import com.github.axet.wget.info.DownloadInfo;
    
    public class Example {
    
        public static void main(String[] args) {
            try {
                AtomicBoolean stop = new AtomicBoolean(false);
                Runnable notify = new Runnable() {
    
                    @Override
                    public void run() {
                        // notify app or save download state
                        // you can extract information from DownloadInfo info;
                        System.out.print(".");
                    }
                };
    
                // choise file
                URL url = new URL("http://download.virtualbox.org/virtualbox/4.2.4/VirtualBox-4.2.4-81684-OSX.dmg");
                // initialize url information object
                DownloadInfo info = new DownloadInfo(url);
                // extract infromation from the web
                info.extract();
                // enable multipart donwload
                info.enableMultipart();
                // Choise target file
                File target = new File("/Users/axet/Downloads/VirtualBox-4.2.4-81684-OSX.dmg");
                // create wget downloader
                WGet w = new WGet(info, target, stop, notify);
                // will blocks until download finishes
                w.download();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    
    }

## Cetral Maven Repo

    <dependency>
      <groupId>com.github.axet</groupId>
      <artifactId>wget</artifactId>
      <version>0.1.10</version>
    </dependency>
