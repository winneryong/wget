package com.github.axet.vget;

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
    }

    void download(String url, String sdirectory) {
        t1 = new VGetThread(this, url, sdirectory);
    }

    void changed() {
    }
}
