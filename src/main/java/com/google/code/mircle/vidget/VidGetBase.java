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
package com.google.code.mircle.vidget;

import java.util.regex.Pattern;

import com.google.code.mircle.vidget.VidGet.VideoQuality;

class VidGetBase {
    private Boolean bQuitrequested = false;

    VidGetThread t1;

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
        t1 = new VidGetThread(this, url, sdirectory);
        t1.setMaxQuality(max);
    }

    void changed() {
    }
}
