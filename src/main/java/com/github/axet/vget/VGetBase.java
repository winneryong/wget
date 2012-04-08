package com.github.axet.vget;

import java.util.regex.Pattern;

import com.github.axet.vget.VGet.VideoQuality;

class VGetBase {
    private Boolean bQuitrequested = false;

    VGetThread t1;

    synchronized Boolean getbQuitrequested() {
        return bQuitrequested;
    }

    synchronized void setbQuitrequested(Boolean bQuitrequested) {
        this.bQuitrequested = bQuitrequested;
    }

    void shutdownAppl() {
        // running downloads are difficult to terminate (Thread.isInterrupted()
        // does not work there)
        synchronized (bQuitrequested) {
            bQuitrequested = true;
        }
        try {
            try {
                t1.interrupt();
            } catch (NullPointerException npe) {
            }
            try {
                t1.join();
            } catch (NullPointerException npe) {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    } // shutdownAppl()

    /**
     * @param string
     * @param regex
     * @param replaceWith
     * @return changed String
     */
    String replaceAll(String string, String regex, String replaceWith) {
        Pattern myPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return (myPattern.matcher(string).replaceAll(replaceWith));
    } // replaceAll

    void download(String url, String sdirectory, VideoQuality max) {
        t1 = new VGetThread(this, url, sdirectory);
        t1.setMaxQuality(max);
    }

    void changed() {
    }
}
