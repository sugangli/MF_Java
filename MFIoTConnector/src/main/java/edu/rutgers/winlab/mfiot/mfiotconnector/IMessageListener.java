package edu.rutgers.winlab.mfiot.mfiotconnector;

/**
 * A general message handler
 * 
 * @author Jiachen Chen
 */
public interface IMessageListener {

    /**
     * Handles a message.
     * @param type The type of the message.
     * @param message The message body.
     */
    public void handleMessage(LogType type, String message);
}
