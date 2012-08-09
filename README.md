wget java download library.

support single thread, single thread with download continue / resume, and multithread download.

== Exceptions

Here is a three kind of exceptions.

1) Fatal exception. all RuntimeException's
  We shall stop application

2) DownloadError (extends RuntimeException)
  We unable to process following url and shall stop to download it
  
3) DownloadRetry (caused by IOException)
  We're having temporary problems. Shall retry download after a delay.

== Examples

        // simple example. direct one call download

        try {
            WGet w = new WGet(new URL("http://www.dd-wrt.com/routerdb/de/download/D-Link/DIR-300/A1/ap61.ram/2049"),
                    new File("/Users/axet/Downloads/"));
            w.download();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // application controlled download with start / resume functionality

        try {
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
            w.download();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        