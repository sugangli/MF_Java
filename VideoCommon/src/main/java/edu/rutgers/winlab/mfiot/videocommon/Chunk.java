/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videocommon;

import edu.rutgers.winlab.jmfapi.GUID;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import javax.imageio.ImageIO;


/**
 *
 * @author ubuntu
 */
public class Chunk {
        public static final String DEFAULT_FILE_FORMAT = "jpg";

    public static final int SLEEP_PER_FRAME = 50;
    public static final int DURATION_PER_FILE = 500;
    public static final int FRAMES_PER_FILE = DURATION_PER_FILE / SLEEP_PER_FRAME;
    public static final Dimension DEFAULT_VIDEO_SIZE = new Dimension(640, 480);

    public LinkedList<byte[]> images = new LinkedList<>();
    public GUID nextGUID;
    public final GUID guid;

    public Chunk(GUID guid) {
        this.guid = guid;
    }

    public int addImage(byte[] image) {
        images.add(image);
        return images.size();
    }

    public void save(OutputStream output) throws IOException {
        // 0-4: MyGUID (as int)
        // 4-8: NextGUID
        // 8-12: image len
        // 12-: image by image
        StreamHelper.writeInt(output, guid.getGUID());
        StreamHelper.writeInt(output, nextGUID == null ? 0 : nextGUID.getGUID());
        StreamHelper.writeInt(output, images.size());
        for (byte[] buf : images) {
            StreamHelper.writeByteArray(output, buf);
    }
        output.flush();
            }

    public static Chunk loadChunk(InputStream input) throws IOException {
        Chunk c = new Chunk(new GUID());
        c.guid.setGUID(StreamHelper.readInt(input));
        System.out.printf("load chunk: guid=%s%n", c.guid);
        int nextGUID = StreamHelper.readInt(input);
        c.nextGUID = nextGUID == 0 ? null : new GUID(nextGUID);
        System.out.printf("load chunk: next guid=%s%n", c.nextGUID);
        int len = StreamHelper.readInt(input);
        System.out.printf("load chunk: frames=%d%n", len);
        for (int i = 0; i < len; i++) {
            c.images.add(StreamHelper.readByteArray(input));
        }
        return c;
    }

    public static byte[] getBytesFromImage(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, DEFAULT_FILE_FORMAT, baos);
            baos.flush();
            return baos.toByteArray();
        }
    }

    public static BufferedImage getImageFromBytes(byte[] buf) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buf)) {
            return ImageIO.read(bais);
        }
    }

    public static File getFileNameFromGUID(File resultDirectory, GUID guid) {
        return new File(resultDirectory, String.format("%s.obj", guid));
    }

}
