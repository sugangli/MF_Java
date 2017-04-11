package edu.rutgers.winlab.mfiot.packets;

import edu.rutgers.winlab.mfiot.utilities.BitUtilities;

/**
 * MF-IoT packet header.
 *
 * @author Jiachen Chen
 */
public final class MFIoTPacket extends Packet {

    /**
     * The int type of MF IoT packet.
     */
    public static final int MFIoT_PACKET_INT_TYPE = 1;
    /**
     * The string type of MF IoT packet.
     */
    private static final String MFIoT_PACKET_TYPE = "NETTYPE_MF_LITE";
    /**
     * Total size of the packet header without src and dst.
     *
     * Use the function getHeaderSize to get the real size of the header.
     */
    public static final int PACKET_HEADER_FIXED_SIZE = 8;
    /**
     * Position of version and type in the packet header.
     */
    public static final int POSITION_VER_TYP = 0;
    /**
     * Position of SID and protocol in the packet header.
     */
    public static final int POSITION_SID_PORT = 1;
    /**
     * Position of TTL and length (top 4 bits) in the packet header.
     */
    public static final int POSITION_TTL_LEN_HIGH = 2;
    /**
     * Position of length (lower 8 bits) in the packet header.
     */
    public static final int POSITION_LEN_LOW = 3;
    /**
     * Position of higher 8bits nonce in the packet header.
     */
    public static final int POSITION_NONCE_HIGH = 4;
    /**
     * Position of lower 8 bits nonce in the packet header.
     */
    public static final int POSITION_NONCE_LOW = 5;
    /**
     * Position of source address length in the packet header.
     */
    public static final int POSITION_SRC_LEN = 6;
    /**
     * Position of destination address length in the packet header.
     */
    public static final int POSITION_DST_LEN = 7;

    /**
     * Generate an MF IoT packet using existing buffer, type, description and
     * next snip.
     *
     * @param buf the content of the packet header.
     * @param intType the int type of the packet header.
     * @param type the string type of the packet header.
     * @param description the description of the packet header.
     * @param next the next snip of the packet header.
     */
    public MFIoTPacket(byte[] buf, int intType, String type, String description, Packet next) {
        super(buf, intType, type, description, next);
    }

    /**
     * Create a shallow copy of the packet.
     *
     * Content and next are not copied. Just pointed to the same objects.
     *
     * @param p the packet to be copied.
     */
    public MFIoTPacket(Packet p) {
        super(p);
    }

    /**
     * Build a MF IoT packet from scratch.
     *
     * @param ver the version of the MF IoT packet.
     * @param sid the SID of the MF IoT packet.
     * @param prot the upper layer protocol of the MF IoT packet.
     * @param ttl the time to live of the MF IoT packet.
     * @param length the length of the MF IoT packet (unused).
     * @param src the source of the MF IoT packet.
     * @param dst the destination of the MF IoT packet.
     * @param nonce the nonce of the MF IoT packet.
     * @param next the next snip of the MF IoT packet.
     */
    public MFIoTPacket(int ver, int sid, int prot, int ttl, int length, byte[] src, byte[] dst, int nonce, Packet next) {
        super(new byte[PACKET_HEADER_FIXED_SIZE], MFIoT_PACKET_INT_TYPE, MFIoT_PACKET_TYPE, "", next);
        setIntType(MFIoT_PACKET_INT_TYPE);
        setSid(sid);
        setVersion(ver);
        setProt(prot);
        setTtl(ttl);
        setLength(length);
        setSrc(src);
        setDst(dst);
        setNonce(nonce);
    }

    /**
     * Get the nonce of the MF IoT packet.
     *
     * @return the nonce.
     */
    public int getNonce() {
        byte[] buf = getBuf();
        return ((buf[POSITION_NONCE_HIGH] << 8) & 0xFF) | ((buf[POSITION_NONCE_LOW]));
    }

    /**
     * Set the nonce of the MF IoT packet.
     *
     * @param nonce the new nonce.
     */
    public void setNonce(int nonce) {
        byte[] buf;
        buf = getBuf();
        buf[POSITION_NONCE_HIGH] = (byte) ((nonce >> 8) & 0xFF);
        buf[POSITION_NONCE_LOW] = (byte) (nonce & 0xFF);
    }

    /**
     * Get the length of the source address in the packet header.
     *
     * @return the length of the source address.
     */
    public int getSrcLength() {
        byte[] buf = getBuf();
        return (buf[POSITION_SRC_LEN] & 0xFF);
    }

    /**
     * Get the length of the destination address in the packet header.
     *
     * @return the length of the destination address.
     */
    public int getDstLength() {
        byte[] buf = getBuf();
        return (buf[POSITION_DST_LEN] & 0xFF);
    }

    // This function should not be called directly. Use setSrc instead.
    private void setSrcLength(int srcLength) {
        byte[] buf;
        buf = getBuf();
        buf[POSITION_SRC_LEN] = (byte) (srcLength & 0xFF);
    }

    // This function should not be called directly. Use setDst instead.
    private void setDstLength(int dstLength) {
        byte[] buf;
        buf = getBuf();
        buf[POSITION_DST_LEN] = (byte) (dstLength & 0xFF);
    }

    /**
     * Set the source address of the packet header.
     *
     * @param src the source address of the packet header.
     */
    public void setSrc(byte[] src) {
        byte[] buf = getBuf();
        int originalSrcLength = getSrcLength();
        if (src.length != originalSrcLength) {
            byte[] tmp = new byte[PACKET_HEADER_FIXED_SIZE + src.length + getDstLength()];
            System.arraycopy(buf, 0, tmp, 0, PACKET_HEADER_FIXED_SIZE);
            System.arraycopy(buf, PACKET_HEADER_FIXED_SIZE + originalSrcLength, tmp, PACKET_HEADER_FIXED_SIZE + src.length, getDstLength());
            System.arraycopy(src, 0, tmp, PACKET_HEADER_FIXED_SIZE, src.length);
            setBuf(tmp);
            setSrcLength(src.length);
        } else {
            System.arraycopy(src, 0, buf, PACKET_HEADER_FIXED_SIZE, src.length);
        }
    }

    /**
     * Set the destination address of the packet header.
     *
     * @param dst the destination address of the packet header.
     */
    public void setDst(byte[] dst) {
        byte[] buf = getBuf();
        int originalDstLength = getDstLength();
        if (dst.length != originalDstLength) {
            byte[] tmp = new byte[PACKET_HEADER_FIXED_SIZE + getSrcLength() + dst.length];
            System.arraycopy(buf, 0, tmp, 0, PACKET_HEADER_FIXED_SIZE);
            System.arraycopy(buf, PACKET_HEADER_FIXED_SIZE, tmp, PACKET_HEADER_FIXED_SIZE, getSrcLength());
            System.arraycopy(dst, 0, tmp, PACKET_HEADER_FIXED_SIZE + getSrcLength(), dst.length);
            setBuf(tmp);
            setDstLength(dst.length);
        } else {
            System.arraycopy(dst, 0, buf, PACKET_HEADER_FIXED_SIZE + getSrcLength(), dst.length);
        }
    }

    /**
     * Get the MF IoT version of the packet header.
     *
     * @return the MF IoT version of the packet header.
     */
    public int getVersion() {
        return BitUtilities.getUpper4Bits(getBuf(), POSITION_VER_TYP);
    }

    /**
     * Set the MF IoT version of the packet header.
     *
     * @param version the new MF IoT version.
     */
    public void setVersion(int version) {
        BitUtilities.setUpper4Bits(getBuf(), POSITION_VER_TYP, version);
    }

    @Override
    public void setIntType(int intType) {
        super.setIntType(intType);
        BitUtilities.setLower4Bits(getBuf(), POSITION_VER_TYP, intType);
    }

    /**
     * Get the source address of the packet header.
     *
     * @return the source address of the packet header.
     */
    public byte[] getSrc() {
        int srcLength = getSrcLength();
        byte[] buf = getBuf();
        byte[] tmp = new byte[srcLength];
        System.arraycopy(buf, PACKET_HEADER_FIXED_SIZE, tmp, 0, srcLength);
        return tmp;
    }

    /**
     * Get the destination address of the packet header.
     *
     * @return the destination address of the packet header.
     */
    public byte[] getDst() {
        int dstLength = getDstLength();
        byte[] buf = getBuf();
        byte[] tmp = new byte[dstLength];
        System.arraycopy(buf, PACKET_HEADER_FIXED_SIZE + getSrcLength(), tmp, 0, dstLength);
        return tmp;
    }

    /**
     * Set the SID of the packet header.
     *
     * @param sid the new SID.
     */
    public void setSid(int sid) {
        BitUtilities.setUpper4Bits(getBuf(), POSITION_SID_PORT, sid);
    }

    /**
     * Get the SID of the packet header.
     *
     * @return the SID of the packet header.
     */
    public int getSid() {
        return BitUtilities.getUpper4Bits(getBuf(), POSITION_SID_PORT);
    }

    /**
     * Set the upper layer protocol ID of the packet header.
     *
     * @param prot the new upper layer protocol ID of the packet header.
     */
    public void setProt(int prot) {
        BitUtilities.setLower4Bits(getBuf(), POSITION_SID_PORT, prot);
    }

    /**
     * Get the upper layer protocol ID of the packet header.
     *
     * @return the upper layer protocol ID of the packet header.
     */
    public int getProt() {
        return BitUtilities.getLower4Bits(getBuf(), POSITION_SID_PORT);
    }

    /**
     * Set the time to live value of the packet header.
     *
     * @param ttl the new time to live value of the packet header.
     */
    public void setTtl(int ttl) {
        BitUtilities.setUpper4Bits(getBuf(), POSITION_TTL_LEN_HIGH, ttl);
    }

    /**
     * Get the time to live value of the packet header.
     *
     * @return the time to live value of the packet header.
     */
    public int getTtl() {
        return BitUtilities.getUpper4Bits(getBuf(), POSITION_TTL_LEN_HIGH);
    }

    /**
     * Set the length of the payload of the packet header (unused).
     *
     * @param length the new length of the payload.
     */
    public void setLength(int length) {
        byte[] buf = getBuf();
        byte tmp = (byte) (buf[POSITION_TTL_LEN_HIGH] & 0xF0);
        buf[POSITION_TTL_LEN_HIGH] = (byte) (tmp | (length >> 8 & 0xF));
        buf[POSITION_LEN_LOW] = (byte) ((length & 0xFF));
    }

    /**
     * Get the length of the payload of the packet header (unused).
     *
     * @return the length of the payload of the packet header.
     */
    public int getLength() {
        byte[] buf = getBuf();
        return (buf[POSITION_TTL_LEN_HIGH] & 0xF) << 8 | (buf[POSITION_LEN_LOW] & 0xFF);
    }

    /**
     * Get the total size of the packet header (including the constant length
     * part and the source and destination address).
     *
     * @return the total size fo the packet header.
     */
    public int getHeaderSize() {
        return PACKET_HEADER_FIXED_SIZE + getSrcLength() + getDstLength();
    }

}
