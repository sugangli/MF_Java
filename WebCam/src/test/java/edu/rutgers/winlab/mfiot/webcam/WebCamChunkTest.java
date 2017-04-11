/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.webcam;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ubuntu
 */
public class WebCamChunkTest {

    public WebCamChunkTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testProcessImage() {
        int start = 262144;
        int count = 7, frames = 12;
        WebCamChunk c = new WebCamChunk(new GUID(start));
        for (int j = 0; j < count; j++) {
            for (int i = 0; i < frames; i++) {
                if (c == null) {
                    c = new WebCamChunk(new GUID(start));
                }
                int imgCount;
                try {
                    String fileName = String.format("welcome/welcome_%d.png", i + 1);
                    BufferedImage img = ImageIO.read(new File(fileName));
                    imgCount = c.addImage(img);
                } catch (IOException ex) {
                    Logger.getLogger(WebCamChunkTest.class.getName()).log(Level.SEVERE, null, ex);
                    fail("Cannot find file: " + i);
                    return;
                }
                if (imgCount >= Chunk.FRAMES_PER_FILE) {
                    if (j != count - 1 || i != frames - 1) {
                        c.nextGUID = new GUID(start + 1);
                    }
                    c.serialize();
                    try (FileOutputStream fos = new FileOutputStream("videos/" + start + ".obj")) {
                        c.save(fos);
                        fos.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(WebCamChunkTest.class.getName()).log(Level.SEVERE, null, ex);
                        fail("Cannot open target file.");
                    }
                    start++;
                    c = null;
                }
            }
        }
        if (c != null) {
            c.serialize();
            try (FileOutputStream fos = new FileOutputStream("videos/" + start + ".obj")) {
                c.save(fos);
                fos.flush();
            } catch (IOException ex) {
                Logger.getLogger(WebCamChunkTest.class.getName()).log(Level.SEVERE, null, ex);
                fail("Cannot open target file.");
            }
        }

    }
}
