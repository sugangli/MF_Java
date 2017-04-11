package edu.rutgers.winlab.mfiot.mfiotconnector;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

/**
 * The log viewer GUI.
 *
 * @author Jiachen Chen
 */
public class LogViewScrollPane extends JScrollPane implements IMessageListener {

    private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {

        @Override
        public void write(int b) throws IOException {

        }
    };
    private static final PrintStream NULL_PRINTSTREAM = new PrintStream(NULL_OUTPUT_STREAM);

    private final DefaultStyledDocument _commandDocument = new DefaultStyledDocument();
    private final JTextPane _commandView = new JTextPane(_commandDocument);
    private final HashMap<LogType, Style> _styleMap = new HashMap<>();
    private final PrintStream rawOutput;

    /**
     * Creates a new log viewer.
     *
     * @param rawOutput the stream to print the raw output.
     */
    public LogViewScrollPane(PrintStream rawOutput) {
        this.rawOutput = rawOutput == null ? NULL_PRINTSTREAM : rawOutput;
        setViewportView(_commandView);
        _commandView.setEditable(false);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        Style s;

        s = _commandDocument.addStyle("input", null);
        s.addAttribute(StyleConstants.Foreground, Color.BLUE);
        s.addAttribute(StyleConstants.FontSize, 14);
        s.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
        s.addAttribute(StyleConstants.Background, Color.LIGHT_GRAY);
        s.addAttribute(StyleConstants.Bold, true);
        _styleMap.put(LogType.INPUT, s);

        s = _commandDocument.addStyle("output", null);
        s.addAttribute(StyleConstants.Foreground, Color.BLACK);
        s.addAttribute(StyleConstants.FontSize, 12);
        s.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
        s.addAttribute(StyleConstants.Bold, false);
        _styleMap.put(LogType.OUTPUT, s);

        s = _commandDocument.addStyle("error", null);
        s.addAttribute(StyleConstants.Foreground, Color.RED);
        s.addAttribute(StyleConstants.FontSize, 12);
        s.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
        s.addAttribute(StyleConstants.Bold, false);
        _styleMap.put(LogType.ERROR, s);

        s = _commandDocument.addStyle("message", null);
        s.addAttribute(StyleConstants.Foreground, Color.DARK_GRAY);
        s.addAttribute(StyleConstants.FontSize, 14);
        s.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
        s.addAttribute(StyleConstants.Bold, false);
        _styleMap.put(LogType.MESSAGE, s);

        s = _commandDocument.addStyle("errorMessage", null);
        s.addAttribute(StyleConstants.Foreground, Color.RED);
        s.addAttribute(StyleConstants.FontSize, 14);
        s.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
        s.addAttribute(StyleConstants.Bold, true);
        _styleMap.put(LogType.ERROR_MESSAGE, s);

        s = _commandDocument.addStyle("packet", null);
        s.addAttribute(StyleConstants.Foreground, new Color(0, 128, 0));
        s.addAttribute(StyleConstants.FontSize, 14);
        s.addAttribute(StyleConstants.FontFamily, Font.MONOSPACED);
        s.addAttribute(StyleConstants.Bold, true);
        _styleMap.put(LogType.PACKET, s);

    }

    /**
     * Places the log into the GUI.
     *
     * @param type type of the log.
     * @param message the message body.
     */
    @Override
    public void handleMessage(LogType type, String message) {
        try {
            int pos = _commandDocument.getLength();
            _commandDocument.insertString(pos, message + "\n", null);
            _commandDocument.setParagraphAttributes(pos, 1, _styleMap.get(type), true);
            Rectangle r = _commandView.getBounds();
            this.viewport.setViewPosition(new Point(0, r.height));
            rawOutput.printf("%s> %s%n", type.toString(), message);
        } catch (BadLocationException | NullPointerException ex) {
            Logger.getLogger(LogViewScrollPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Clear the log in the viewer.
     */
    public void clearLog() {
        try {
            _commandDocument.remove(0, _commandDocument.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(LogViewScrollPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
