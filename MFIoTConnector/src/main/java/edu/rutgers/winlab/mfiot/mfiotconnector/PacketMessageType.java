package edu.rutgers.winlab.mfiot.mfiotconnector;

/**
 * Types of packet messages.
 *
 * @author Jiachen Chen
 */
public enum PacketMessageType {

    /**
     * The message is sent by the node.
     */
    SEND,
    /**
     * The message is received by the node.
     */
    RECEIVE;
}
