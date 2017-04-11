package edu.rutgers.winlab.mfiot.mfiotconnector;

/**
 * Log generator that can add/remove message handlers.
 *
 * @author Jiachen Chen
 */
public interface ILogCreator {

    /**
     * Add a new message handler to the log creator.
     *
     * The handleMessage will be called when the log creator creates a message.
     *
     * @param listener the message handler.
     * @return if the message handler is added successfully. False if the
     * listener is already listening.
     */
    public boolean addMessageListener(IMessageListener listener);

    /**
     * Remove a message handler from the log creator.
     *
     * @param listener the message handler.
     * @return if the message handler is removed successfully. False if the
     * listener is not listening.
     */
    public boolean removeMessageListener(IMessageListener listener);
}
