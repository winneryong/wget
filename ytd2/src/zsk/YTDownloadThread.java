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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;
// necessary external libraries
// http://hc.apache.org/downloads.cgi -> httpcomponents-client-4.0.3-bin-with-dependencies.tar.gz (or any later version?!)
// plus corresponding sources as Source Attachment within the Eclipse Project Properties
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
//import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.cookie.CookieOrigin;
//import org.apache.http.cookie.CookieSpec;
//import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
//import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * knoedel@section60:~/YouTube Downloads$ url=`wget --save-cookies savecookies.txt --keep-session-cookies --output-document=- http://www.youtube.com/watch?v=9QFK1cLhytY 2>/dev/null | grep --after-context=6 --max-count=1 yt.preload.start | grep img.src | sed -e 's/img.src =//' -e 's/generate_204/videoplayback/'  -e 's/\\\u0026/\&/g' -e 's/\\\//g' -e 's/;//g' -e "s/'//g" -e 's/ //g' -e 's/"//g' ` && wget --load-cookies=savecookies.txt -O videofile.flv ${url} && echo ok || echo nok
 * 
 * works without cookies as well
 *
 */
public class YTDownloadThread extends Thread {
	
	public boolean bDEBUG;
	
	boolean bNODOWNLOAD;

	static int iThreadcount=0;
	int iThreadNo = YTDownloadThread.iThreadcount++; // every download thread get its own number
	
	final String ssourcecodeurl = "http://";
	final String ssourcecodeuri = "[a-zA-Z0-9%&=\\.]";

	String sURL = null;				// main URL (youtube start web page)
	String sTitle = null;			// will be used as filename
	String sFilenameResPart = null;	// can contain a string that prepends the filename
	String sVideoURL = null;		// one video web resource
	String s403VideoURL = null;		// the video URL which we can use as fallback to my wget call
	Vector<String> sNextVideoURL = new Vector<String>();	// list of URLs from webpage source
	String sFileName = null;		// contains the absolute filename
	//CookieStore bcs = null;			// contains cookies after first HTTP GET
	boolean bisinterrupted = false; // basically the same as Thread.isInterrupted()
	int iRecursionCount = -1;		// counted in downloadone() for the 3 webrequest to one video

	String 				sContentType = null;
	BufferedReader		textreader = null;
	BufferedInputStream binaryreader = null;
	HttpGet				httpget = null;
	HttpClient			httpclient = null;
	HttpHost			proxy = null;
	HttpHost			target = null;
	HttpContext			localContext = null;
    HttpResponse		response = null;
	
	public YTDownloadThread(boolean bD) {
		super();
		this.bDEBUG = bD;
		String sv = "thread started: ".concat(this.getMyName()); 
		output(sv); debugoutput(sv);
	} // YTDownloadThread()
	
	boolean downloadone(String sURL) {
		boolean rc = false;
		boolean rc204 = false;
		boolean rc302 = false;
		boolean rc403 = false;
	
		this.iRecursionCount++;
		
		// stop recursion
		try {
			if (sURL.equals("")) return(false);
		} catch (NullPointerException npe) {
			return(false);
		}
		if (JFCMainClient.getbQuitrequested()) return(false); // try to get information about application shutdown
		
		debugoutput("start.");
		
		// TODO GUI option for proxy?
		
		// http://www.youtube.com/watch?v=Mt7zsortIXs&feature=related 1080p !! "Lady Java" is cool, Oracle is not .. hopefully OpenOffice and Java stay open and free
		
		// http://www.youtube.com/watch?v=WowZLe95WDY&feature=related	Tom Petty And the Heartbreakers - Learning to Fly (wih lyrics)
		// http://www.youtube.com/watch?v=86OfBExGSE0&feature=related	URZ 720p
		// http://www.youtube.com/watch?v=cNOP2t9FObw 					Blade 360 - 480
		// http://www.youtube.com/watch?v=HvQBrM_i8bU					MZ 1000 Street Fighter
		
		// http://wiki.squid-cache.org/ConfigExamples/DynamicContent/YouTube
		// using local squid to save download time for tests

		try {
			// determine http_proxy environment variable
			if (!this.getProxy().equals("")) {

				String sproxy = JFCMainClient.sproxy.toLowerCase().replaceFirst("http://", "") ;
				this.proxy = new HttpHost( sproxy.replaceFirst(":(.*)", ""), Integer.parseInt( sproxy.replaceFirst("(.*):", "")), "http");

				SchemeRegistry supportedSchemes = new SchemeRegistry();
				supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, "UTF-8");
				HttpProtocolParams.setUseExpectContinue(params, true);

				ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);

				// with proxy
				this.httpclient = new DefaultHttpClient(ccm, params);
				this.httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, this.proxy);
				this.httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			} else {
				// without proxy
				this.httpclient = new DefaultHttpClient();
				this.httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			}
			this.httpget = new HttpGet( getURI(sURL) );			
			this.target = new HttpHost( getHost(sURL), 80, "http" );
		} catch (Exception e) {
			debugoutput(e.getMessage());
		}
		
        debugoutput("executing request: ".concat( this.httpget.getRequestLine().toString()) );
        debugoutput("uri: ".concat( this.httpget.getURI().toString()) );
        debugoutput("host: ".concat( this.target.getHostName() ));
        debugoutput("using proxy: ".concat( this.getProxy() ));
        
        // we dont need cookies at all because the download runs even without it (like my wget does) - in fact it blocks downloading videos from different webpages, because we do not handle the bcs for every URL (downloading of one video with different resolutions does work)
        /*
        this.localContext = new BasicHttpContext();
        if (this.bcs == null) this.bcs = new BasicCookieStore(); // make cookies persistent, otherwise they would be stored in a HttpContext but get lost after calling org.apache.http.impl.client.AbstractHttpClient.execute(HttpHost target, HttpRequest request, HttpContext context)
		((DefaultHttpClient) httpclient).setCookieStore(this.bcs); // cast to AbstractHttpclient would be best match because DefaultHttpClass is a subclass of AbstractHttpClient
		*/
        
        // TODO maybe we save the video IDs+res that were downloaded to avoid downloading the same video again?
       
		try {
			this.response = this.httpclient.execute(this.target,this.httpget,this.localContext);
		} catch (ClientProtocolException cpe) {
			debugoutput(cpe.getMessage());
		} catch (UnknownHostException uhe) {
			output((JFCMainClient.isgerman()?"Fehler bei der Verbindung zu: ":"error connecting to: ").concat(uhe.getMessage()));
			debugoutput(uhe.getMessage());
		} catch (IOException ioe) {
			debugoutput(ioe.getMessage());
		} catch (IllegalStateException ise) {
			debugoutput(ise.getMessage());
		}
		
		/*
		CookieOrigin cookieOrigin = (CookieOrigin) localContext.getAttribute( ClientContext.COOKIE_ORIGIN);
		CookieSpec cookieSpec = (CookieSpec) localContext.getAttribute( ClientContext.COOKIE_SPEC);
		CookieStore cookieStore = (CookieStore) localContext.getAttribute( ClientContext.COOKIE_STORE) ;
		try { debugoutput("HTTP Cookie store: ".concat( cookieStore.getCookies().toString( )));
		} catch (NullPointerException npe) {} // useless if we don't set our own CookieStore before calling httpclient.execute
		try {
			debugoutput("HTTP Cookie origin: ".concat(cookieOrigin.toString()));
			debugoutput("HTTP Cookie spec used: ".concat(cookieSpec.toString()));
			debugoutput("HTTP Cookie store (persistent): ".concat(this.bcs.getCookies().toString()));
		} catch (NullPointerException npe) {
		}
		*/

		try {
			debugoutput("HTTP response status line:".concat( this.response.getStatusLine().toString()) );
			//for (int i = 0; i < response.getAllHeaders().length; i++) {
			//	debugoutput(response.getAllHeaders()[i].getName().concat("=").concat(response.getAllHeaders()[i].getValue()));
			//}
			// TODO youtube sends a "HTTP/1.1 303 See Other" response if you try to open a webpage that does not exist

			// the second request of a browser is with an URL containing generate_204 which leads to an HTTP response code of (guess) 204! the next query is the same URL with videoplayback instead of generate_204 which leads to an HTTP response code of (guess again) .. no not 200! but 302 and in that response header there is a field Location with a different (host) which we can now request with HTTP GET and then we get a response of (guess :) yes .. 200 and the video resource in the body - whatever the girlsnboys at google had in mind developing this ping pong - we'll never now.
			// but because all nessesary URLs are provided in the source code we dont have to do the same requests as web-browsers do
			// abort if HTTP response code is != 200, != 302 and !=204 - wrong URL?
			// make one exception for 403 - switch to old method of videplayback instead of generate_204
			if (!(rc = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)200(.*)")) & 
					!(rc204 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)204(.*)")) &
					!(rc302 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)302(.*)")) &
					!(rc403 = this.response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)403(.*)"))) {
				debugoutput(this.response.getStatusLine().toString().concat(" ").concat(sURL));
				output(this.response.getStatusLine().toString().concat(" \"").concat(this.sTitle).concat("\""));
				return(rc & rc204 & rc302);
			}
			if (rc204) {
				debugoutput("last response code==204 - download: ".concat(this.sNextVideoURL.get(0)));
				rc = downloadone(this.sNextVideoURL.get(0));
				return(rc);
			}
			if (rc302) 
				debugoutput("location from HTTP Header: ".concat(this.response.getFirstHeader("Location").toString()));
			if (rc403) {
				String smsg = "falling back to former download method."; 
				debugoutput(smsg); output(smsg);
				this.sFilenameResPart = null;
				rc = downloadone(this.s403VideoURL);
			}
		} catch (NullPointerException npe) {
			// if an IllegalStateException was catched while calling httpclient.execute(httpget) a NPE is caught here because
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
			try {
				if (this.response.getFirstHeader("Content-Type").getValue().toLowerCase().matches("^text/html(.*)"))
					this.textreader = new BufferedReader(new InputStreamReader(entity.getContent()));
				else
					this.binaryreader = new BufferedInputStream( entity.getContent());
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            try {
            	// test if we got a webpage
            	this.sContentType = this.response.getFirstHeader("Content-Type").getValue().toLowerCase();
            	if (this.sContentType.matches("^text/html(.*)")) {
            		savetextdata();
            	// test if we got the binary content
            	} else if (this.sContentType.matches("video/(.)*")) {
            		if (JFCMainClient.bNODOWNLOAD)
            			reportheaderinfo();
            		else
            			savebinarydata();
            	} else { // content-type is not video/
            		rc = false;
            		this.sVideoURL = null;
            	}
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
        
       	this.httpclient.getConnectionManager().shutdown();

        debugoutput("done: ".concat(sURL));
		try { 
			debugoutput("try to download video from URL: ".concat(this.sVideoURL));
			rc = downloadone(this.sVideoURL);
			this.sVideoURL = null;
		} catch (NullPointerException npe) {
		}
		
		return(rc);
		
	} // downloadone()

	void reportheaderinfo() {
		if (this.bDEBUG) {
			debugoutput("");
			debugoutput("no-download mode active (ndl on)");
			debugoutput("all HTTP header fields:");
			for (int i = 0; i < this.response.getAllHeaders().length; i++) {
				debugoutput(this.response.getAllHeaders()[i].getName().concat("=").concat(this.response.getAllHeaders()[i].getValue()));
			}
		} else {
			Long iFileSize = Long.parseLong(this.response.getFirstHeader("Content-Length").getValue());
			output("");
			output("no-download active (ndl on)");
			output("some HTTP header fields:");
			output("content-type: ".concat( this.response.getFirstHeader("Content-Type").getValue()) );
			output("content-length: ".concat(iFileSize.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iFileSize/1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iFileSize/1024/1024)).concat(" MiB")) );
		}
		this.sVideoURL = null;
	} // reportheaderinfo()

	void savetextdata() throws IOException {
		// read lines one by one and search for video URL
		String sline = "";
		while (sline != null) {
			sline = this.textreader.readLine();
			try {
				if (this.iRecursionCount==0 && sline.matches("(.*)generate_204(.*)")) {
					sline = sline.replaceFirst("img.src = '?", "");					//debugoutput("URL: ".concat(sline));
					sline = sline.replaceFirst("';", "");							//debugoutput("URL: ".concat(sline));
					sline = sline.replaceFirst("\\u0026", "&");						//debugoutput("URL: ".concat(sline));
					sline = sline.replaceAll("\\\\", "");							//debugoutput("URL: ".concat(sline));
					sline = sline.replaceAll("\\s", "");							debugoutput("img.src URL: ".concat(sline));
					this.s403VideoURL = sline.replaceFirst("generate_204", "videoplayback");	//debugoutput("URL: ".concat(sline)); // this is what my wget command does
					this.sVideoURL = sline;
				}
				if (this.iRecursionCount==0 && sline.matches("( *)var swfConfig =(.*)")) { // 2011-03-08 - source code changed from "var swfHTML" to "var swfConfig"
    				HashMap<String, String> ssourcecodevideourls = new HashMap<String, String>();

					// by anonymous
					sline = sline.replaceFirst(".*\"fmt_url_map\": \"", "").replaceFirst("\".*", "").replace("\\u0026", "&").replace("\\", "");
					String[] ssourcecodeyturls = sline.split(",");
					debugoutput("ssourcecodeuturls.length: ".concat(Integer.toString(ssourcecodeyturls.length)));
					String[] urlStrings = sline.split(",");
					
					for (String urlString : urlStrings) {
						String[] fmtUrlPair = urlString.split("\\|");
						// String fmt = fmtUrlPair[0], url = fmtUrlPair[1];
						ssourcecodevideourls.put(fmtUrlPair[0], fmtUrlPair[1]); // save that URL
						debugoutput(String.format( "video url saved with key %s: %s",fmtUrlPair[0],ssourcecodevideourls.get(fmtUrlPair[0]) ));
						output((JFCMainClient.isgerman()?"gefundene Video URL für Auflösung: ":"found video URL for resolution: ").concat(fmtUrlPair[0].equals("22")?"720p":fmtUrlPair[0].equals("35")?"480p?":fmtUrlPair[0].equals("18")?"270p?":fmtUrlPair[0].equals("34")?"360p?":fmtUrlPair[0].equals("37")?"1080p":fmtUrlPair[0].equals("5")?"240p?":"unknown resolution?"));
					} // for

					debugoutput("ssourcecodevideourls.length: ".concat(Integer.toString(ssourcecodevideourls.size())));
					String sno="";
					// figure out what resolution-button is pressed now and fill list with possible URLs
					switch (JFCMainClient.getIdlbuttonstate()) {
					case 4:
						// 37|22 - better quality first
						this.sNextVideoURL.add(0, ssourcecodevideourls.get(sno="37"));
						this.sNextVideoURL.add(1, ssourcecodevideourls.get("22"));
						this.sNextVideoURL.add(2, ssourcecodevideourls.get("35"));
						this.sNextVideoURL.add(3, ssourcecodevideourls.get("34"));
						this.sNextVideoURL.add(4, ssourcecodevideourls.get("18"));
						this.sNextVideoURL.add(5, ssourcecodevideourls.get("5"));
						break;
					case 2:
						// 35|34
						this.sNextVideoURL.add(0, ssourcecodevideourls.get(sno="35"));
						this.sNextVideoURL.add(1, ssourcecodevideourls.get("34"));
						this.sNextVideoURL.add(2, ssourcecodevideourls.get("18"));
						this.sNextVideoURL.add(3, ssourcecodevideourls.get("5"));
						break;
					case 1:
						// 18|5
						this.sNextVideoURL.add(0, ssourcecodevideourls.get(sno="18"));
						this.sNextVideoURL.add(1, ssourcecodevideourls.get("5"));
						break;
					default:
						this.sNextVideoURL = null;
						this.sVideoURL = null;
						this.sFilenameResPart = null;
						break;
					}
					
					if (this.sNextVideoURL.get(0)==null && this.sNextVideoURL.get(1)==null) {
						String smsg = JFCMainClient.isgerman()?"Konnte Video URL für ausgewählte Auflösung nicht finden. Versuche geringere Auflösung!":"could not find video url for selected resolution! trying lower res...";
						output(smsg); debugoutput(smsg);
					}
					
					// remove null entries in list - we later try to download the first (index 0) and if it fails the next one (at index 1) and so on
					for (int x=this.sNextVideoURL.size()-1;x>=0;x--) {
						if (this.sNextVideoURL.get(x)==null) this.sNextVideoURL.remove(x);
					}
					
					try { 
						debugoutput("choosed resolution ".concat(sno).concat(": ").concat(this.sNextVideoURL.get(0))); 
						this.sFilenameResPart = sno.equals("5")?"(LD)":""; // Standard is 360p/480p and HD is either 720p or 1080p but it's a mp4-file so adding text the the filename provides no extra information
					} catch (NullPointerException npe) {
					}
					
					// 2011-03-08 new - skip generate_204
					this.sVideoURL = this.sNextVideoURL.get(0);
				}
				// TODO exchange HTML characters to UTF-8 = http://sourceforge.net/projects/htmlparser/ 
				if (this.iRecursionCount==0 && sline.matches("(.*)<meta name=\"title\" content=(.*)")) {
					this.setTitle( sline.replaceFirst("<meta name=\"title\" content=", "").trim().replaceAll("&amp;", "&").replaceAll("[!\"#$%'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]", "") );	
				}
			} catch (NullPointerException npe) {
			}
		} // while
	} // savetextdata()

	void savebinarydata() throws IOException {
		FileOutputStream fos = null;
		try {
			File f; Integer idupcount = 0;
			String sdirectorychoosed;
			synchronized (JFCMainClient.frame.directorytextfield) {
				sdirectorychoosed = JFCMainClient.frame.directorytextfield.getText();
			}
			String sfilename = this.getTitle()/*.replaceAll(" ", "_")*/.concat( this.sFilenameResPart==null?"":this.sFilenameResPart );
    		debugoutput("title: ".concat(this.getTitle()).concat("sfilename: ").concat(sfilename));
			do {
				f = new File(sdirectorychoosed, sfilename.concat((idupcount>0?"(".concat(idupcount.toString()).concat(")"):"")).concat(".").concat(this.sContentType.replaceFirst("video/", "").replaceAll("x-", "")));
				idupcount += 1;
			} while (f.exists());
			this.setFileName(f.getAbsolutePath());
			
			Long iBytesReadSum = (long) 0;
			Long iPercentage = (long) -1;
			Long iBytesMax = Long.parseLong(this.response.getFirstHeader("Content-Length").getValue());
			fos = new FileOutputStream(f);
			
			debugoutput(String.format("writing %d bytes to: %s",iBytesMax,this.getFileName()));
			output((JFCMainClient.isgerman()?"Dateigröße von \"":"file size of \"").concat(this.getTitle()).concat("\" = ").concat(iBytesMax.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iBytesMax/1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iBytesMax/1024/1024)).concat(" MiB")));
		    
			byte[] bytes = new byte[4096];
			Integer iBytesRead = 1;
			String sOldURL = JFCMainClient.szDLSTATE.concat(this.sURL);
			String sNewURL = "";
			
			// adjust blocks of percentage to output - larger files are shown with smaller pieces
			Integer iblocks = 10; if (iBytesMax>20*1024*1024) iblocks=4; if (iBytesMax>32*1024*1024) iblocks=2; if (iBytesMax>56*1024*1024) iblocks=1;
			while (!this.bisinterrupted && iBytesRead>0) {
				iBytesRead = this.binaryreader.read(bytes);
				iBytesReadSum += iBytesRead;
				// drop a line every x% of the download 
				if ( (((iBytesReadSum*100/iBytesMax) / iblocks) * iblocks) > iPercentage ) {
					iPercentage = (((iBytesReadSum*100/iBytesMax) / iblocks) * iblocks);
					sNewURL = JFCMainClient.szDLSTATE.concat("(").concat(Long.toString(iPercentage).concat(" %) ").concat(this.sURL));
					JFCMainClient.exchangeYTURLInList(sOldURL, sNewURL);
					sOldURL = sNewURL ; 
				}
				try {fos.write(bytes,0,iBytesRead);} catch (IndexOutOfBoundsException ioob) {}
				this.bisinterrupted = JFCMainClient.getbQuitrequested(); // try to get information about application shutdown
			} // while
			
			JFCMainClient.exchangeYTURLInList(sNewURL, JFCMainClient.szDLSTATE.concat(this.sURL));
			
			// rename files if download was interrupted before completion of download
			if (this.bisinterrupted && iBytesReadSum<iBytesMax) {
				try {
					// this part is especially for our M$-Windows users because of the different behavior of File.renameTo() in contrast to non-windows
					// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298  and others
					// even with Java 1.6.0_22 the renameTo() does not work directly on M$-Windows! 
					fos.close();
				} catch (Exception e) {
				}
//				System.gc(); // we don't have to do this but to be sure the file handle gets released we do a thread sleep 
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
		         
				// this part runs on *ix platforms without closing the FileOutputStream explicitly
				this.httpclient.getConnectionManager().shutdown(); // otherwise binaryreader.close() would cause the entire datastream to be transmitted 
				debugoutput(String.format("download canceled. (%d)",(iBytesRead)));
				changeFileNamewith("CANCELED.");
				String smsg = "renaming unfinished file to: ".concat( this.getFileName() );
				output(smsg); debugoutput(smsg);
				
				if (!f.renameTo(new File( this.getFileName()))) {
					smsg = "error renaming unfinished file to: ".concat( this.getFileName() );
    				output(smsg); debugoutput(smsg);
				}
			}
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
		if (sfilesep.equals("\\")) sfilesep += sfilesep; // on m$-windows we need to escape the \

		String sdirectorychoosed="";
		String[] srenfilename = this.getFileName().split(sfilesep);
		
		try {
			for (int i = 0; i < srenfilename.length-1; i++) {
				sdirectorychoosed += srenfilename[i].concat((i<srenfilename.length-1)?sfilesep:""); // constructing folder where file is saved now (could be changed in GUI already)
			}
		} catch (ArrayIndexOutOfBoundsException aioobe) {}
		
		String sfilename = srenfilename[srenfilename.length-1];
		debugoutput("changeFileNamewith() sfilename: ".concat(sfilename));
		do {
			 // filename will be prepended with a parameter string and possibly a duplicate counter
			f = new File(sdirectorychoosed, string.concat((idupcount>0?"(".concat(idupcount.toString()).concat(")"):"")).concat(sfilename));
			idupcount += 1;
		} while (f.exists());
		
		debugoutput("changeFileNamewith() new filename: ".concat(f.getAbsolutePath()));
		this.setFileName(f.getAbsolutePath());
		
	} // changeFileNamewith

	String getProxy() {
		String sproxy = JFCMainClient.sproxy;
		if (sproxy==null) return(""); else return(sproxy);
	} // getProxy() 

	String getURI(String sURL) {
		String suri = "/".concat(sURL.replaceFirst(JFCMainClient.szYTHOSTREGEX, ""));
		return(suri);
	} // getURI

	String getHost(String sURL) {
		String shost = sURL.replaceFirst(JFCMainClient.szYTHOSTREGEX, "");
		shost = sURL.substring(0, sURL.length()-shost.length());
		shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
		return(shost);
	} // gethost
	
	String getTitle() {
		if (this.sTitle != null) return this.sTitle; else return("");
	}

	void setTitle(String sTitle) {
		this.sTitle = sTitle;
	}
	
	String getFileName() {
		if (this.sFileName != null) return this.sFileName; else return("");
	}

	void setFileName(String sFileName) {
		this.sFileName = sFileName;
	}

	void debugoutput (String s) {
		if (!JFCMainClient.bDEBUG)
			return;
		// sometimes this happens:  Exception in thread "Thread-2" java.lang.Error: Interrupted attempt to aquire write lock (on quit only)
		// maybe we should use synchronize anyway
		try {
			JFCMainClient.addTextToConsole("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s));
		} catch (Exception e) {
			try { Thread.sleep(50); } catch (InterruptedException e1) {}
			try { JFCMainClient.addTextToConsole("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s)); } catch (Exception e2) {}
		}
		System.out.println("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s));
	} // debugoutput
	
	void output (String s) {
		if (JFCMainClient.bDEBUG)
			return;
		JFCMainClient.addTextToConsole("#info - ".concat(s));
	} // output
	
	String getMyName() {
		return this.getClass().getName().concat(Integer.toString(this.iThreadNo));
	} // getMyName()
	
	public void setbDEBUG(boolean bDEBUG) {
		this.bDEBUG = bDEBUG;
	} // setbDEBUG
	
	public void run() {
		boolean bDOWNLOADOK = false;
		while (!this.bisinterrupted) {
			try {
				synchronized (JFCMainClient.frame.dlm) {
//					debugoutput("going to sleep.");
					JFCMainClient.frame.dlm.wait(1000); // check for new URLs (if they got pasted faster than threads removing them)
//					debugoutput("woke up ".concat(this.getClass().getName()));
					this.bisinterrupted = JFCMainClient.getbQuitrequested(); // if quit was pressed while this threads works it would not get the InterruptedException and therefore prevent application shutdown 
				}
				// TODO check what kind of website the URL is from - this class can only handle YouTube-URLs ... we add other video sources later
				this.sURL = JFCMainClient.getfirstURLFromList();
				output((JFCMainClient.isgerman()?"versuche herunterzuladen: ":"try to download: ").concat(this.sURL));
				JFCMainClient.removeURLFromList(this.sURL);
				JFCMainClient.addYTURLToList(JFCMainClient.szDLSTATE.concat(this.sURL));
				
				this.bNODOWNLOAD = JFCMainClient.getbNODOWNLOAD(); // copy ndl-state because this thread should end with a complete file (and report so) even if someone switches to nodl before this thread is finished
				
				// download one webresource and show result
				bDOWNLOADOK = downloadone(this.sURL); this.iRecursionCount=-1;
				if (bDOWNLOADOK && !this.bNODOWNLOAD)
					output((JFCMainClient.isgerman()?"fertig heruntergeladen: ":"download complete: ").concat("\"").concat(this.getTitle()).concat("\"").concat(" to ").concat(this.getFileName()));
				else
					output((JFCMainClient.isgerman()?"Fehler beim herunterladen: ":"error downloading: ").concat(this.sURL));
				
				JFCMainClient.removeURLFromList(JFCMainClient.szDLSTATE.concat(this.sURL));
			} catch (InterruptedException e) {
				this.bisinterrupted = true;
			} catch (NullPointerException npe) {
//				debugoutput("npe - nothing to download?");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // while
		debugoutput("thread ended: ".concat(this.getMyName()));
	} // run()

} // class YTDownloadThread