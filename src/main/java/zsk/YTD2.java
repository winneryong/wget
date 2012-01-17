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
import java.awt.datatransfer.UnsupportedFlavorException;
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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
 * knoedel@section60:~/workspace/ytd2$ echo " *" `egrep -v
 * "(^\s*(\/\*|\*|//)|^\s
 * *$)" src/zsk/*java | wc -l` java code lines && echo -e "
 * *" `egrep "(^\s*(\/\*
 * |\*|//)|^\s*$)" src/zsk/*java | wc -l` empty/comment lines "\n *" 1077 java
 * code lines 432 empty/comment lines
 * 
 * knoedel@section60:~/workspace/ytd2$ date && uname -a && cat /etc/*rele* &&
 * java -version Mon Aug 29 22:39:47 CEST 2011 Linux section60 2.6.38-11-generic
 * #48-Ubuntu SMP Fri Jul 29 19:02:55 UTC 2011 x86_64 x86_64 x86_64 GNU/Linux
 * DISTRIB_ID=Ubuntu DISTRIB_RELEASE=11.04 DISTRIB_CODENAME=natty
 * DISTRIB_DESCRIPTION="Ubuntu 11.04" java version "1.6.0_26" Java(TM) SE
 * Runtime Environment (build 1.6.0_26-b03) Java HotSpot(TM) 64-Bit Server VM
 * (build 20.1-b02, mixed mode)
 * 
 * 
 * http://www.youtube.com/watch?v=5nj77mJlzrc <meta name="title"
 * content="BF109 G"> In lovely memory of my grandpa, who used to fly around the
 * clouds. http://www.youtube.com/watch?v=I3lq1yQo8OY&NR=1&feature=fvwp <meta
 * name="title" content="Showdown: Air Combat - Me-109">
 * http://www.youtube.com/watch?v=yxXBhKJnRR8
 * http://www.youtube.com/watch?v=RYXd60D_kgQ&feature=related <meta name="title"
 * content="Me 262 Flys Again!">
 * http://www.youtube.com/watch?v=6ejc9_yR5oQ&feature=related <meta name="title"
 * content="Focke Wulf 190 attacks Boeing B 17 in 2009 at Hahnweide">
 * 
 * technobase.fm / We Are One!
 * 
 * using Eclipse 3.5/3.6/3.7 TODOs are for Eclipse IDE - Tasks View
 * 
 * tested on GNU/Linux JRE 1.6.0_24 64bit, M$-Windows XP 64bit JRE 1.6.0_22
 * 32&64Bit and M$-Windows 7 32Bit JRE 1.6.0_23 32Bit using Mozilla Firefox
 * 3.6-6 and M$-IE (8)
 * 
 * source code compliance level is 1.5 java files are UTF-8 encoded javac shows
 * no warning java code could be easily converted to Java 1.4.2
 */
public class YTD2 extends JFrame implements ActionListener, WindowListener, DocumentListener, ChangeListener,
        DropTargetListener {
    public static final String szVersion = "V20110922_2202 by MrKnödelmann";

    private static final long serialVersionUID = 6791957129816930254L;

    private static final String newline = "\n";

    // more or less (internal) output
    static boolean bDEBUG = false;

    // just report file size of HTTP header - don't download binary data (the
    // video)
    static boolean bNODOWNLOAD = false;

    public static String sproxy = null;

    public static String szDLSTATE = "downloading ";

    // TODO download with cli only? does this make sense if its all about
    // videos?!

    // something like
    // [http://][www.]youtube.[cc|to|pl|ev|do|ma|in]/watch?v=0123456789A
    public static final String szYTREGEX = "^((H|h)(T|t)(T|t)(P|p)://)?((W|w)(W|w)(W|w)\\.)?(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/(W|w)(A|a)(T|t)(C|c)(H|h)\\?(v|V)=[^&]{11}"; // http://de.wikipedia.org/wiki/CcTLD
    // something like [http://][*].youtube.[cc|to|pl|ev|do|ma|in]/ the last / is
    // for marking the end of host, it does not belong to the hostpart
    public static final String szYTHOSTREGEX = "^((H|h)(T|t)(T|t)(P|p)://)?(.*)\\.(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/";

    // RFC-1123 ? hostname [with protocol]
    // public static final String szPROXYREGEX =
    // "^((H|h)(T|t)(T|t)(P|p)://)?([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*$";
    public static final String szPROXYREGEX = "(^((H|h)(T|t)(T|t)(P|p)://)?([a-zA-Z0-9]+:[a-zA-Z0-9]+@)?([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*(:[0-90-90-90-9]{1,4})?$)|()";

    private static final String szPLAYLISTREGEX = "/view_play_list\\?p=([A-Za-z0-9]*)&playnext=[0-9]{1,2}&v=";

    // all characters that do not belong to an HTTP URL - could be written
    // shorter?? (where did I used this?? dont now anymore)
    final String snotsourcecodeurl = "[^(a-z)^(A-Z)^(0-9)^%^&^=^\\.^:^/^\\?^_^-]";

    static YTD2 frame = null;

    private static Boolean bQuitrequested = false;

    static YTDownloadThread t1;
    static YTDownloadThread t2;
    static YTDownloadThread t3;
    static YTDownloadThread t4;

    JPanel panel = null;
    JSplitPane middlepane = null;
    JTextArea textarea = null;
    JList urllist = null;
    JButton quitbutton = null;
    JButton directorybutton = null;
    JTextField directorytextfield = null;
    JTextField textinputfield = null;
    DefaultListModel dlm = null;
    JRadioButton hdbutton = null;
    JRadioButton stdbutton = null;
    JRadioButton ldbutton = null;

    public static synchronized Boolean getbQuitrequested() {
        return bQuitrequested;
    }

    public static synchronized void setbQuitrequested(Boolean bQuitrequested) {
        YTD2.bQuitrequested = bQuitrequested;
    }

    public static synchronized int getIdlbuttonstate() {
        return (YTD2.frame.hdbutton.isSelected() ? 4 : 0)
                + (YTD2.frame.stdbutton.isSelected() ? 2 : 0)
                + (YTD2.frame.ldbutton.isSelected() ? 1 : 0);
    }

    /**
     * append text to textarea
     * 
     * @param Object
     *            o
     */
    public static void addTextToConsole(Object o) {
        try {
            // append() is threadsafe
            YTD2.frame.textarea.append(o.toString().concat(newline));
            YTD2.frame.textarea.setCaretPosition(YTD2.frame.textarea.getDocument().getLength());
            YTD2.frame.textinputfield.requestFocusInWindow();
        } catch (Exception e) {
            @SuppressWarnings("unused")
            // for debuging
            String s = e.getMessage();
        }
    } // addTexttoconsole()

    public static void addYTURLToList(String sname) {
        String sn = sname;
        // bring all URLs into the same form
        if (sname.toLowerCase().startsWith("youtube"))
            sn = "http://www.".concat(sname);
        if (sname.toLowerCase().startsWith("www"))
            sn = "http://".concat(sname);
        synchronized (YTD2.frame.dlm) {
            YTD2.frame.dlm.addElement(sn);
            debugoutput("notify() ");
            frame.dlm.notify();
        }
    } // addYTURLToList

    public static void exchangeYTURLInList(String sfromname, String stoname) {
        synchronized (YTD2.frame.dlm) {
            try {
                int i = YTD2.frame.dlm.indexOf(sfromname);
                YTD2.frame.dlm.setElementAt(stoname, i);
            } catch (IndexOutOfBoundsException ioobe) {
            }
        }
    } // exchangeYTURLInList

    public static void removeURLFromList(String sname) {
        synchronized (YTD2.frame.dlm) {
            try {
                int i = YTD2.frame.dlm.indexOf(sname);
                YTD2.frame.dlm.remove(i);
            } catch (IndexOutOfBoundsException ioobe) {
            }
        }
    } // removeURLFromList

    public static String getfirstURLFromList() {
        String src = null;
        synchronized (YTD2.frame.dlm) {
            try {
                int i;
                // try to find the index of an URL entry in the list without
                // "downloading " at the beginning
                for (i = 0; i < YTD2.frame.dlm.getSize(); i++) {
                    if (!((String) YTD2.frame.dlm.get(i)).startsWith(YTD2.szDLSTATE))
                        break;
                }
                src = ((String) YTD2.frame.dlm.get(i)).replaceFirst(YTD2.szDLSTATE, "");
            } catch (IndexOutOfBoundsException ioobe) {
            }
        }
        return src;
    } // getfirstURLFromList

    public static void clearURLList() {
        try {
            synchronized (YTD2.frame.dlm) {
                YTD2.frame.dlm.clear();
            }
        } catch (NullPointerException npe) {
        }
    } // clearURLList

    public static boolean isgerman() {
        return Locale.getDefault().toString().startsWith("de_")
                || (YTD2.bDEBUG && System.getProperty("user.home").equals("/home/knoedel"));
    } // isgerman

    public void setfocustotextfield() {
        this.textinputfield.requestFocusInWindow();
    } // setfocustotextfield()

    public void shutdownAppl() {
        // running downloads are difficult to terminate (Thread.isInterrupted()
        // does not work there)
        synchronized (YTD2.bQuitrequested) {
            YTD2.bQuitrequested = true;
        }
        debugoutput("bQuitrequested = true");

        output("terminating threads...");
        try {
            try {
                YTD2.t1.interrupt();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t2.interrupt();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t3.interrupt();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t4.interrupt();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t1.join();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t2.join();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t3.join();
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t4.join();
            } catch (NullPointerException npe) {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        debugoutput("quit.");
        System.exit(0);
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
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(frame.textinputfield)) {
            if (!e.getActionCommand().equals("")) {
                if (e.getActionCommand().matches(szYTREGEX))
                    addYTURLToList(e.getActionCommand());
                else {
                    addTextToConsole(e.getActionCommand());
                    cli(e.getActionCommand().toLowerCase());
                }
            }
            synchronized (frame.textinputfield) {
                frame.textinputfield.setText("");
            }
            return;
        }

        // let the user choose another dir
        if (e.getSource().equals(frame.directorybutton)) {
            debugoutput("frame.directorybutton");
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            synchronized (frame.directorytextfield) {
                // we have to set current directory here because it gets lost
                // when fc is lost
                fc.setCurrentDirectory(new File(frame.directorytextfield.getText()));
            }
            debugoutput("current dir: ".concat(fc.getCurrentDirectory().getAbsolutePath()));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            String snewdirectory = fc.getSelectedFile().getAbsolutePath().concat(System.getProperty("file.separator"));
            File ftest = new File(snewdirectory);
            if (ftest.exists()) {
                if (ftest.isDirectory()) {
                    synchronized (frame.directorytextfield) {
                        frame.directorytextfield.setText(snewdirectory);
                    }
                    debugoutput("new current dir: ".concat(fc.getCurrentDirectory().getAbsolutePath()));
                } else {
                    output("not a directory: ".concat(snewdirectory));
                }
            } else {
                output("directory does not exist: ".concat(snewdirectory));
            }
            return;
        }

        // let the user choose another download resolution
        if (e.getActionCommand().equals(this.hdbutton.getActionCommand())
                || e.getActionCommand().equals(this.stdbutton.getActionCommand())
                || e.getActionCommand().equals(this.ldbutton.getActionCommand())) {
            debugoutput("trying: ".concat(e.getActionCommand()));
            return;
        }

        if (e.getActionCommand().equals("quit")) {
            this.shutdownAppl();
            return;
        }
        debugoutput("action? ".concat(e.getSource().toString()));
    } // actionPerformed()

    void cli(String scmd) {
        if (scmd.matches("^(hilfe|help|[-/][h|\\?])")) {
            addTextToConsole("debug[ on| off]\t: more or less (internal) output");
            addTextToConsole("help|-h|/?]\t\t: show this text");
            addTextToConsole("ndl[ on| off]\t\t: no download, just report file size");
            addTextToConsole("quit|exit\t\t: shutdown application");
            addTextToConsole("proxy[ URL]\t\t: get or set proxy variable - [http://]proxyhost:proxyport");
            addTextToConsole("version|-v|\t\t: show version");
        } else if (scmd.matches("^(-?v(ersion)?)"))
            addTextToConsole(szVersion);
        else if (scmd.matches("^(debug)( on| off| true| false)?")) {
            if (scmd.matches(".*(on|true)$"))
                YTD2.bDEBUG = true;
            else if (scmd.matches(".*(off|false)$"))
                YTD2.bDEBUG = false;

            addTextToConsole("debug: ".concat(Boolean.toString(YTD2.bDEBUG)));

            // TODO bDEBUG should not be static - it belongs to this object
            // (GUI) not the threads
            try {
                YTD2.t1.setbDEBUG(YTD2.bDEBUG);
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t2.setbDEBUG(YTD2.bDEBUG);
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t3.setbDEBUG(YTD2.bDEBUG);
            } catch (NullPointerException npe) {
            }
            try {
                YTD2.t4.setbDEBUG(YTD2.bDEBUG);
            } catch (NullPointerException npe) {
            }
        } else if (scmd.matches("^(ndl)( on| off| true| false)?")) { // or "dl"
                                                                     // on|off ?
            if (scmd.matches(".*(on|true)$"))
                setbNODOWNLOAD(true);
            else if (scmd.matches(".*(off|false)$"))
                setbNODOWNLOAD(false);

            addTextToConsole("ndl: ".concat(Boolean.toString(YTD2.getbNODOWNLOAD())));
        } else if (scmd.matches("^(quit|exit)"))
            this.shutdownAppl();
        else if (scmd.matches("^(proxy)( .*)?")) {
            if (!scmd.matches("^(proxy)$")) {
                // replace "" and '' with <nothing> otherwise it's interpreted
                // as host - perhaps some users don't know how to input
                // "proxy[ URL]" with an empty URL ;-)
                String snewproxy = scmd.replaceAll("\"", "").replaceAll("'", "").replaceFirst("proxy ", "");
                debugoutput("snewproxy: ".concat(snewproxy));
                if (snewproxy.matches(YTD2.szPROXYREGEX))
                    YTD2.sproxy = snewproxy;
                else
                    addTextToConsole(isgerman() ? "Proxy Zeichenkette entspricht nicht der Spezifikation!"
                            : "proxy string does not match hostname specification!");
            }
            addTextToConsole("proxy: ".concat(YTD2.sproxy));
        } else
            addTextToConsole(isgerman() ? "? (versuche hilfe|help|-h|/?)" : "? (try help|-h|/?)");

    } // cli()

    static synchronized void setbNODOWNLOAD(boolean bNODOWNLOAD) {
        YTD2.bNODOWNLOAD = bNODOWNLOAD;
    } // setbNODOWNLOAD

    static synchronized boolean getbNODOWNLOAD() {
        return (YTD2.bNODOWNLOAD);
    } // getbNODOWNLOAD

    /**
     * @param pane
     */
    public void addComponentsToPane(final Container pane) {
        this.panel = new JPanel();

        this.panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        this.dlm = new DefaultListModel();
        this.urllist = new JList(this.dlm);
        // TODO maybe we add a button to remove added URLs from list?
        // this.userlist.setSelectionMode(
        // ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        this.urllist.setFocusable(false);
        this.textarea = new JTextArea(2, 2);
        this.textarea.setEditable(true);
        this.textarea.setFocusable(false);

        JScrollPane leftscrollpane = new JScrollPane(this.urllist);
        JScrollPane rightscrollpane = new JScrollPane(this.textarea);
        this.middlepane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftscrollpane, rightscrollpane);
        this.middlepane.setOneTouchExpandable(true);
        this.middlepane.setDividerLocation(200);

        Dimension minimumSize = new Dimension(50, 50);
        leftscrollpane.setMinimumSize(minimumSize);
        rightscrollpane.setMinimumSize(minimumSize);

        this.directorybutton = new JButton("", createImageIcon("images/open.png", ""));
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.directorybutton.addActionListener(this);
        this.panel.add(this.directorybutton, gbc);

        String sfilesep = System.getProperty("file.separator");

        // TODO check if initial download directory exists
        // assume that at least the users homedir exists
        String shomedir = System.getProperty("user.home").concat(sfilesep)/*
                                                                           * .concat
                                                                           * (
                                                                           * "YouTube Downloads"
                                                                           * )
                                                                           */.concat(sfilesep);
        if (System.getProperty("user.home").equals("/home/knoedel"))
            shomedir = "/home/knoedel/YouTube Downloads/";
        if (sfilesep.equals("\\"))
            sfilesep += sfilesep; // on m$-windows we need to escape the \
        shomedir = shomedir.replaceAll(sfilesep.concat(sfilesep), sfilesep);
        debugoutput("file.separator: ".concat(System.getProperty("file.separator")).concat(
                "  sfilesep: ".concat(sfilesep)));
        debugoutput("user.home: ".concat(System.getProperty("user.home")).concat("  shomedir: ".concat(shomedir)));

        debugoutput("os.name: ".concat(System.getProperty("os.name")));
        debugoutput("os.arch: ".concat(System.getProperty("os.arch")));
        debugoutput("os.version: ".concat(System.getProperty("os.version")));
        debugoutput("Locale.getDefault: ".concat(Locale.getDefault().toString()));

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.directorytextfield = new JTextField(shomedir, 80);
        // TODO if the user enters a directory rather than choosing one, the
        // directory may not exist -> ask user what to do
        this.directorytextfield.setEnabled(false); // dont let the user enter
                                                   // something for now ->
                                                   // changing that later - see
                                                   // this.textinputfield.getDocument().addDocumentListener(this);
        this.directorytextfield.setFocusable(true);
        this.directorytextfield.addActionListener(this);
        this.panel.add(this.directorytextfield, gbc);

        JLabel dirhint = new JLabel(isgerman() ? "Speichern im Ordner:" : "Download to folder:");

        gbc.gridx = 0;
        gbc.gridy = 1;
        this.panel.add(dirhint, gbc);

        this.middlepane.setPreferredSize(new Dimension(900, 200)); // looks OK
                                                                   // on a 23"
                                                                   // Samsung
                                                                   // LCD at
                                                                   // 2048x1152
                                                                   // ;)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 2;
        gbc.weightx = 2;
        gbc.gridwidth = 2;
        this.panel.add(this.middlepane, gbc);

        this.hdbutton = new JRadioButton("HD");
        this.hdbutton.setActionCommand("hd");
        this.hdbutton.addActionListener(this);
        this.hdbutton.setToolTipText("1080p/720p");
        this.stdbutton = new JRadioButton("Std");
        this.stdbutton.setActionCommand("std");
        this.stdbutton.addActionListener(this);
        this.stdbutton.setToolTipText("480p/360p");
        this.ldbutton = new JRadioButton("LD");
        this.ldbutton.setActionCommand("ld");
        this.ldbutton.addActionListener(this);
        this.ldbutton.setToolTipText("240p");

        this.stdbutton.setSelected(true);

        ButtonGroup bgroup = new ButtonGroup();
        bgroup.add(this.hdbutton);
        bgroup.add(this.stdbutton);
        bgroup.add(this.ldbutton);

        this.hdbutton.setEnabled(true);
        this.ldbutton.setEnabled(true);

        JPanel radiopanel = new JPanel(new GridLayout(1, 0));
        radiopanel.add(this.hdbutton);
        radiopanel.add(this.stdbutton);
        radiopanel.add(this.ldbutton);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 0;
        gbc.gridwidth = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        this.panel.add(radiopanel, gbc);

        JLabel hint = new JLabel(
                isgerman() ? "eingeben, reinkopieren, reinziehen von YT-Webadressen oder YT-Videobilder:"
                        : "Type, paste or drag'n drop a YouTube video address:");

        gbc.fill = 0;
        gbc.gridwidth = 0;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        this.panel.add(hint, gbc);

        this.textinputfield = new JTextField(30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        this.textinputfield.setEnabled(true);
        this.textinputfield.setFocusable(true);
        this.textinputfield.addActionListener(this);
        this.textinputfield.getDocument().addDocumentListener(this);
        this.panel.add(this.textinputfield, gbc);

        this.quitbutton = new JButton("", createImageIcon("images/exit.png", ""));
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 0;
        this.quitbutton.addActionListener(this);
        this.quitbutton.setActionCommand("quit");
        this.quitbutton.setToolTipText("Exit.");

        this.panel.add(this.quitbutton, gbc);

        pane.add(this.panel);
        addWindowListener(this);

        YTD2.frame.setDropTarget(new DropTarget(this, this));
        YTD2.frame.textarea.setTransferHandler(null); // otherwise the
                                                               // dropped text
                                                               // would be
                                                               // inserted

    } // addComponentsToPane()

    public YTD2(String name) {
        super(name);
    }

    public YTD2() {
        super();
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event dispatch thread.
     */
    static void createAndShowGUI() {
        setDefaultLookAndFeelDecorated(false);
        String sv = "YTD2 ".concat(szVersion).concat(" ").concat("http://sourceforge.net/projects/ytd2/");
        sv = isgerman() ? sv.replaceFirst("by", "von") : sv;
        frame = new YTD2(sv);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addComponentsToPane(frame.getContentPane());
        frame.pack();
        frame.setVisible(true);

        YTD2.szDLSTATE = isgerman() ? "heruntergeladen " : YTD2.szDLSTATE;

        sv = "version: ".concat(szVersion).concat(bDEBUG ? " DEBUG " : "");
        sv = isgerman() ? sv.replaceFirst("by", "von") : sv;
        output(sv);
        debugoutput(sv);
        output(""); // \n

        // TODO ensure threads are running even if one ends with an (unhandled)
        // Exception

        YTD2.sproxy = System.getenv("http_proxy");
        if (YTD2.sproxy == null)
            sproxy = "";
        sv = "env var http_proxy: ".concat(sproxy);
        output(sv);
        debugoutput(sv);

        // lets respect the upload limit of google (youtube)
        // downloading is faster than viewing anyway so don't start more than
        // four threads and don't play around with the URL-strings please!!!
        t1 = new YTDownloadThread(bDEBUG);
        t1.start();
        t2 = new YTDownloadThread(bDEBUG);
        t2.start();
        t3 = new YTDownloadThread(bDEBUG);
        t3.start();
        t4 = new YTDownloadThread(bDEBUG);
        t4.start();

        output(""); // \n
        output(isgerman() ? "besuche sf.net/projects/ytd2/forums für irgendwelche Tipps, Vorschläge, Neuerungen, Fragen!"
                : "Visit sf.net/projects/ytd2/forums for tips, questions and comments!");

    } // createAndShowGUI()

    public void windowActivated(WindowEvent e) {
        setfocustotextfield();
    } // windowActivated()

    public void windowClosed(WindowEvent e) {
    }

    /**
     * quit==exit
     * 
     */
    public void windowClosing(WindowEvent e) {
        this.shutdownAppl();
    } // windowClosing()

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void processComponentEvent(ComponentEvent e) {
        switch (e.getID()) {
        case ComponentEvent.COMPONENT_MOVED:
            break;
        case ComponentEvent.COMPONENT_RESIZED:
            YTD2.frame.middlepane.setDividerLocation(YTD2.frame.middlepane.getWidth() / 3);
            break;
        case ComponentEvent.COMPONENT_HIDDEN:
            break;
        case ComponentEvent.COMPONENT_SHOWN:
            break;
        }
    } // processComponentEvent

    /**
     * main entry point
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
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
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    YTD2.createAndShowGUI();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    } // main()

    static void debugoutput(String s) {
        if (!YTD2.bDEBUG)
            return;

        YTD2.addTextToConsole("#DEBUG ".concat(s));
        System.out.println("#DEBUG ".concat(s));
    } // debugoutput

    static void output(String s) {
        if (YTD2.bDEBUG)
            return;
        YTD2.addTextToConsole("#info - ".concat(s));
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

    // private String getHost(String sURL) {
    // String shost = sURL.replaceFirst(JFCMainClient.szHOSTREGEX, "");
    // shost = sURL.substring(0, sURL.length()-shost.length());
    // shost = shost.toLowerCase().replaceFirst("http://", "").replaceAll("/",
    // "");
    // return(shost);
    // } // gethost

    /**
     * check if a youtube-URL was pasted or typed in if yes cut it out and send
     * it to the URLList to get processed by one of the threads
     * 
     * the user can paste a long string containing many youtube-URLs .. but here
     * is work to do because we have to erase the string(s) that remain(s)
     */
    void checkInputFieldforYTURLs() {
        String sinput = frame.textinputfield.getText(); // dont call
                                                        // .toLowerCase() !

        // TODO this can probably be done better - replace input so URLs get
        // extracted without user activity (works even if URLs are spread across
        // multiple lines and pasted at once)
        sinput = sinput.replaceAll("&feature=fvwp&", "&"); // after that text
                                                           // there could be
                                                           // another yt-URL or
                                                           // more query_string
                                                           // options
        sinput = sinput.replaceAll("&feature=fvwphttp", "http");
        sinput = sinput.replaceAll("&feature=fvwp", "");
        sinput = sinput.replaceAll("&feature=related&", "&");
        sinput = sinput.replaceAll("&feature=relatedhttp", "http");
        sinput = sinput.replaceAll("&feature=related", "");
        sinput = sinput.replaceAll("&feature=mfu_in_order&list=[0-9A-Z]{1,2}", "");
        sinput = sinput.replaceAll("&feature=[a-zA-Z]{1,2}&list=([a-zA-Z0-9]*)&index=[0-9]{1,2}", "");
        sinput = sinput.replaceAll("&feature=[0-9A-Z]{1,3}&list=(PL[a-zA-Z0-9]{16})&index=[0-9]{1,2}", "");
        sinput = sinput.replaceAll("&playnext=[0-9A-Z]{1,2}&list=(PL[a-zA-Z0-9]{16})", "");
        sinput = sinput.replaceAll("&NR=[0-9]&", "&");
        sinput = sinput.replaceAll("&NR=[0-9]http", "http");
        sinput = sinput.replaceAll("&NR=[0-9]", "");
        sinput = sinput.replaceAll(" ", "");
        sinput = sinput.replaceAll(szPLAYLISTREGEX, "/watch?v=");

        String surl = sinput.replaceFirst(szYTREGEX, "");

        // if nothing could be replaced we have to yt-URL found
        if (sinput.equals(surl))
            return;

        debugoutput("sinput: ".concat(sinput).concat(" surl: ".concat(surl)));

        // starting at index 0 because szYTREGEX should start with ^ // if
        // szYTREGEX does not start with ^ then you have to find the index where
        // the match is before you can cut out the URL
        surl = sinput.substring(0, sinput.length() - surl.length());
        addYTURLToList(surl);
        sinput = sinput.substring(surl.length());
        debugoutput(String.format("sinput: %s surl: %s", sinput, surl));

        // if remaining text is shorter than shortest possible yt-url we delete
        // it
        if (sinput.length() < "youtube.com/watch?v=0123456789a".length())
            sinput = "";

        // frame.textinputfield.setText(sinput); // generates a
        // java.lang.IllegalStateException: Attempt to mutate in notification

        final String fs = sinput;

        // let a thread update the textfield in the UI
        Thread worker = new Thread() {
            public void run() {
                synchronized (YTD2.frame.textinputfield) {
                    YTD2.frame.textinputfield.setText(fs);
                }
            }
        };
        SwingUtilities.invokeLater(worker);
    } // checkInputFieldforYTURLS

    ImageIcon createImageIcon(String path, String description) {
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

    /**
     * processing event of dropping a HTTP URL, YT-Video Image or plain text
     * (URL) onto the frame
     * 
     * seems not to work with M$-IE (8) - what a pity!
     */
    public void drop(DropTargetDropEvent dtde) {
        Transferable tr = dtde.getTransferable();
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        DataFlavor fl = null;
        String str = "";

        debugoutput("DataFlavors found: ".concat(Integer.toString(flavors.length)));
        for (int i = 0; i < flavors.length; i++) {
            fl = flavors[i];
            if (fl.isFlavorTextType() /*
                                       * || fl.isMimeTypeEqual("text/html") ||
                                       * fl
                                       * .isMimeTypeEqual("application/x-java-url"
                                       * ) ||
                                       * fl.isMimeTypeEqual("text/uri-list")
                                       */) {
                try {
                    dtde.acceptDrop(dtde.getDropAction());
                } catch (Throwable t) {
                }
                try {
                    if (tr.getTransferData(fl) instanceof InputStreamReader) {
                        debugoutput("Text-InputStream");
                        BufferedReader textreader = new BufferedReader((Reader) tr.getTransferData(fl));
                        String sline = "";
                        try {
                            while (sline != null) {
                                sline = textreader.readLine();
                                if (sline != null)
                                    str += sline;
                            }
                        } catch (Exception e) {
                        } finally {
                            textreader.close();
                        }
                        str = str.replaceAll("<[^>]*>", ""); // remove HTML
                                                             // tags, especially
                                                             // a hrefs - ignore
                                                             // HTML characters
                                                             // like &szlig;
                                                             // (which are no
                                                             // tags)
                    } else if (tr.getTransferData(fl) instanceof InputStream) {
                        debugoutput("Byte-InputStream");
                        InputStream input = new BufferedInputStream((InputStream) tr.getTransferData(fl));
                        int idata = input.read();
                        String sresult = "";
                        while (idata != -1) {
                            if (idata != 0)
                                sresult += new Character((char) idata).toString();
                            idata = input.read();
                        } // while
                        debugoutput("sresult: ".concat(sresult));
                    } else {
                        str = tr.getTransferData(fl).toString();
                    }
                } catch (IOException ioe) {
                } catch (UnsupportedFlavorException ufe) {
                }

                debugoutput("drop event text: ".concat(str).concat(" (").concat(fl.getMimeType()).concat(") "));
                // insert text into textfield - almost the same as user drops
                // text/url into this field
                // except special characaters -> from
                // http://de.wikipedia.org/wiki/GNU-Projekt („GNU is not
                // Unix“)(&bdquo;GNU is not Unix&ldquo;)
                // two drops from same source .. one time in textfield and
                // elsewhere - maybe we change that later?!
                if (str.matches(szYTREGEX.concat("(.*)"))) {
                    synchronized (YTD2.frame.textinputfield) {
                        YTD2.frame.textinputfield.setText(str.concat(YTD2.frame.textinputfield
                                .getText()));
                    }
                    debugoutput("breaking for-loop with str: ".concat(str));
                    break;
                }
            } else {
                String sv = "drop event unknown type: ".concat(fl.getHumanPresentableName());
                // output(sv);
                debugoutput(sv);
            }
        } // for

        dtde.dropComplete(true);
    } // drop()

} // class JFCMainClient