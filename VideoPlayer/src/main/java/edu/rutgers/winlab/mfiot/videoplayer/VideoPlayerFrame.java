/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videoplayer;

import edu.rutgers.winlab.jmfapi.GUID;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author ubuntu
 */
public class VideoPlayerFrame extends JFrame {

    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String VIDEO_FILE_NAME = "videos.txt";

    public static class VideoEntry {

        public GUID guid;
        public long time;

        public VideoEntry(GUID g, long time) {
            this.guid = g;
            this.time = time;
        }

        @Override
        public String toString() {
            return String.format("%s | %s", DEFAULT_DATE_FORMAT.format(new Date(time)), guid);
        }

        public void writeToFile(PrintStream output) {
            output.printf("%d,%d%n", time, guid.getGUID());
            output.flush();
        }

        public static VideoEntry fromLine(String line) {
            String[] parts = line.split(",");
            if (parts.length != 2) {
                return null;
            }
            try {
                long t = Long.parseLong(parts[0]);
                int g = Integer.parseInt(parts[1]);
                return new VideoEntry(new GUID(g), t);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private final VideoPanel vp;
    private final JList<VideoEntry> list;
    private final DefaultListModel<VideoEntry> listModel = new DefaultListModel<>();
    private PrintStream output = null;
    private final JCheckBox jcb = new JCheckBox("Realtime");
    private GUID vpGUID = null;

    public VideoPlayerFrame(ChunkLoader loader) {
        super("Video player");

        try (FileReader fr = new FileReader(VIDEO_FILE_NAME)) {
            try (BufferedReader br = new BufferedReader(fr)) {
                String line;
                int lineNumber = 1;
                while ((line = br.readLine()) != null) {
                    VideoEntry entry = VideoEntry.fromLine(line);
                    if (entry == null) {
                        System.out.printf("Skip line: %d %s%n", lineNumber, line);
                    } else {
                        System.out.printf("Line: %d %s%n", lineNumber, entry);
                        listModel.add(0, entry);
                    }
                    lineNumber++;
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot load video file. Skip.");
        }

        try {
            output = new PrintStream(new FileOutputStream(VIDEO_FILE_NAME, true), true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(VideoPlayerFrame.class.getName()).log(Level.SEVERE, "Cannot store video file", ex);
        }

        getContentPane().setLayout(new BorderLayout());
        vp = new VideoPanel(loader);
        list = new JList<>(listModel);

        getContentPane().add(getMainPanel(), BorderLayout.CENTER);
        getContentPane().add(getLeftPanel(), BorderLayout.WEST);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                GUID g = list.getSelectedValue().guid;
                if (g.equals(vpGUID)) {
                    return;
                }
                vp.loadVideo(g);
                vpGUID = g;
            }
        });
        if (listModel.getSize() > 0) {
            list.setSelectedIndex(0);
        }
    }

    public void addGUID(GUID guid) {
        VideoEntry entry = new VideoEntry(guid, System.currentTimeMillis());
        if (output != null) {
            entry.writeToFile(output);
        }
        listModel.add(0, entry);
        pack();
        if (jcb.isSelected()) {
            list.setSelectedIndex(0);
        }
//        listModel.addElement(guid);
    }

    private JComponent getLeftPanel() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane jsp = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        list.setMinimumSize(new Dimension(200, 0));

        JPanel p = new JPanel(new BorderLayout());
        p.add(jsp, BorderLayout.CENTER);
        p.add(jcb, BorderLayout.NORTH);
        return p;

    }

    private JComponent getMainPanel() {
        JLayeredPane jlp = new JLayeredPane();

        vp.setBounds(0, 0, vp.getPreferredSize().width, vp.getPreferredSize().height);
        jlp.add(vp, new Integer(0));

        JButton jb = new JButton("Rewind");
        jb.setMnemonic('R');
        jb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                vp.rewind();
            }
        });
        JPanel jp = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        jp.setOpaque(false);
        jp.add(jb);
        jp.setBounds(0, 0, vp.getPreferredSize().width, vp.getPreferredSize().height);
        jlp.add(jp, new Integer(1));
        jlp.setPreferredSize(vp.getPreferredSize());
        return jlp;
    }
}
