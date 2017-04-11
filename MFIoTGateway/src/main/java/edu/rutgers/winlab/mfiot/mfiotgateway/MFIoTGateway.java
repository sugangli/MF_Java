/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.mfiotgateway;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.jmfapi.JMFAPI;
import edu.rutgers.winlab.jmfapi.JMFException;
import edu.rutgers.winlab.mfiot.mfiotconnector.IMessageListener;
import edu.rutgers.winlab.mfiot.mfiotconnector.IPacketListener;
import edu.rutgers.winlab.mfiot.mfiotconnector.LogType;
import edu.rutgers.winlab.mfiot.mfiotconnector.MFIoTConnector;
import edu.rutgers.winlab.mfiot.mfiotconnector.MFIoTConnectorPanel;
import edu.rutgers.winlab.mfiot.mfiotconnector.PacketMessageType;
import edu.rutgers.winlab.mfiot.packets.LUID;
import edu.rutgers.winlab.mfiot.packets.MFIoTPacket;
import edu.rutgers.winlab.mfiot.packets.Packet;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author ubuntu
 */
public class MFIoTGateway {

    public static final String PROPERTIES_DEFAULT_PORT = "DefaultPort";
    public static final String PROPERTIES_PYTERM_LOCATION = "PytermLocation";
    public static final String PROPERTIES_PYTERM_HOME = "PytermHome";
    public static final String PROPERTIES_RAW_OUTPUT = "RawOutput";

    public static final String RAW_OUTPUT_SYSTEM_OUT = "System.out";
    public static final String RAW_OUTPUT_SYSTEM_ERR = "System.err";

    public static final String DEFAULT_PROPERTIES_FILE = "mfiotgateway.properties";

    private final MFIoTConnector connector;
    private final HashMap<GUID, JMFAPI> handles = new HashMap<>();
    private final GUIDLUIDTable mapping = new GUIDLUIDTable();
    private String acmID, pyterm, home;
    private PrintStream rawOutput;
//    private final MFIoTConnectorPanel panel;
//    private final JFrame frame;

    private final Thread shutdownHook = new Thread("shutdown hook") {

        @Override
        public void run() {
            System.out.println("Closing");
            for (Map.Entry<GUID, JMFAPI> entrySet : handles.entrySet()) {
                GUID key = entrySet.getKey();
                JMFAPI value = entrySet.getValue();
                System.out.printf("Closing handle: %s%n", key);
                try {
                    if (value.isOpen()) {
                        value.jmfclose();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(MFIoTGateway.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    };

    private final IPacketListener packetListener = new IPacketListener() {

        @Override
        public void handlePacket(PacketMessageType type, Packet packet) {
            MFIoTPacket m_packet = new MFIoTPacket(packet.getNext());

            LUID src = new LUID(m_packet.getSrc()), dst = new LUID(m_packet.getDst());
            System.out.printf("src=%s, dst=%s, %n", src, dst);

            GUID[] srcGUIDs = mapping.getGUIDs(src);

            if (srcGUIDs != null) {
                GUID srcGUID = srcGUIDs[0];
                GUID[] dstGUIDs = mapping.getGUIDs(dst);
                if (dstGUIDs != null) {
                    try {
                        JMFAPI handle = handles.get(srcGUID);
                        if (handle == null) {
                            handle = new JMFAPI();
                            handle.jmfopen("basic", srcGUID);
                            System.out.printf("creating handle for %s%n", srcGUID);
                            handles.put(srcGUID, handle);
                        }
                        for (GUID dstGUID : dstGUIDs) {
                            System.out.printf("src: %s->%s, dst: %s->%s%n", src, srcGUID, dst, dstGUID);
                            byte[] payload = packet.getNext().getNext().getBuf();
                            int ret = handle.jmfsend(payload, payload.length, dstGUID);
                            System.out.printf("packet sent, ret=%d%n", ret);
                        }
                    } catch (JMFException ex) {
                        Logger.getLogger(MFIoTConnector.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.printf("Cannot find GUID for dst: %s%n", dst);
                }
            } else {
                System.out.printf("Cannot find GUID for src: %s%n", src);
            }
        }
    };

    private final IMessageListener messageListener = new IMessageListener() {
        @Override
        public void handleMessage(LogType type, String message) {
            rawOutput.println(String.format("%s: %s", type, message));
        }
    };

    private void parseParameters(String propertiesFile) throws IOException {

        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
            p.load(fis);
            System.out.printf("Getting %s...%n", PROPERTIES_DEFAULT_PORT);
            acmID = p.getProperty(PROPERTIES_DEFAULT_PORT);
            System.out.printf("%s: %s%n", PROPERTIES_DEFAULT_PORT, acmID);
            System.out.printf("Getting %s...%n", PROPERTIES_PYTERM_LOCATION);
            pyterm = p.getProperty(PROPERTIES_PYTERM_LOCATION);
            System.out.printf("%s: %s%n", PROPERTIES_PYTERM_LOCATION, pyterm);
            System.out.printf("Getting %s...%n", PROPERTIES_PYTERM_HOME);
            home = p.getProperty(PROPERTIES_PYTERM_HOME);
            System.out.printf("%s: %s%n", PROPERTIES_PYTERM_HOME, home);
            // rawOutput
            String strRawOutput = p.getProperty(PROPERTIES_RAW_OUTPUT);
            if (strRawOutput == null) {
                rawOutput = null;
                System.out.printf("No raw output.%n");
            } else {
                switch (strRawOutput) {
                    case RAW_OUTPUT_SYSTEM_OUT:
                        rawOutput = System.out;
                        System.out.printf("Using %s as raw output.%n", RAW_OUTPUT_SYSTEM_OUT);
                        break;
                    case RAW_OUTPUT_SYSTEM_ERR:
                        rawOutput = System.err;
                        System.out.printf("Using %s as raw output.%n", RAW_OUTPUT_SYSTEM_ERR);
                        break;
                    default:
                        rawOutput = new PrintStream(new FileOutputStream(strRawOutput), true);
                        System.out.printf("Using file %s as raw output.%n", strRawOutput);
                        break;
                }
            }
        }
    }

    public MFIoTGateway(String propertiesFile) throws IOException {
        parseParameters(propertiesFile);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        connector = new MFIoTConnector(pyterm, acmID, home);
        connector.addMessageListener(messageListener);
        connector.addPacketListener(packetListener);
//        panel = new MFIoTConnectorPanel(connector, rawOutput);
//        frame = new JFrame("Aggregator on " + acmID);
//        frame.addWindowListener(new WindowAdapter() {
//
//            @Override
//            public void windowOpened(WindowEvent e) {
//                panel.grabFocus();
//            }
//
//        });

//        frame.getContentPane().add(panel, BorderLayout.CENTER);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(800, 600);
    }

    public void start() {
//        frame.setVisible(true);
//        try {
//            Thread.sleep(300);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(MFIoTGateway.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        frame.setLocation(0, 0);
//        try {
//            Thread.sleep(300);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(MFIoTGateway.class.getName()).log(Level.SEVERE, null, ex);
//        }
        connector.start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String propertiesFile = DEFAULT_PROPERTIES_FILE;

        if (args.length > 0) {
            propertiesFile = args[0];
        }
        System.out.println("Using setting file:" + propertiesFile);
        MFIoTGateway gw = new MFIoTGateway(propertiesFile);
        initTables(gw);
        gw.start();
        while (true) {
            Thread.sleep(1000);
        }
    }

    public static void initTables(MFIoTGateway gateway) {
        gateway.mapping.put(new GUID(0x10102), LUID.fromValue(0x0102));
        gateway.mapping.put(new GUID(0x10101), LUID.fromValue(0x0101));
        gateway.mapping.put(new GUID(0x10104), LUID.fromValue(0x0104));
        gateway.mapping.put(new GUID(0x10106), LUID.fromValue(0x0104));
        gateway.mapping.put(new GUID(0x10105), LUID.fromValue(0x0105));
    }

}
