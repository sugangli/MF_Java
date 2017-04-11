package edu.rutgers.winlab.mfiot.webcam;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ubuntu
 */
public class WebCamChunk extends Chunk {

    private final LinkedList<BufferedImage> myImages = new LinkedList<>();

    public WebCamChunk(GUID guid) {
        super(guid);
        if (guid == null) {
            System.out.println("Got chunk GUID == null");
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                System.out.printf("\t%s%n", e);
            }
        }
    }

    public int addImage(BufferedImage image) {
        myImages.add(image);
        return myImages.size();
    }

    public void serialize() {
        images.clear();
        for (BufferedImage bi : myImages) {
            while (true) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(bi, DEFAULT_FILE_FORMAT, baos);
                    images.add(baos.toByteArray());
                    break;
                } catch (IOException | RuntimeException ex) {
                    Logger.getLogger(WebCamChunk.class.getName()).log(Level.SEVERE, String.format("GUID:%s, images:%d", this.guid.toString(), myImages.size()), ex);
                }
            }
        }
    }

    @Override
    public void save(OutputStream output) throws IOException {
        serialize();
        super.save(output);
    }

}
