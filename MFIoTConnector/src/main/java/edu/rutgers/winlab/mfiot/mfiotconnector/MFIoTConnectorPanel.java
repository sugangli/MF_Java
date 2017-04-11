package edu.rutgers.winlab.mfiot.mfiotconnector;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.PrintStream;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The GUI panel for MF-IoT connector.
 * 
 * @author Jiachen Chen
 */
public class MFIoTConnectorPanel extends JPanel {

    private final MFIoTConnector _connector;
    private final LogViewScrollPane _logViewPanel;
    private final JTextField _commandField;
    private final JTextField _addressField;
    private final JSlider _sizeSlider;

    private final ActionListener _sendCommandActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            String txt = _commandField.getText();
            switch (txt) {
                case "/connect":
                    _connector.start();
                    break;
                case "/exit":
                    _connector.exit();
                    break;
                case "/clear":
                    _logViewPanel.clearLog();
                    break;
                default:
                    _connector.sendCommand(_commandField.getText());
                    break;
            }
            _commandField.selectAll();
            _commandField.grabFocus();
        }
    };

    private final ActionListener _sendPacketActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            int val = _sizeSlider.getValue();
            byte[] buf = new byte[val];
            for (int i = 0; i < val; i++) {
                buf[i] = (byte) (Math.random() * 256);
            }
            _connector.sendPacket(_addressField.getText(), buf);
            _addressField.selectAll();
            _addressField.grabFocus();
        }
    };

    /**
     * Creates a MF-IoT connector GUI using the connector.
     * 
     * @param connector the underlying MF-connector.
     * @param rawOutput the output for raw messages
     */
    public MFIoTConnectorPanel(MFIoTConnector connector, PrintStream rawOutput) {
        super(new BorderLayout());

        JLabel label;
        JButton btn;

        _connector = connector;
        _logViewPanel = new LogViewScrollPane(rawOutput);
        connector.addMessageListener(_logViewPanel);

        _commandField = new JTextField();
        _commandField.addActionListener(_sendCommandActionListener);
        _commandField.setFont(Font.decode(Font.MONOSPACED));
        _addressField = new JTextField();
        _addressField.setFont(Font.decode(Font.MONOSPACED));
        _addressField.setColumns(10);
        _sizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 116, 64);

        JPanel logContainer = new JPanel(new BorderLayout(2, 2));
        logContainer.setBorder(new TitledBorder("Log"));
        logContainer.add(_logViewPanel, BorderLayout.CENTER);

        JPanel controlContainer = new JPanel(new GridLayout(2, 1, 1, 1));
        controlContainer.setBorder(new TitledBorder("Controls"));

        JPanel sendCommandContainer = new JPanel(new BorderLayout(2, 2));
        controlContainer.add(sendCommandContainer);

        label = new JLabel("<HTML><u>C</u>ommand: </HTML>");
        label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt C"), label);
        label.getActionMap().put(label, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _commandField.grabFocus();
                _commandField.selectAll();
            }
        });
        sendCommandContainer.add(label, BorderLayout.LINE_START);

        sendCommandContainer.add(_commandField, BorderLayout.CENTER);

        btn = new JButton("<HTML><u>S</u>end</HTML>");
        btn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt S"), btn);
        btn.getActionMap().put(btn, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _sendCommandActionListener.actionPerformed(e);
            }
        });
        sendCommandContainer.add(btn, BorderLayout.LINE_END);
        btn.addActionListener(_sendCommandActionListener);

        JPanel sendPacketContainer = new JPanel(new BorderLayout());
        controlContainer.add(sendPacketContainer);

        JPanel addressContainer = new JPanel(new BorderLayout());
        label = new JLabel("<HTML><u>A</u>ddress: </HTML>");
        label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt A"), label);
        label.getActionMap().put(label, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _addressField.grabFocus();
                _addressField.selectAll();
            }
        });

        addressContainer.add(label, BorderLayout.LINE_START);
        addressContainer.add(_addressField, BorderLayout.CENTER);
        sendPacketContainer.add(addressContainer, BorderLayout.LINE_START);

        JPanel sizeContainer = new JPanel(new BorderLayout());
        final JLabel tmp = new JLabel("<HTML>Si<u>z</u>e: (" + _sizeSlider.getValue() + ")</HTML>");
        tmp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt Z"), tmp);
        tmp.getActionMap().put(tmp, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _sizeSlider.grabFocus();
            }
        });
        sizeContainer.add(tmp, BorderLayout.LINE_START);
        sizeContainer.add(_sizeSlider, BorderLayout.CENTER);
        _sizeSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                tmp.setText("<HTML>Si<u>z</u>e: (" + _sizeSlider.getValue() + ")</HTML>");
            }
        });
        sendPacketContainer.add(sizeContainer, BorderLayout.CENTER);

        btn = new JButton("<HTML>S<u>e</u>nd</HTML>");
        btn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt E"), btn);
        btn.getActionMap().put(btn, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _sendPacketActionListener.actionPerformed(e);
            }
        });
        btn.addActionListener(_sendPacketActionListener);
        sendPacketContainer.add(btn, BorderLayout.LINE_END);

        add(logContainer, BorderLayout.CENTER);
        add(controlContainer, BorderLayout.PAGE_END);

        this.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                _commandField.grabFocus();
            }

        });

    }
}
