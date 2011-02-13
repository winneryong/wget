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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * knoedel@section60:~/YouTube Downloads$ url=`wget --save-cookies savecookies.txt --keep-session-cookies --output-document=- http://www.youtube.com/watch?v=9QFK1cLhytY 2>/dev/null | grep --after-context=6 --max-count=1 yt.preload.start | grep img.src | sed -e 's/img.src =//' -e 's/generate_204/videoplayback/' -e 's/\\\//g' -e 's/;//g' -e "s/'//g" -e 's/ //g'` && wget --load-cookies=savecookies.txt -O videofile.flv ${url} && echo ok || echo nok
 * 
 * works without cookies as well
 *
 */
public class YTDownloadThread extends Thread {
	
	public boolean bDEBUG;

	static int iThreadcount=0;
	int iThreadNo = YTDownloadThread.iThreadcount++; // every download thread get its own number
	
	final String ssourcecodeurl = "http://";
	final String ssourcecodeuri = "[a-zA-Z0-9%&=\\.]";

	private String sURL = null;				// main URL (youtube start web page)
	private String sTitle = null;			// will be used as filename
	private String sFilenameResPart = null;	// can contain a string that prepends the filename
	private String sVideoURL = null;		// one video web resource
	private String s403VideoURL = null;		// the video URL which we can use as fallback to my wget call
	private String sNextVideoURL = null;	// will be downloaed after 204
	private String sFileName = null;		// contains the absolute filename
	//private CookieStore bcs = null;			// contains cookies after first HTTP GET
	private boolean bisinterrupted = false; // basically the same as Thread.isInterrupted()
	private int iRecursionCount = -1;		// counted in downloadone() for the 3 webrequest to one video
	
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
		
		// lately found: http://wiki.squid-cache.org/ConfigExamples/DynamicContent/YouTube
		// using local squid to save download time for tests
		
		HttpGet			httpget = null;
		HttpClient		httpclient = null;
		HttpHost		proxy = null;
		HttpHost		target = null;
		HttpContext		localContext = null;
        HttpResponse	response = null;

		try {
			// determine http_proxy environment variable
			if (!this.getProxy().equals("")) {

				String sproxy = JFCMainClient.sproxy.toLowerCase().replaceFirst("http://", "") ;
				proxy = new HttpHost( sproxy.replaceFirst(":(.*)", ""), Integer.parseInt( sproxy.replaceFirst("(.*):", "")), "http");

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
				httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			} else {
				// without proxy
				httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			}
			httpget = new HttpGet( getURI(sURL) );			
			target = new HttpHost( getHost(sURL), 80, "http" );
		} catch (Exception e) {
			debugoutput(e.getMessage());
		}
		
        debugoutput("executing request: ".concat( httpget.getRequestLine().toString()) );
        debugoutput("uri: ".concat( httpget.getURI().toString()) );
        debugoutput("host: ".concat( target.getHostName() ));
        debugoutput("using proxy: ".concat( this.getProxy() ));
        
        localContext = new BasicHttpContext();
        //if (this.bcs == null) this.bcs = new BasicCookieStore(); // make cookies persistent, otherwise they would be stored in a HttpContext but get lost after calling org.apache.http.impl.client.AbstractHttpClient.execute(HttpHost target, HttpRequest request, HttpContext context)
		//((DefaultHttpClient) httpclient).setCookieStore(this.bcs); // cast to AbstractHttpclient would be best match because DefaultHttpClass is a subclass of AbstractHttpClient
        // we dont need cookies at all because the download runs even without it (like my wget does) - in fact its blocks downloading of videos from different webpages, because we do not handle the bcs for every URL (downloading of one video with different resolutions does work)
        // TODO maybe we save the video IDs that were downloaded to avoid downloading the same video again? (or we dont to let the user download different resolutions of the same video)
       
		try {
			response = httpclient.execute(target,httpget,localContext);
		} catch (ClientProtocolException cpe) {
			debugoutput(cpe.getMessage());
		} catch (UnknownHostException uhe) {
			output("error connecting to: ".concat(uhe.getMessage()));
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
			debugoutput("HTTP response status line:".concat( response.getStatusLine().toString()) );
			//for (int i = 0; i < response.getAllHeaders().length; i++) {
			//	debugoutput(response.getAllHeaders()[i].getName().concat("=").concat(response.getAllHeaders()[i].getValue()));
			//}
			// TODO youtube sends a "HTTP/1.1 303 See Other" response if you try to open a webpage that does not exist

			// the second request of a browser is with an URL containing generate_204 which leads to an HTTP response code of (guess) 204! the next query is the same URL with videoplayback instead of generate_204 which leads to an HTTP response code of (guess again) .. no not 200! but 302 and in that response header there is a field Location with a different (host) which we can now request with HTTP GET and then we get a response of (guess :) yes .. 200 and the video resource in the body - whatever the girlsnboys at google had in mind developing this ping pong - we'll never now.
			// but because all nessesary URLs are provided in the source code we dont have to do the same requests as web-browsers do
			// abort if HTTP response code is != 200, != 302 and !=204 - wrong URL?
			// make one exception for 403 - switch to old method of videplayback instead of generate_204
			if (!(rc = response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)200(.*)")) & 
					!(rc204 = response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)204(.*)")) &
					!(rc302 = response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)302(.*)")) &
					!(rc403 = response.getStatusLine().toString().toLowerCase().matches("^(http)(.*)403(.*)"))) {
				debugoutput(response.getStatusLine().toString().concat(" ").concat(sURL));
				output(response.getStatusLine().toString().concat(" \"").concat(this.sTitle).concat("\""));
				return(rc & rc204 & rc302);
			}
			if (rc204) {
				debugoutput("last response code==204 - download: ".concat(this.sNextVideoURL));
				rc = downloadone(this.sNextVideoURL);
				return(rc);
			}
			if (rc302) 
				debugoutput("location from HTTP Header: ".concat(response.getFirstHeader("Location").toString()));
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
            	String sContentType = response.getFirstHeader("Content-Type").getValue().toLowerCase();
            	if (sContentType.matches("^text/html(.*)")) {
            		// read lines one by one and search for video URL
            		String sline = "";
            		while (sline != null) {
            			sline = textreader.readLine();
            			try {
            				if (this.iRecursionCount==0 && sline.matches("(.*)generate_204(.*)")) {
            					sline = sline.replaceFirst("img.src = '", "");					//debugoutput("URL: ".concat(sline));
            					sline = sline.replaceFirst("';", "");							//debugoutput("URL: ".concat(sline));
            					sline = sline.replaceAll("\\\\", "");							//debugoutput("URL: ".concat(sline));
            					sline = sline.replaceAll("\\s", "");							debugoutput("img.src URL: ".concat(sline));
            					this.s403VideoURL = sline.replaceFirst("generate_204", "videoplayback");	//debugoutput("URL: ".concat(sline)); // this is what my wget command does
            					this.sVideoURL = sline;
            				}
            				if (this.iRecursionCount==0 && sline.matches("( *)var swfHTML =(.*)")) {
                				HashMap<String, String> ssourcecodevideourls = new HashMap<String, String>();
                				String shmkey = null; // key for hashmap containg video URLs from webpage source code
                				Integer iidx = null;

            					sline = sline.toLowerCase().replaceFirst(".*\" : \"","\""); // we use the part for non-IE browsers
            					sline = sline.replace("%25", "%").replace("%2c",",").replace("%7c", "|").replace("%3f", "?").replace("%3d", "=").replace("%26", "&").replace("%2f", "/").replace("%3a", ":");
            					
            					String[] ssourcecodeyturls = sline.split(this.ssourcecodeurl); // that block of javascript contains all videoURLs twice - we take the first ones without "||".. 
            					debugoutput("ssourcecodeuturls.length: ".concat(Integer.toString(ssourcecodeyturls.length)));
            					final String szITAG = "itag=";
            					for (int i = ssourcecodeyturls.length-1; i >= 0; i--) {
           							if (ssourcecodeyturls[i].toLowerCase().matches("(.*)youtube.com/videoplayback(.*)")) {
           								ssourcecodeyturls[i] = "http://".concat( ssourcecodeyturls[i].replaceFirst("\\|\\|(.*)", "") );
           								shmkey = ssourcecodeyturls[i].substring( iidx=ssourcecodeyturls[i].indexOf(szITAG), iidx+szITAG.length()+2) ; // e.g. itag=5 or itag=22 (one or two digits) - unimportant for hasmap but looks better
           								shmkey = shmkey.matches("(.*)[^(0-9)]$")?shmkey.substring(0,shmkey.length()-1):shmkey; // delete last non-digit if any
           								if (!ssourcecodevideourls.containsKey(shmkey)) {
           									ssourcecodevideourls.put(shmkey, ssourcecodeyturls[i]); // save that URL
           									debugoutput(String.format( "video url #%d saved with key %s: %s",i,shmkey,ssourcecodevideourls.get(shmkey) ));
           									output("found video URL: ".concat(shmkey));
           								}
           							};
								} // for

            					debugoutput("ssourcecodevideourls.length: ".concat(Integer.toString(ssourcecodevideourls.size())));
            					
            					String sno = "";
         					
            					// figure out what resolution-button is pressed now
            					switch (JFCMainClient.getIdlbuttonstate()) {
            					case 4:
            						sno = "22"; // 22|35
            						this.sNextVideoURL = ssourcecodevideourls.get(szITAG.concat(sno));
            						if (this.sNextVideoURL==null) {
            							sno = "35";
            							this.sNextVideoURL = ssourcecodevideourls.get(szITAG.concat(sno));
            						}
            						break;
            					case 2:
            						sno = "18"; // 18|34
            						this.sNextVideoURL = ssourcecodevideourls.get(szITAG.concat(sno));
            						if (this.sNextVideoURL==null) {
            							sno = "34";
            							this.sNextVideoURL = ssourcecodevideourls.get(szITAG.concat(sno));
            						}
            						break;
            					case 1:
            						sno = "5"; // 5|?
        							this.sNextVideoURL = ssourcecodevideourls.get(szITAG.concat(sno));
            						break;
            					default:
            						this.sNextVideoURL = null;
            						this.sVideoURL = null;
            						this.sFilenameResPart = null;
            						break;
            					}
            					
        						try { debugoutput("choosed resolution ".concat(sno).concat(": ").concat(this.sNextVideoURL)); this.sFilenameResPart = "(itag".concat(sno).concat(")");} catch (NullPointerException npe) {}
        						
            					if (this.sNextVideoURL==null) {
            						String smsg = "could not find video url for selected resolution!";
            						output(smsg); debugoutput(smsg);
            						// TODO implement some kind of fallback - when HD is selected that download at ne next lower res video if HD is not available 
            					}
            				}
            				if (this.iRecursionCount==0 && sline.matches("(.*)<meta name=\"title\" content=(.*)")) {
            					this.setTitle( sline.replaceFirst("<meta name=\"title\" content=", "").trim().replaceAll("[!\"#$%&'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]", "") );	
            				}
            			} catch (NullPointerException npe) {
            			}
            		} // while
            	// test if we got the binary content
            	} else if (sContentType.matches("video/(.)*")) {
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
            				f = new File(sdirectorychoosed, sfilename.concat((idupcount>0?"(".concat(idupcount.toString()).concat(")"):"")).concat(".").concat(sContentType.replaceFirst("video/", "").replaceAll("x-", "")));
            				idupcount += 1;
            			} while (f.exists());
            			this.setFileName(f.getAbsolutePath());
            			
            			Long iBytesReadSum = (long) 0;
            			Long iPercentage = (long) -1;
            			Long iBytesMax = Long.parseLong(response.getFirstHeader("Content-Length").getValue());
            			fos = new FileOutputStream(f);
            			
            			debugoutput(String.format("writing %d bytes to: %s",iBytesMax,this.getFileName()));
            			output("file size of \"".concat(this.getTitle()).concat("\" = ").concat(iBytesMax.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iBytesMax/1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iBytesMax/1024/1024)).concat(" MiB")));
            		    
            			byte[] bytes = new byte[4096];
            			Integer iBytesRead = 1;
            			String sOldURL = JFCMainClient.szDLSTATE.concat(this.sURL);
            			String sNewURL = "";
            			
            			// adjust blocks of percentage to output - larger files are shown with smaller pieces
            			Integer iblocks = 10; if (iBytesMax>20*1024*1024) iblocks=4; if (iBytesMax>40*1024*1024) iblocks=2;
            			while (!this.bisinterrupted && iBytesRead>0) {
            				iBytesRead = binaryreader.read(bytes);
            				iBytesReadSum += iBytesRead;
            				// every x% of the download drop a line 
            				if ( (((iBytesReadSum*100/iBytesMax) / iblocks) * iblocks) > iPercentage ) {
            					iPercentage = (((iBytesReadSum*100/iBytesMax) / iblocks) * iblocks);
            					sNewURL = JFCMainClient.szDLSTATE.concat("(").concat(Long.toString(iPercentage).concat(" %) ").concat(this.sURL));
            					JFCMainClient.exchangeYTURLInList(sOldURL, sNewURL);
            					sOldURL = sNewURL ; 
            				}
            				try {fos.write(bytes,0,iBytesRead);} catch (IndexOutOfBoundsException ioob) {}
            				this.bisinterrupted = JFCMainClient.getbQuitrequested(); // try to get informatation about application shutdown
            			} // while
            			
            			JFCMainClient.exchangeYTURLInList(sNewURL, JFCMainClient.szDLSTATE.concat(this.sURL));
            			
            			// rename files if download was interrupted before completion of download
            			if (this.bisinterrupted && iBytesReadSum<iBytesMax) {
            				httpclient.getConnectionManager().shutdown(); // otherwise binaryreader.close() would cause the entire datastream to be transmitted 
            				debugoutput(String.format("download canceled. (%d)",(iBytesRead)));
            				changeFileNamewith("CANCELED.");
            				String smsg = "renaming unfinished file to: ".concat( this.getFileName() );
            				output(smsg); debugoutput(smsg);
            				f.renameTo(new File( this.getFileName()));
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
        					textreader.close();
        				} catch (Exception e) {
        				}
                        try {
        					binaryreader.close();
        				} catch (Exception e) {
        				}
            		} // try
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

	private void changeFileNamewith(String string) {
		String src = "";
		String[] srenfilename = this.getFileName().split(System.getProperty("file.separator"));
		
		srenfilename[srenfilename.length-1] = string.concat(srenfilename[srenfilename.length-1]); // filename will be prepended with parameter string
		
		for (String s : srenfilename) {
			src += s.concat(System.getProperty("file.separator")); 
		}
		src = src.substring(0, src.length()-1); // cut off last file.separator
		this.setFileName(src);
	} // changeFileNamewith

	private String getProxy() {
		String sproxy = JFCMainClient.sproxy;
		if (sproxy==null) return(""); else return(sproxy);
	} // getProxy() 

	private String getURI(String sURL) {
		String suri = "/".concat(sURL.replaceFirst(JFCMainClient.szYTHOSTREGEX, ""));
		return(suri);
	} // getURI

	private String getHost(String sURL) {
		String shost = sURL.replaceFirst(JFCMainClient.szYTHOSTREGEX, "");
		shost = sURL.substring(0, sURL.length()-shost.length());
		shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
		return(shost);
	} // gethost
	
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
				output("try to download: ".concat(this.sURL));
				JFCMainClient.removeURLFromList(this.sURL);
				JFCMainClient.addYTURLToList(JFCMainClient.szDLSTATE.concat(this.sURL));
				
				// download one webresource and show result
				bDOWNLOADOK = downloadone(this.sURL); this.iRecursionCount=-1;
				if (bDOWNLOADOK) 
					output("download complete: ".concat("\"").concat(this.getTitle()).concat("\"").concat(" to ").concat(this.getFileName()));
				else
					output("error downloading: ".concat(this.sURL));
				
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