/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.mfiotconnector;

import edu.rutgers.winlab.mfiot.packets.Packet;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The connector for MF-IoT (RIOT) using pyterm.
 *
 * @author Jiachen Chen
 */
public class MFIoTConnector implements ILogCreator {

    /**
     * The control command for sending a message.
     */
    public static final String CTLSND = "ctlsnd ";
    /**
     * The control command for buffering a message.
     */
    public static final String CTLBUF = "ctlbuf ";
    /**
     * The maximum length of a command line.
     */
    public static final int LINE_LENGTH = 126;
    /**
     * The MTU of an MF-IoT packet.
     */
    public static final int MTU = 116;

    private boolean _running;
    private final String _command[];
    private final String _envp[];
    private Process p;
    private PrintStream output;
    private final LinkedList<IPacketListener> _packetListeners;
    private final LinkedList<IMessageListener> _messageListeners;
    private final String _port;

    /**
     * Returns if the connector is running.
     *
     * @return if the connector is running.
     */
    public boolean isRunning() {
        return _running;
    }

    /**
     * Add a new packet handler to the connector.
     *
     * The handlePacket function will be called when the connector gets a packet
     * snip.
     *
     * @param listener the packet handler.
     * @return if the packet handler is added successfully. False if the
     * listener is already listening.
     */
    public boolean addPacketListener(IPacketListener listener) {
        if (_packetListeners.indexOf(listener) != -1) {
            log(LogType.ERROR_MESSAGE, "Packet listener already exit");
            return false;
        }
        _packetListeners.addLast(listener);
        return true;
    }

    /**
     * Remove a packet handler from the connector.
     *
     * @param listener the packet handler.
     * @return if the packet handler is removed successfully. False if the
     * listener is not listening.
     */
    public boolean removePacketListener(IPacketListener listener) {
        return _packetListeners.remove(listener);
    }

    @Override
    public boolean addMessageListener(IMessageListener listener) {
        if (_messageListeners.indexOf(listener) != -1) {
            log(LogType.ERROR_MESSAGE, "Packet listener already exit");
            return false;
        }
        _messageListeners.addLast(listener);
        return true;
    }

    @Override
    public boolean removeMessageListener(IMessageListener listener) {
        return _messageListeners.remove(listener);
    }

    /**
     * Constructs a new MF-IoT connector.
     *
     * @param command the pyterm command.
     * @param port the port to listen to.
     * @param home the home folder to start the pyterm.
     */
    public MFIoTConnector(String command, String port, String home) {
        _command = new String[]{command, "-p", port, "-b", "115200"};
        _envp = new String[]{"HOME=" + home};
        _packetListeners = new LinkedList<>();
        _messageListeners = new LinkedList<>();
        _port = port;

        Runtime.getRuntime().addShutdownHook(
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        exit();
                    }
                })
        );
    }

    /**
     * Start listening using pyterm.
     */
    public void start() {
        synchronized (this) {
            if (isRunning()) {
                log(LogType.ERROR_MESSAGE, "Terminal already running");
                return;
            }
            try {
                log(LogType.MESSAGE, "Listening to port: " + _port);
                p = Runtime.getRuntime().exec(_command, _envp);
                _running = true;
                Thread t;
                t = new Thread(new ListenThread("Output", new BufferedReader(new InputStreamReader(p.getInputStream())), LogType.OUTPUT));
                t.setDaemon(true);
                t.start();
                t = new Thread(new ListenThread("Error", new BufferedReader(new InputStreamReader(p.getErrorStream())), LogType.OUTPUT));
                t.setDaemon(true);
                t.start();
                output = new PrintStream(p.getOutputStream());
            } catch (IOException ex) {
                log(ex);
                Logger.getLogger(MFIoTConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Send a Ethernet packet using the board.
     *
     * @param macAddr the next hop mac address.
     * @param buf the packet payload.
     */
    public void sendPacket(String macAddr, byte[] buf) {
        if (buf.length > MTU) {
            log(LogType.ERROR_MESSAGE, "Packet should not be bigger than " + MTU + "bytes");
            return;
        }
        synchronized (this) {
            if (!isRunning()) {
                log(LogType.ERROR_MESSAGE, "Should start the terminal first");
                return;
            }
            StringBuilder builder = new StringBuilder(macAddr);
            builder.append(' ');
            for (byte b : buf) {
                builder.append(String.format("%02X", b));
            }
            while (builder.length() + CTLSND.length() > LINE_LENGTH) {
                StringBuilder b2 = new StringBuilder(CTLBUF);
                int remainingLength = LINE_LENGTH - b2.length();
                b2.append(builder.subSequence(0, remainingLength));
                builder.replace(0, remainingLength, "");
                sendCommand(b2.toString());
            }
            sendCommand(CTLSND + builder.toString());
        }
    }

    /**
     * Exit the connector.
     */
    public void exit() {
        synchronized (this) {
            if (!isRunning()) {
                log(LogType.ERROR_MESSAGE, "Should start the terminal first");
                return;
            }
            log(LogType.MESSAGE, "Exiting...");
            sendCommand("/exit");
            log(LogType.MESSAGE, "Exitted");
            _running = false;
        }
    }

    /**
     * Send a command to the board.
     * <br/>
     * The length of the command should not exceed LINE_LENGTH.
     * 
     * @param command the command to be sent.
     */
    public void sendCommand(String command) {
        if (command.length() > LINE_LENGTH) {
            log(LogType.ERROR_MESSAGE, "Command should not be longer than " + LINE_LENGTH + "bytes");
            return;
        }
        synchronized (this) {
            if (!isRunning()) {
                log(LogType.ERROR_MESSAGE, "Should start the terminal first");
                return;
            }
            log(LogType.INPUT, command);
            output.println(command);
            output.flush();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(MFIoTConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void log(LogType type, String str) {
        for (IMessageListener l : _messageListeners) {
            l.handleMessage(type, str);
        }
    }

    private void log(Exception e) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos)) {
            e.printStackTrace(ps);
            ps.flush();
            baos.flush();
            byte[] buf = baos.toByteArray();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log(LogType.ERROR_MESSAGE, line);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    private void firePacketEvent(PacketMessageType t, Packet p) {
        for (IPacketListener l : _packetListeners) {
            l.handlePacket(t, p);
        }
    }

    private enum PacketReadStatus {

        NONE,
        BEGIN,
        DATA,
        DESCRIPTION;
    };

    private static final Pattern DATA_LINE_BEGIN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} - INFO #\\W+\\d+\\W+");
    private static final Pattern SNIP_LINE_FORMAT = Pattern.compile("~~ SNIP\\W+\\d+\\W+-\\W+size:\\W+(\\d+)\\W+byte,\\W+type:\\W+(.+)\\W+\\(([-\\d]+)\\)");

    private class ListenThread implements Runnable {

        private static final String PACKET_RECEIVE_START_MAGIC = "PKTDUMP: data received:";
        private static final String PACKET_SEND_START_MAGIC = "PKTDUMP: data to send:";
        private static final String PACKET_HEADER_MAGIC = "~~ SNIP";
        private static final String PACKET_FINISH_MAGIC = "~~ PKT";

        private final String _name;
        private final BufferedReader _reader;
        private final LogType _type;
        private PacketReadStatus _status;

        private PacketMessageType _packetMessageType;
        private final LinkedList<String> _packetString = new LinkedList<>();
        private int _packetSize;
        private int _packetIntType;
        private String _packetType;
        private byte[] _packetBuf;
        private int _packetBufRead;
        private final StringBuilder _packetDescription = new StringBuilder();
        private Packet _packetLast;

        public ListenThread(String name, BufferedReader reader, LogType type) {
            _name = name;
            _reader = reader;
            _type = type;
            _status = PacketReadStatus.NONE;
        }

        @Override
        public void run() {
            log(LogType.MESSAGE, _name + " listener started.");
            while (_running) {
                try {
                    String line = _reader.readLine();
                    if (line != null) {
                        switch (_status) {
                            case NONE: {
                                boolean isReceive = line.contains(PACKET_RECEIVE_START_MAGIC);
                                if (isReceive || line.contains(PACKET_SEND_START_MAGIC)) {
                                    _status = PacketReadStatus.BEGIN;
                                    _packetLast = null;
                                    _packetString.clear();
                                    _packetString.addLast(line);
                                    _packetMessageType = isReceive ? PacketMessageType.RECEIVE : PacketMessageType.SEND;
                                } else {
                                    log(_type, line);
                                }
                                break;
                            }
                            case DESCRIPTION: {
                                if (line.contains(PACKET_HEADER_MAGIC)) {
                                    _packetLast = new Packet(_packetBuf, _packetIntType, _packetType, _packetDescription.toString(), _packetLast);
                                    _status = PacketReadStatus.BEGIN;
                                } else if (line.contains(PACKET_FINISH_MAGIC)) {
                                    _packetLast = new Packet(_packetBuf, _packetIntType, _packetType, _packetDescription.toString(), _packetLast);
                                    firePacketEvent(_packetMessageType, _packetLast);
                                    _packetString.addLast(line);
                                    clearPacketBuf(LogType.PACKET);
                                    _status = PacketReadStatus.NONE;
                                    break;
                                } else {
                                    _packetString.addLast(line);
                                    _packetDescription.append(line);
                                    _packetDescription.append('\n');
                                    break;
                                }
                            }
                            case BEGIN: {
                                _packetString.addLast(line);
                                Matcher m = SNIP_LINE_FORMAT.matcher(line);
                                if (m.find()) {
                                    try {
                                        _packetSize = Integer.parseInt(m.group(1));
                                        _packetType = m.group(2);
                                        _packetIntType = Integer.parseInt(m.group(3));
                                        _packetBuf = new byte[_packetSize];
                                        _packetBufRead = 0;
                                        _status = PacketReadStatus.DATA;
                                        break;
                                    } catch (NumberFormatException e) {
                                    }
                                }
                                clearPacketBuf(_type);
                                _status = PacketReadStatus.NONE;
                                break;
                            }
                            case DATA: {
                                _packetString.addLast(line);
                                Matcher m = DATA_LINE_BEGIN.matcher(line);
                                if (m.find()) {
                                    try {
                                        readBinary(line.substring(m.end()));
                                        if (_packetBufRead == _packetSize) {
                                            _packetDescription.replace(0, _packetDescription.length(), "");
                                            _status = PacketReadStatus.DESCRIPTION;
                                        }
                                        break;
                                    } catch (Exception e) {
                                        e.printStackTrace(System.err);
                                    }
                                }
                                clearPacketBuf(_type);
                                _status = PacketReadStatus.NONE;
                                break;
                            }
                        }

                    }
                } catch (IOException | RuntimeException e) {
                    log(e);
                }
            }
            log(LogType.MESSAGE, _name + " listener exitted.");
        }

        private void clearPacketBuf(LogType type) {
            for (String line : _packetString) {
                log(type, line);
            }
            _packetString.clear();
        }

        private void readBinary(String binary) throws Exception {
            String[] tmp = binary.split("\\W");
            for (String b : tmp) {
                if (b.length() != 2) {
                    throw new Exception("Error binary format");
                }
                _packetBuf[_packetBufRead++] = (byte) Integer.parseInt(b, 16);
            }
        }
    }

}
