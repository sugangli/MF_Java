package edu.rutgers.winlab.mfiot.mfiotconnector;

import edu.rutgers.winlab.mfiot.packets.Packet;

/**
 * A packet handler.
 *
 * @author Jiachen Chen
 */
public interface IPacketListener {

    /**
     * Handles a packet.
     *
     * @param type the type of the message (sent/received).
     * @param p the packet.
     */
    public void handlePacket(PacketMessageType type, Packet p);
}
