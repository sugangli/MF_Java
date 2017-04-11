package edu.rutgers.winlab.mfiot.packets;

import java.util.Arrays;

/**
 * A packet snip (header or payload) from RIOT.
 *
 * @author Jiachen Chen
 */
public class Packet {

    private int _intType;
    private String _type;
    private byte[] _buf;
    private String _description;
    private Packet _next;

    /**
     * Create a new packet instance with packet snip contents.
     *
     * @param buf the real content in the snip.
     * @param intType the int type of the packet snip.
     * @param type the String type of the packet snip.
     * @param description the description of the packet snip got from sniffer in
     * RIOT.
     * @param next the next packet snip component.
     */
    public Packet(byte[] buf, int intType, String type, String description, Packet next) {
        _buf = buf;
        _intType = intType;
        _type = type;
        _description = description;
        _next = next;
    }

    /**
     * Make a shallow copy of the original packet p.
     * 
     * Buffer and next are pointed to p's buffer. Not a deep copy.
     * 
     * @param p the original packet.
     */
    public Packet(Packet p) {
        _buf = p._buf;
        _intType = p._intType;
        _type = p._type;
        _description = p._description;
        _next = p._next;
    }

    /**
     * Get the real content of the snip.
     *
     * @return the real content of the snip.
     */
    public byte[] getBuf() {
        return _buf;
    }

    /**
     * Set the real content of the snip.
     *
     * @param buf the new content of the snip.
     */
    public void setBuf(byte[] buf) {
        _buf = buf;
    }

    /**
     * Get the description of the snip.
     *
     * @return the description of the snip.
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Set the description of the snip.
     *
     * @param _description the new description of the snip.
     */
    public void setDescription(String _description) {
        this._description = _description;
    }

    /**
     * Get the next snip.
     *
     * @return the next snip, NULL if there is no next snip.
     */
    public Packet getNext() {
        return _next;
    }

    /**
     * Set the next snip.
     *
     * @param _next the new next snip.
     */
    public void setNext(Packet _next) {
        this._next = _next;
    }

    /**
     * Get the int type of the packet.
     *
     * @return the int type of the packet.
     */
    public int getIntType() {
        return _intType;
    }

    /**
     * Set the int type of the packet.
     *
     * @param _intType the new int type of the packet.
     */
    public void setIntType(int _intType) {
        this._intType = _intType;
    }

    /**
     * Get the string type of the packet.
     *
     * @return the string type of the packet.
     */
    public String getType() {
        return _type;
    }

    /**
     * Set the string type of the packet
     *
     * @param _type the string type of the packet.
     */
    public void setType(String _type) {
        this._type = _type;
    }

    /**
     * Returns the string representation of a packet snip.
     * <br>
     * The returned string would be in the format:<br>
     *
     * Type: [Type] ([intType])<br>
     * Buf: [content]<br>
     * [description]<br>
     * [next_snip]<br>
     *
     * @return the string representation of the packet snip.
     */
    @Override
    public String toString() {
        return String.format("Type: %s (%d)%nBuf:%s%n%s%n%s", _type, _intType, Arrays.toString(_buf), _description, _next);
    }

}
