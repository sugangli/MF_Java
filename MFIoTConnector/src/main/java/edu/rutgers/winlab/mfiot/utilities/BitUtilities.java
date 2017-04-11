package edu.rutgers.winlab.mfiot.utilities;

/**
 * Helper for set/get bits in integer values.
 *
 * @author ubuntu
 */
public class BitUtilities {

    /**
     * Set the upper 4 bits to a byte.
     *
     * @param buf the source.
     * @param position the position of the byte to be written.
     * @param value the upper 4 bits of buf[position].
     */
    public static void setUpper4Bits(byte[] buf, int position, int value) {
        buf[position] = (byte) ((buf[position] & 0xF) | ((value & 0xF) << 4));
    }

    /**
     * Set the lower 4 bits to a byte.
     *
     * @param buf the source.
     * @param position the position of the byte to be written.
     * @param value the lower 4 bits of buf[position].
     */
    public static void setLower4Bits(byte[] buf, int position, int value) {
        buf[position] = (byte) ((buf[position] & 0xF0) | ((value & 0xF)));
    }

    /**
     * Get the upper 4 bits from a byte.
     *
     * @param byt the byte to be read.
     * @return the upper 4 bits of the specified byte.
     */
    public static int getUpper4Bits(byte byt) {
        return (byt & 0xF0) >> 4;
    }

    /**
     * Get the upper 4 bits from a byte.
     *
     * @param buf the source.
     * @param position the location of the byte to be read.
     * @return the upper 4 bits of buf[position].
     */
    public static int getUpper4Bits(byte[] buf, int position) {
        return getUpper4Bits(buf[position]);
    }

    /**
     * Get the lower 4 bits from a byte.
     *
     * @param byt the byte to be read.
     * @return the lower 4 bits of the specified byte.
     */
    public static int getLower4Bits(byte byt) {
        return (byt & 0xF);
    }

    /**
     * Get the lower 4 bits from a byte.
     *
     * @param buf the source.
     * @param position the location of the byte to be read.
     * @return the lower 4 bits of buf[position].
     */
    public static int getLower4Bits(byte[] buf, int position) {
        return getLower4Bits(buf[position]);
    }

    /**
     * Converts a hex string to byte array.
     *
     * @param s the hex string
     * @return the byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        // make sure that the string is with even length.
        if (s.length() % 2 == 1) {
            s = "0" + s;
        }
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
