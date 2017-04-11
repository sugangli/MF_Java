/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videocommon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 *
 * @author ubuntu
 */
public class StreamHelper {

    public static void writeInt(OutputStream output, int value) throws IOException {
        byte[] buf = ByteBuffer.allocate(Integer.SIZE).putInt(value).array();
        output.write(buf);
    }

    public static void writeByteArray(OutputStream output, byte[] buf) throws IOException {
        writeByteArray(output, buf, 0, buf.length);
    }

    public static void writeByteArray(OutputStream output, byte[] buf, int len) throws IOException {
        writeByteArray(output, buf, 0, len);
    }

    public static void writeByteArray(OutputStream output, byte[] buf, int start, int len) throws IOException, IllegalArgumentException {
        if (start < 0 || start > buf.length || start + len > buf.length || len < 0) {
            String msg = String.format("Error in WriteBuffer: start=%d, len=%d, buf size=%d", start, len, buf.length);
            throw new IllegalArgumentException(msg);
        }
        writeInt(output, len);
        output.write(buf, start, len);
    }

    public static void robustReadByteArray(InputStream input, byte[] buf, int start, int len) throws IOException {
        int remaining = len, read;
        while (remaining > 0) {
            read = input.read(buf, start, remaining);
            if (read < 0) {
                throw new IOException("Not enough buffer: remaining=" + remaining);
            }
            start += read;
            remaining -= read;
        }
    }

    public static int readInt(InputStream input) throws IOException {
        byte[] buf = new byte[Integer.SIZE];
        robustReadByteArray(input, buf, 0, buf.length);
        return ByteBuffer.wrap(buf).getInt();
    }

    public static byte[] readByteArray(InputStream input) throws IOException {
        int len = readInt(input);
//        System.out.printf("byte array size=%d%n", len);
        byte[] ret = new byte[len];
        robustReadByteArray(input, ret, 0, len);
        return ret;
    }
}
