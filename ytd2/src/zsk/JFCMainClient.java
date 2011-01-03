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

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * knoedel@section60:~/workspace/ytd2$ echo " *" `egrep -v "(^\s*(\/\*|\*|//)|^\s*$)" src/zsk/*java | wc -l` java code lines && echo -e " *" `egrep "(^\s*(\/\*|\*|//)|^\s*$)" src/zsk/*java | wc -l` empty/comment lines "\n *"
 * 700 java code lines
 * 307 empty/comment lines 
 * 
 * knoedel@section60:~/workspace/ytd2$ date && uname -a && cat /etc/*rele* && java -version
 * Mon Dec 27 23:45:38 CET 2010
 * Linux section60 2.6.35-23-generic #41-Ubuntu SMP Wed Nov 24 11:55:36 UTC 2010 x86_64 GNU/Linux
 * DISTRIB_ID=Ubuntu
 * DISTRIB_RELEASE=10.10
 * DISTRIB_CODENAME=maverick
 * DISTRIB_DESCRIPTION="Ubuntu 10.10"
 * java version "1.6.0_22"
 * Java(TM) SE Runtime Environment (build 1.6.0_22-b04)
 * Java HotSpot(TM) 64-Bit Server VM (build 17.1-b03, mixed mode)
 * 
 * http://www.youtube.com/watch?v=5nj77mJlzrc  					<meta name="title" content="BF109 G">																																																																																								In lovely memory of my grandpa, who used to fly around the clouds. 
 * http://www.youtube.com/watch?v=I3lq1yQo8OY&NR=1&feature=fvwp	<meta name="title" content="Showdown: Air Combat - Me-109">																																																																																			http://www.youtube.com/watch?v=yxXBhKJnRR8
 * http://www.youtube.com/watch?v=RYXd60D_kgQ&feature=related	<meta name="title" content="Me 262 Flys Again!">
 * http://www.youtube.com/watch?v=6ejc9_yR5oQ&feature=related	<meta name="title" content="Focke Wulf 190 attacks Boeing B 17 in 2009 at Hahnweide">
 *
 * technobase.fm / We Are One!
 * 
 * using Eclipse 3.6.1 64Bit Helios
 * TODOs are for Eclipse IDE - Tasks View
 * 
 * tested on GNU/Linux JRE 1.6.0_22 64bit, M$-Windows XP 64bit JRE 1.6.0_22 32&64Bit and M$-Windows 7 32Bit JRE 1.6.0_23 32Bit
 * 
 * source code compliance level is 1.5
 * java files are UTF-8 encoded
 * javac shows no warning
 * java code could be easily converted to Java 1.4.2
 */
public class JFCMainClient extends JFrame implements ActionListener, WindowListener, DocumentListener, ChangeListener, DropTargetListener {
	public static final String szVersion = "V20110103_1800 by MrKnödelmann";
	
	private static final long serialVersionUID = 6791957129816930254L;

	private static final String newline = "\n";
	
	// more or less output
	static boolean bDEBUG = true;
	
	public static final String szDLSTATE = "downloading ";
	
	// TODO downlaod via cli only? does this make sense if its all about videos?!
			 
	// something like [http://][www.]youtube.[cc|to|pl|ev|do|ma|in]/watch?v=0123456789A 
	private static final String szYTREGEX = "^((H|h)(T|t)(T|t)(P|p)://)?((W|w)(W|w)(W|w)\\.)?(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/(W|w)(A|a)(T|t)(C|c)(H|h)\\?(v|V)=.{11}"; // http://de.wikipedia.org/wiki/CcTLD
	// something like [http://][*].youtube.[cc|to|pl|ev|do|ma|in]/   the last / is for marking the end of host, it does not belong to the hostpart
	public static final String szHOSTREGEX = "^((H|h)(T|t)(T|t)(P|p)://)?(.*)\\.(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/";

	private static final String szPLAYLISTREGEX = "/view_play_list\\?p=([A-Za-z0-9]*)&playnext=[0-9]{1,2}&v=";
	
	static Thread t1;
	static Thread t2;
	static Thread t3;
	static Thread t4;
	
	static JFCMainClient frame = null;

	public static Boolean bQuitrequested = false;
	
	JPanel panel = null;
	JSplitPane middlepane = null;
	JTextArea textarea = null;
	JList urllist = null;
	JButton quitbutton = null;
	JButton directorybutton = null;
	JTextField directorytextfield = null;
	JTextField textinputfield = null;
	DefaultListModel dlm = null;
	JSlider slResolution = null;
	
	/**
	 * append text to textarea
	 * 
	 * @param Object o
	 */
	public static void addTextToConsole( Object o ) {
		try {
			// append() is threadsafe
			JFCMainClient.frame.textarea.append( o.toString().concat( newline ) );
			JFCMainClient.frame.textarea.setCaretPosition( JFCMainClient.frame.textarea.getDocument().getLength() );
			JFCMainClient.frame.textinputfield.requestFocusInWindow();
		} catch (Exception e) {
			@SuppressWarnings( "unused" ) // for debuging
			String s = e.getMessage();
		}
	} // addTexttoconsole()
	
	
	public static void addYTURLToList( String sname ) {
		String sn = sname;
		// bring all URLs into the same form
		if (sname.toLowerCase().startsWith("youtube")) sn = "http://www.".concat(sname);
		if (sname.toLowerCase().startsWith("www")) sn = "http://".concat(sname);
		synchronized (JFCMainClient.frame.dlm) {
			JFCMainClient.frame.dlm.addElement( sn );
			debugoutput("notify() ");
			frame.dlm.notify();
		}
	} // addYTURLToList

	public static void removeURLFromList( String sname ) {
		synchronized (JFCMainClient.frame.dlm) {
			try {
				int i = JFCMainClient.frame.dlm.indexOf( sname );
				JFCMainClient.frame.dlm.remove( i );
			} catch (IndexOutOfBoundsException n) {}
		}
	} // removeURLFromList
	
	public static String getfirstURLFromList( ) {
		String src = null;
//		debugoutput("getfirstURLFromList()");
		synchronized (JFCMainClient.frame.dlm) {
			try {
				int i;
				// try to find the index of an URL entry in the list without "downloading " at the beginning
				for ( i = 0; i < JFCMainClient.frame.dlm.getSize(); i++) {
					if (!((String)JFCMainClient.frame.dlm.get(i)).startsWith( JFCMainClient.szDLSTATE )) break;
				}
				src = ((String) JFCMainClient.frame.dlm.get(i)).replaceFirst( JFCMainClient.szDLSTATE, "" );
			} catch (IndexOutOfBoundsException n) {}
		}
//		debugoutput("getfirstURLFromList() src: ".concat(src.toString()));
		return src;
	} // getfirstURLFromList

	public static void clearURLList() {
		try {
			synchronized (JFCMainClient.frame.dlm) {
				JFCMainClient.frame.dlm.clear();
			}
		} catch (NullPointerException n) {}
	} // clearURLList

	public void setfocustotextfield() {
		this.textinputfield.requestFocusInWindow();
	} // setfocustotextfield()
	
	public void shutdownAppl() {
		// running downloads are difficult to terminate (Thread.isInterrupted() does not work there)
		synchronized (JFCMainClient.bQuitrequested) {
			JFCMainClient.bQuitrequested = true;	
		}
		debugoutput("bQuitrequested = true");
		
		output("terminating threads...");
		try {
			try {JFCMainClient.t1.interrupt();} catch (NullPointerException npe) {}
			try {JFCMainClient.t2.interrupt();} catch (NullPointerException npe) {}
			try {JFCMainClient.t3.interrupt();} catch (NullPointerException npe) {}
			try {JFCMainClient.t4.interrupt();} catch (NullPointerException npe) {}
			try {JFCMainClient.t1.join();} catch (NullPointerException npe) {}
			try {JFCMainClient.t2.join();} catch (NullPointerException npe) {}
			try {JFCMainClient.t3.join();} catch (NullPointerException npe) {}
			try {JFCMainClient.t4.join();} catch (NullPointerException npe) {}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		debugoutput( "quit." );
		System.exit( 0 );
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
	
	/**
	 * process events of ActionListener
	 * 
	 */
	public void actionPerformed( final ActionEvent e ) {
		if (e.getSource().equals( frame.textinputfield )) {
			if (!e.getActionCommand().equals( "" )) { 
				if (e.getActionCommand().matches(szYTREGEX))
					addYTURLToList(e.getActionCommand());
				else {
					// TODO some kind of gui-cli :) .. could be used to start/stop/test threads or output some internals at command
					addTextToConsole(e.getActionCommand());
					if (e.getActionCommand().toLowerCase().matches("^(help|-h|/\\?|\\?)")) addTextToConsole("need help? comes later!");
					else if (e.getActionCommand().toLowerCase().matches("^(version|-v)")) addTextToConsole(szVersion);
					else addTextToConsole("?");
				}
			}
			synchronized (frame.textinputfield) {
				frame.textinputfield.setText("");				
			}
			return;
		}
		
		// let the user choose another dir
		if (e.getSource().equals( frame.directorybutton )) {
			debugoutput("frame.directorybutton");
			JFileChooser fc = new JFileChooser();
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			synchronized (frame.directorytextfield) {
				// we have to set current directory here because it gets lost when fc is lost
				fc.setCurrentDirectory( new File( frame.directorytextfield.getText()) );
			}
			debugoutput("current dir: ".concat( fc.getCurrentDirectory().getAbsolutePath()) );
			if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			String snewdirectory = fc.getSelectedFile().getAbsolutePath().concat( System.getProperty("file.separator") );
			File ftest = new File(snewdirectory);
			if (ftest.exists()) {
				if (ftest.isDirectory()) {
					synchronized (frame.directorytextfield) {
						frame.directorytextfield.setText( snewdirectory );
					}
					debugoutput("new current dir: ".concat( fc.getCurrentDirectory().getAbsolutePath()) );
				} else {
					output("not a directory: ".concat(snewdirectory));
				}
			} else {
				output("directory does not exist: ".concat(snewdirectory));
			}
			return;
		}
		
		if (e.getActionCommand().equals( "quit" )) {
			this.shutdownAppl();
			return;
		}
		debugoutput("action? ".concat(e.getSource().toString()));
	} // actionPerformed()

	/**
	 * @param pane
	 */
	public void addComponentsToPane( final Container pane ) {
		this.panel = new JPanel();

		this.quitbutton = new JButton( "" ,createImageIcon("images/exit.png",""));
		this.panel.setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.anchor = GridBagConstraints.WEST;

		this.dlm = new DefaultListModel();
		this.urllist = new JList( this.dlm );
		// TODO maybe we add a button to remove added URLs from list?
//		this.userlist.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.urllist.setFocusable( false );
		this.textarea = new JTextArea( 2, 2 );
		this.textarea.setEditable( true );
		this.textarea.setFocusable( false );

		JScrollPane leftscrollpane = new JScrollPane( this.urllist );
		JScrollPane rightscrollpane = new JScrollPane( this.textarea );
		this.middlepane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, leftscrollpane, rightscrollpane );
		this.middlepane.setOneTouchExpandable( true );
		this.middlepane.setDividerLocation( 200 );

		Dimension minimumSize = new Dimension( 50, 50 );
		leftscrollpane.setMinimumSize( minimumSize );
		rightscrollpane.setMinimumSize( minimumSize );
		
		this.directorybutton = new JButton("", createImageIcon("images/open.png",""));
		gbc.gridx = 0;
		gbc.gridy = 0;
		this.directorybutton.addActionListener( this );
		this.panel.add( this.directorybutton, gbc );
		
		String sfilesep = System.getProperty("file.separator");

		// TODO check if initial download directory exists
		// assume that at least the users homedir exists
		String shomedir = System.getProperty("user.home").concat(sfilesep)/*.concat("YouTube Downloads")*/.concat(sfilesep);
		if (System.getProperty("user.home").equals("/home/knoedel")) shomedir = "/home/knoedel/YouTube Downloads/";
		if (sfilesep.equals("\\")) sfilesep += sfilesep; // on m$-windows we need to escape the \
		// TODO on m$-windows we need to surround the directory with " " othewise the user cannot create a directory within the dialog if the last directoryname contains a <space>
		shomedir = shomedir.replaceAll(sfilesep.concat(sfilesep), sfilesep) ;
//		debugoutput("file.separator: ".concat(System.getProperty("file.separator")).concat("  sfilesep: ".concat(sfilesep)));
//		debugoutput("user.home: ".concat(System.getProperty("user.home")).concat("  shomedir: ".concat(shomedir)));

		debugoutput("os.name: ".concat(System.getProperty("os.name")));
		debugoutput("os.arch: ".concat(System.getProperty("os.arch")));
		debugoutput("os.version: ".concat(System.getProperty("os.version")));
		debugoutput("Locale.getDefault: ".concat(Locale.getDefault().toString()));
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.directorytextfield = new JTextField( shomedir, 80 );
		// TODO if the user enters a directory rather than choosing one, the directory may not be exist -> ask user what to do 
		this.directorytextfield.setEnabled( false ); // dont let the user enter something for now -> changing that later - see this.textinputfield.getDocument().addDocumentListener(this);
		this.directorytextfield.setFocusable( true );
		this.directorytextfield.addActionListener( this );
		this.panel.add( this.directorytextfield, gbc);
		
		JLabel dirhint = new JLabel( "download into folder:");
		gbc.gridx = 0;
		gbc.gridy = 1;
		this.panel.add( dirhint, gbc);
		
		this.middlepane.setPreferredSize( new Dimension( 900, 200 ) ); // looks OK on a 23" Samsung LCD at 2048x1152 ;)
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 2;
		gbc.weightx = 2;
		gbc.gridwidth = 2;
		this.panel.add( this.middlepane, gbc );

		final int RES_MIN = 240;
		final int RES_MAX = 720;
		final int RES_INIT = 480; 
		this.slResolution = new JSlider(SwingConstants.HORIZONTAL, RES_MIN, RES_MAX, RES_INIT);
		this.slResolution.addChangeListener(this);

		this.slResolution.setMajorTickSpacing(120);
		this.slResolution.setMinorTickSpacing(120);
		this.slResolution.setPaintTicks(false);
		this.slResolution.setPaintLabels(true);
		this.slResolution.setSnapToTicks(true);
		this.slResolution.setPaintTrack(false);
		this.slResolution.setPreferredSize(new Dimension(40, 40));
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		
		//this.panel.add( this.slResolution, gbc );

		JLabel hint = new JLabel( "(type in, paste in, drop in -> yt-webaddresses or yt-videoimages) URLs:");
		gbc.fill = 0;
		gbc.gridwidth = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.WEST;
		this.panel.add( hint, gbc );
		this.textinputfield = new JTextField( 30 );
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		this.textinputfield.setEnabled( true );
		this.textinputfield.setFocusable( true );
		this.textinputfield.addActionListener( this );
		this.textinputfield.getDocument().addDocumentListener(this);
		this.panel.add( this.textinputfield, gbc );
		gbc.gridx = 2;
		gbc.gridy = 5;
		gbc.gridwidth = 0;
		this.quitbutton.addActionListener( this );
		this.quitbutton.setActionCommand( "quit" );
		this.quitbutton.setToolTipText( "Exit." );

		this.panel.add( this.quitbutton, gbc );

		pane.add( this.panel );
		addWindowListener( this );
		
		JFCMainClient.frame.setDropTarget(new DropTarget(this, this));
		JFCMainClient.frame.textarea.setTransferHandler(null); // otherwise the droped text would be inserted

	} // addComponentsToPane()

	public JFCMainClient( String name ) {
		super( name );
	}

	public JFCMainClient() {
		super();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	static void createAndShowGUI() {
		setDefaultLookAndFeelDecorated(false);
		frame = new JFCMainClient( "YTD2 ".concat(szVersion).concat(" ").concat("https://sourceforge.net/projects/ytd2/") );
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		frame.addComponentsToPane( frame.getContentPane() );
		frame.pack();
		frame.setVisible( true );
		
		String sv = "version: ".concat( szVersion ).concat(bDEBUG?" DEBUG ":""); 
		output(sv); debugoutput(sv);
		output(""); // \n

		// TODO ensure threads are running even if one ends with an Exception

		String sproxy = System.getenv("http_proxy");
		if (sproxy==null) sproxy="";
		sv = "env var http_proxy: ".concat(sproxy);
		output(sv); debugoutput(sv);

		// lets honor the upload limit of google (youtube)
		// downloading is faster than viewing anyway so dont start more than four threads please!!!
		t1 = new Thread( new YTDownloadThread(bDEBUG) );
		t1.start();
		t2 = new Thread( new YTDownloadThread(bDEBUG) );
		t2.start();
		t3 = new Thread( new YTDownloadThread(bDEBUG) );
		t3.start();
		t4 = new Thread( new YTDownloadThread(bDEBUG) );
		t4.start();

	} // createAndShowGUI()

	
	public void windowActivated( WindowEvent e ) {
			setfocustotextfield();
	} // windowActivated()

	public void windowClosed( WindowEvent e ) {
	}

	/**
	 * quit==exit
	 * 
	 */
	public void windowClosing( WindowEvent e ) {
		this.shutdownAppl();
	} // windowClosing()

	public void windowDeactivated( WindowEvent e ) {
	}

	public void windowDeiconified( WindowEvent e ) {
	}

	public void windowIconified( WindowEvent e ) {
	}

	public void windowOpened( WindowEvent e ) {
	}
	
	public void processComponentEvent(ComponentEvent e) {
		switch (e.getID()) {
		case ComponentEvent.COMPONENT_MOVED:
			break;
		case ComponentEvent.COMPONENT_RESIZED:
			JFCMainClient.frame.middlepane.setDividerLocation(JFCMainClient.frame.middlepane.getWidth() / 3);
			break;
		case ComponentEvent.COMPONENT_HIDDEN:
			break;
		case ComponentEvent.COMPONENT_SHOWN:
			break;
		}
	}
	
	/**
	 * main entry point
	 * 
	 * @param args
	 */
	public static void main( String[] args ) {
		try {
			UIManager.setLookAndFeel( "javax.swing.plaf.metal.MetalLookAndFeel" );
		} catch (UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		try {
			javax.swing.SwingUtilities.invokeAndWait( new Runnable() {
				public void run() {
					JFCMainClient.createAndShowGUI();
				}
			} );
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

//		javax.swing.SwingUtilities.invokeLater( new Runnable() {
//			public void run() {
//				checkClientSocketStatus();
//			}
//		} );
		
	} // main()
	
	static void debugoutput (String s) {
		if (!JFCMainClient.bDEBUG)
			return;

		JFCMainClient.addTextToConsole("#DEBUG ".concat(s));
		System.out.println("#DEBUG ".concat(s));
	} // debugoutput
	
	static void output (String s) {
		if (JFCMainClient.bDEBUG)
			return;
		JFCMainClient.addTextToConsole("#info - ".concat(s));
	} // output


	public void changedUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	}


	public void insertUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	} 

	public void removeUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	}
	
//	private String getHost(String sURL) {
//		String shost = sURL.replaceFirst(JFCMainClient.szHOSTREGEX, "");
//		shost = sURL.substring(0, sURL.length()-shost.length());
//		shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/", "");
//		return(shost);
//	} // gethost
	
	/**
	 * check if a youtube-URL was pasted or typed in
	 * if yes cut it out and send it to the URLList to get processed by one of the threads
	 * 
	 * the user can paste a long string containing many youtube-URLs .. but here is work to do because we have to erase the string(s) that remain(s)
	 */
	void checkInputFieldforYTURLs() {
		String sinput = frame.textinputfield.getText(); // dont call .toLowerCase() !

		// TODO this can probably be done better - replace input so URLs get extracted without user activity (works even if URLs are spread across multiple lines)
		sinput = sinput.replaceAll("&feature=fvwp&", "&"); // after that text could be another yt-URL or more query_string options
		sinput = sinput.replaceAll("&feature=fvwphttp", "http");
		sinput = sinput.replaceAll("&feature=fvwp", "");
		sinput = sinput.replaceAll("&feature=relatedhttp", "http"); // if somebody writes a regex for notanytURL as replacement for fvwp|related ... ;)
		sinput = sinput.replaceAll("&feature=related&", "&");
		sinput = sinput.replaceAll("&feature=related", "");
		sinput = sinput.replaceAll("&feature=mfu_in_order&list=[A-Z]{1,2}", "");
		sinput = sinput.replaceAll("&feature=[A-Z]{1,2}&list=([A-Za-z0-9]*)&index=[0-9]", "");
		sinput = sinput.replaceAll("&NR=[0-9]&", "&");
		sinput = sinput.replaceAll("&NR=[0-9]http", "http");
		sinput = sinput.replaceAll("&NR=[0-9]", "");
		sinput = sinput.replaceAll(" ", "");
		sinput = sinput.replaceAll(szPLAYLISTREGEX, "/watch?v=");

		String surl = sinput.replaceFirst(szYTREGEX, "");
		
		if (sinput.equals(surl)) return;

		debugoutput("sinput: ".concat(sinput).concat(" surl: ".concat(surl)));
		
		// starting at index 0 because szYTREGEX should start with ^ // if szYTREGEX does not start with ^ then you have to find the index where the match is before you can cut out the URL 
		surl = sinput.substring(0, sinput.length()-surl.length());
		addYTURLToList(surl);
		sinput = sinput.substring(surl.length());
		debugoutput(String.format("sinput: %s surl: %s",sinput,surl));
		//frame.textinputfield.setText(sinput); // generates a java.lang.IllegalStateException: Attempt to mutate in notification
		
		final String fs = sinput;

		// let a thread update the textfield in the UI
		Thread worker = new Thread() {
            public void run() {
            	synchronized (JFCMainClient.frame.textinputfield) {
            		JFCMainClient.frame.textinputfield.setText(fs);
				}
            }
        };
        SwingUtilities.invokeLater (worker);
	} // checkInputFieldforYTURLS
	
	protected ImageIcon createImageIcon(String path, String description) {
	    java.net.URL imgURL = getClass().getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	} // createImageIcon

	public void stateChanged(ChangeEvent e) {
	}


	public void dragEnter(DropTargetDragEvent dtde) {
	}


	public void dragOver(DropTargetDragEvent dtde) {
	}


	public void dropActionChanged(DropTargetDragEvent dtde) {
	}


	public void dragExit(DropTargetEvent dte) {
	}


	public void drop(DropTargetDropEvent dtde) {
		try {
			Transferable tr = dtde.getTransferable();
			DataFlavor[] flavors = tr.getTransferDataFlavors();
			DataFlavor fl = null;
			String str = "";
			for (int i = 0; i < flavors.length; i++) {
				fl = flavors[i];
				if (fl.isFlavorTextType() || fl.isMimeTypeEqual("text/html") || fl.isMimeTypeEqual("application/x-java-url") ) {
					dtde.acceptDrop (dtde.getDropAction());
					if (tr.getTransferData(fl) instanceof InputStreamReader) {
						BufferedReader textreader = new BufferedReader( (Reader) tr.getTransferData(fl) );
						String sline = "";
						try {
							while (sline != null) {
								sline = textreader.readLine();
								if (sline != null) str += sline;
							}
						} catch (Exception e) {
						} finally {
							textreader.close();
						}
						str = str.replaceAll("<[A-Za-z/!\\-]*>", ""); // remove HTML tags - ignore HTML characters like &szlig; (which are no tags)
					} else {
						str = tr.getTransferData(fl).toString();
					}
					dtde.dropComplete(true);
					debugoutput("drop event text: ".concat(str).concat(" (").concat(fl.getMimeType()).concat(") ")) ;
					// append text to textfield - same as user drops text into this field
					// except special characaters -> from http://de.wikipedia.org/wiki/GNU-Projekt („GNU is not Unix“)(&bdquo;GNU is not Unix&ldquo;)
					// two drops from same source .. one time in textfield and elsewhere - maybe we change that later?!
					synchronized (JFCMainClient.frame.textinputfield) {
						JFCMainClient.frame.textinputfield.setText(JFCMainClient.frame.textinputfield.getText().concat(str));
					}
					return;
				} else 
					debugoutput("drop event unknown type: ".concat( fl.getHumanPresentableName()));
			} // for
		} catch (Throwable t) {
		}
	} // drop()
	
} // class JFCMainClient