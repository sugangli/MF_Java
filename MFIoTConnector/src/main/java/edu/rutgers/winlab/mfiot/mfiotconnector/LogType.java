package edu.rutgers.winlab.mfiot.mfiotconnector;

/**
 * Log type of pyterm
 *
 * @author Jiachen Chen
 */
public enum LogType {

    /**
     * Default input to pyterm
     */
    INPUT,
    /**
     * Default output from pyterm
     */
    OUTPUT,
    /**
     * Got a packet
     */
    PACKET,
    /**
     * Default error from pyterm
     */
    ERROR,
    /**
     * Error message sent from connector
     */
    ERROR_MESSAGE,
    /**
     * Normal message sent from connector
     */
    MESSAGE;
}
