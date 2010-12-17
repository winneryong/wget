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
package zsk;

import java.io.BufferedReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 * knoedel@section60:~/YouTube Downloads$ url=`wget --save-cookies savecookies.txt --keep-session-cookies --output-document=- http://www.youtube.com/watch?v=9QFK1cLhytY 2>/dev/null | grep --after-context=6 --max-count=1 yt.preload.start | grep img.src | sed -e 's/img.src =//' -e 's/generate_204/videoplayback/' -e 's/\\\//g' -e 's/;//g' -e "s/'//g" -e 's/ //g'` && wget --load-cookies=savecookies.txt -O videofile.flv ${url} && echo ok || echo nok
 * 
 * works without cookies as well
 *
 */
public class DownloadThread extends Thread {
	static int iThreadcount=0;
	int iThreadNo = DownloadThread.iThreadcount++;
	
	boolean bDEBUG;
	private String sTitle = null;
	private String sVideoURL = null;
	private String sFileName = null;
	private boolean bisinterrupted = false;
	
	public DownloadThread(boolean bD) {
		super();
		this.bDEBUG = bD;
		String sv = "thread started: ".concat(this.getMyName()); 
		output(sv); debugoutput(sv);
	} // DownloadThread()
	
	boolean downloadone(String sURL) {
		boolean rc = false;
	
		// stop recursion
		try {
			if (sURL.equals("")) return(false);
		} catch (NullPointerException npe) {
			return(false);
		}
		
		debugoutput("start.");
		
		// TODO GUI option for proxy?
		// TODO GUI option for HD/480/360/240
		
		HttpGet httpget = null;
		HttpClient httpclient = null;
		HttpHost proxy = null;
		HttpHost target = null;

		try {
			// determine http_proxy var
			if (!this.getProxy().equals("")) {

				proxy = new HttpHost(System.getenv("http_proxy").replaceFirst(":(.*)", ""), Integer.parseInt( System.getenv("http_proxy").replaceFirst("(.*):", "")), "http");

				SchemeRegistry supportedSchemes = new SchemeRegistry();
				supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, "UTF-8");
				HttpProtocolParams.setUseExpectContinue(params, true);

				ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);

				// with proxy
				httpclient = new DefaultHttpClient(ccm, params);
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			} else {
				// without proxy
				httpclient = new DefaultHttpClient();
			}
			httpget = new HttpGet( getURI(sURL) );			
			target = new HttpHost( getHost(sURL), 80, "http" );
		} catch (Exception e) {
			debugoutput(e.getMessage());
		}
		
        debugoutput("executing request: ".concat( httpget.getRequestLine().toString()) );
        debugoutput("uri: ".concat( httpget.getURI().toString()) );
        debugoutput("host: ".concat( target.getHostName() ));
        debugoutput("via proxy: ".concat( this.getProxy() ));

        HttpResponse response = null;
        
		try {
			response = httpclient.execute(target,httpget);
		} catch (ClientProtocolException cpe) {
			debugoutput(cpe.getMessage());
		} catch (IOException ioe) {
			debugoutput(ioe.getMessage());
		} catch (IllegalStateException ise) {
			debugoutput(ise.getMessage());
		}
		
		try {
			debugoutput("HTTP response status line:".concat( response.getStatusLine().toString()) );
			for (int i = 0; i < response.getAllHeaders().length; i++) {
				debugoutput(response.getAllHeaders()[i].getName().concat("=").concat(response.getAllHeaders()[i].getValue()));
			}
			// TODO youtube sends a "HTTP/1.1 303 See Other" response if you try to open a webpage that does not exist

			// abort if HTTP response code is != 200 - wrong URL?
			if (!(rc = response.getStatusLine().toString().matches("^(H|h)(T|t)(T|t)(P|p)(.*)200(.*)"))) {
				return rc;
			}
		} catch (NullPointerException npe) {
			// if an IllegalStateException was catched while calling httpclient.execute(httpget) a NPE is caught here because
			// response.getStatusLine() == null
			this.sVideoURL = null;
		}
		
		HttpEntity entity = null;
        try {
            entity = response.getEntity();
        } catch (NullPointerException npe) {
        }
        
        // try to read HTTP response body
        if (entity != null) {
        	BufferedReader textreader = null;
        	BufferedInputStream binaryreader = null;
			try {
				if (response.getFirstHeader("Content-Type").getValue().toLowerCase().matches("^text/html(.*)"))
					textreader = new BufferedReader(new InputStreamReader(entity.getContent()));
				else
					binaryreader = new BufferedInputStream( entity.getContent());
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            try {
            	// test if we got a webpage
            	if (response.getFirstHeader("Content-Type").getValue().toLowerCase().matches("^text/html(.*)")) {
            		// read lines one by one and search for video URL
            		String sline = "";
            		while (sline != null) {
            			sline = textreader.readLine();
            			try {
            				if (sline.matches("(.*)generate_204(.*)")) {
            					sline = sline.replaceFirst("generate_204", "videoplayback"); debugoutput("URL: ".concat(sline));
            					sline = sline.replaceFirst("img.src = '", "");debugoutput("URL: ".concat(sline));
            					sline = sline.replaceFirst("';", "");debugoutput("URL: ".concat(sline));
            					sline = sline.replaceAll("\\\\", "");debugoutput("URL: ".concat(sline));
            					sline = sline.replaceAll("\\s", "");debugoutput("URL: ".concat(sline));
            					this.sVideoURL = sline;
            				} else if (sline.matches("(.*)<meta name=\"title\" content=(.*)")) {
            					this.setTitle( sline.replaceFirst("<meta name=\"title\" content=", "").trim().replaceAll("[!\"#$%&'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]", "") );	
            				}
            			} catch (NullPointerException npe) {
            			}
            		} // while
            	// test if we got the binary content
            	} else if (response.getFirstHeader("Content-Type").getValue().toLowerCase().matches("video/x-flv")) {
            		FileOutputStream fos = null;
            		try {
            			
            			File f; Integer idupcount = 0;
            			String sdirectorychoosed;
            			synchronized (JFCMainClient.frame.directorytextfield) {
            				sdirectorychoosed = JFCMainClient.frame.directorytextfield.getText();
            			}
            			String sfilename = this.getTitle()/*.replaceAll(" ", "_")*/;
	            		debugoutput("title: ".concat(this.getTitle()).concat("sfilename: ").concat(sfilename));
            			do {
            				f = new File(sdirectorychoosed, sfilename.concat((idupcount>0?"(".concat(idupcount.toString()).concat(")"):"")).concat(".flv"));
            				idupcount += 1;
            			} while (f.exists());
            			this.setFileName(f.getAbsolutePath());
            			
            			Long iBytesReadSum = (long) 0;
            			Long iPercentage = (long) -1;
            			Long iBytesMax = Long.parseLong(response.getFirstHeader("Content-Length").getValue());
            			fos = new FileOutputStream(f);
            			
            			debugoutput(String.format("writing %d bytes to: %s",iBytesMax,this.getFileName()));
            			output("file size of \"".concat(this.getTitle()).concat("\" = ").concat(iBytesMax.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iBytesMax/1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iBytesMax/1024/1024)).concat(" MiB")));
            		    
//            			debugoutput("Endianness: ".concat(ByteOrder.nativeOrder().toString()));

            			byte[] bytes = new byte[4096];
            			Integer iBytesRead = 1;
            			while (!this.bisinterrupted & iBytesRead>0) {
            				iBytesRead = binaryreader.read(bytes);
            				iBytesReadSum += iBytesRead;
            				// every 10% of the download we drop a line for the user 
            				if ( ((((iBytesReadSum*100/iBytesMax) / 10) % 10) * 10) != iPercentage ) {
            					iPercentage = ((((iBytesReadSum*100/iBytesMax) / 10) % 10) * 10);
            					output(Long.toString(iPercentage).concat("% of  \"").concat(this.getTitle()).concat("\"") );
            					debugoutput( this.getMyName().concat("  ").concat(Long.toString(iPercentage).concat("% ")) );
            				}
            				try {fos.write(bytes,0,iBytesRead);} catch (IndexOutOfBoundsException ioob) {}
            				// TODO if a downloading thread gets terminated, than we should consider deleting the unfinished file OR continuing download at offset? :)
            				synchronized (JFCMainClient.bQuitrequested) { this.bisinterrupted = JFCMainClient.bQuitrequested; } // try to get informatation about application shutdown
            			} 
            			if (JFCMainClient.bQuitrequested & iBytesReadSum<iBytesMax) debugoutput(String.format("dowloading canceled. (%d)",(iBytesRead)));
            			debugoutput("done writing.");
            		} catch (FileNotFoundException fnfe) {
            			throw(fnfe)		;
            		} catch (IOException ioe) {
            			debugoutput("IOException");
            			throw(ioe);
            		} finally {
            			this.sVideoURL = null;
            			try {
							fos.close();
						} catch (Exception e) {
						}
                        try {
        					textreader.close();
        				} catch (Exception e) {
        				}
                        try {
        					binaryreader.close();
        				} catch (Exception e) {
        				}
            		} // try
            	} // if .. content-type
            } catch (IOException ex) {
                try {
					throw ex;
				} catch (IOException e) {
					e.printStackTrace();
				}
            } catch (RuntimeException ex) {
                try {
					throw ex;
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
        } //if (entity != null)
        
       	httpclient.getConnectionManager().shutdown();

        debugoutput("done: ".concat(sURL));
		try { 
			debugoutput("try to download video from URL: ".concat(this.sVideoURL));
			rc = downloadone(this.sVideoURL);
			this.sVideoURL = null;
		} catch (NullPointerException npe) {
		}
		
		return(rc);
		
	} // downloadone()

	private String getProxy() {
		String sproxy = System.getenv("http_proxy");
		if (sproxy==null) return(""); else return(sproxy);
	} // getProxy() 

	private String getURI(String sURL) {
		String suri = "/".concat(sURL.replaceFirst(JFCMainClient.szHOSTREGEX, ""));
		return(suri);
	} // getURI

	private String getHost(String sURL) {
		String shost = sURL.replaceFirst(JFCMainClient.szHOSTREGEX, "");
		shost = sURL.substring(0, sURL.length()-shost.length());
		shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
		return(shost);
	} // gethost

	public void run() {
		String sURL = null;
		while (!this.bisinterrupted) {
			try {
				synchronized (JFCMainClient.frame.dlm) {
//					debugoutput("going to sleep.");
					JFCMainClient.frame.dlm.wait(2000); // to check for new URLs (if they got pasted faster than threads removing them)
//					debugoutput("woke up ".concat(this.getClass().getName()));
					output("try to download: ".concat(sURL = JFCMainClient.getfirstURLFromList()));
					JFCMainClient.removeURLFromList(sURL);
				} // synchronized (JFCMainClient.frame.dlm)
				
				// download one webresource and show result
				output((downloadone(sURL)?"download completed: ":"error downloading: ").concat("\"").concat(this.getTitle()).concat("\"").concat(" to "));

			} catch (InterruptedException e) {
				this.bisinterrupted = true; // only when we use the interface Runnable to use this.isInterrupted()
			} catch (NullPointerException npe) {
//				debugoutput("npe - nothing to download?");
//				npe.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // while
		debugoutput("thread ended: ".concat(this.getMyName()));
	} // run()
	
	private String getTitle() {
		if (this.sTitle != null) return this.sTitle; else return("");
	}

	private void setTitle(String sTitle) {
		this.sTitle = sTitle;
	}
	
	private String getFileName() {
		if (this.sFileName != null) return this.sFileName; else return("");
	}

	private void setFileName(String sFileName) {
		this.sFileName = sFileName;
	}

	void debugoutput (String s) {
		if (!JFCMainClient.bDEBUG)
			return;

		JFCMainClient.addTextToConsole("#DEBUG ".concat(s));
		System.out.println("#DEBUG ".concat(s));
	} // debugoutput
	
	void output (String s) {
		if (JFCMainClient.bDEBUG)
			return;
		JFCMainClient.addTextToConsole("#info - ".concat(s));
	} // output
	
	String getMyName() {
		return this.getClass().getName().concat(Integer.toString(this.iThreadNo));
	} // getMyName()

} // class downloadthread

