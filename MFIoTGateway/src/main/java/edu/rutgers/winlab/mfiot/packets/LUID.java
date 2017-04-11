/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.packets;

/**
 *
 * @author ubuntu
 */
public class LUID {

    public static final int POSITION_LUID_HIGH = 0;
    public static final int POSITION_LUID_LOW = 1;
    public static final int LUID_SIZE = 2;

    private final byte[] buf;

    public LUID(byte[] buf) {
        assert buf.length == LUID_SIZE;
        this.buf = buf;
    }

    public byte[] getBuf() {
        return buf;
    }

    public static LUID fromValue(int val) {
        LUID luid = new LUID(new byte[2]);
        luid.setValue(val);
        return luid;
    }

    public void setValue(int val) {
        buf[POSITION_LUID_LOW] = (byte) (val & 0xFF);
        buf[POSITION_LUID_HIGH] = (byte) ((val >> 8) & 0xFF);
    }

    public int getValue() {
        return ((buf[POSITION_LUID_HIGH] & 0xFF) << 8) | (buf[POSITION_LUID_LOW] & 0xFF);
    }

    @Override
    public String toString() {
        return Integer.toString(getValue());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + buf[POSITION_LUID_HIGH];
        hash = 31 * hash + buf[POSITION_LUID_LOW];
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof LUID) {
            LUID luid = (LUID) obj;
            return luid.buf[POSITION_LUID_HIGH] == buf[POSITION_LUID_HIGH]
                    && luid.buf[POSITION_LUID_LOW] == buf[POSITION_LUID_LOW];
        }
        return false;
    }

}
