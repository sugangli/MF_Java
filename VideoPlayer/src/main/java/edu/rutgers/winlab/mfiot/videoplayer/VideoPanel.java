/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videoplayer;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author ubuntu
 */
public class VideoPanel extends JPanel implements ChunkLoader.ChunkLoadedHandler {

    private final ChunkLoader chunkLoader;
//    private long lastValue = 0;

    public VideoPanel(ChunkLoader chunkLoader) {
        super(new FlowLayout(FlowLayout.TRAILING));
        this.setPreferredSize(Chunk.DEFAULT_VIDEO_SIZE);
        this.chunkLoader = chunkLoader;
        this.setDoubleBuffered(true);
        Timer t = new Timer(Chunk.SLEEP_PER_FRAME, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                currentFrameCount = Math.min(currentFrameCount + 1, frames.size());
//                long now = System.currentTimeMillis();
//                System.out.println(now - lastValue);
//                lastValue = now;
                updateUI();
            }
        });
        t.start();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        String str;
        if (currentFrameCount < frames.size()) {
            drawFrame(g, frames.get(currentFrameCount));
            str = (currentFrameCount + 1) + "/" + frames.size();
        } else if (frames.size() > 0) {
            drawFrame(g, frames.get(frames.size() - 1));
            str = frames.size() + "/" + frames.size();
        } else {
            str = "0/0";
        }
        g.setColor(Color.WHITE);
        g.drawString(str, 5 - 1, g.getFontMetrics().getHeight());
        g.drawString(str, 5 + 1, g.getFontMetrics().getHeight());
        g.drawString(str, 5, g.getFontMetrics().getHeight() - 1);
        g.drawString(str, 5, g.getFontMetrics().getHeight() + 1);
        g.setColor(Color.BLACK);
        g.drawString(str, 5, g.getFontMetrics().getHeight());
    }

    private void drawFrame(Graphics g, byte[] image) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(image)) {
            g.drawImage(ImageIO.read(bais), 0, 0, null);
        } catch (IOException ex) {
            Logger.getLogger(VideoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void loadVideo(GUID guid) {
        currentFrameCount = 0;
        frames = new ArrayList<>();
        synchronized (this) {
            loadingGUID = guid;
            chunkLoader.loadChunk(guid, this);
        }
    }

    @Override
    public void chunkLoaded(Chunk chunk) {
        synchronized (this) {
            if (!chunk.guid.equals(loadingGUID)) {
                return;
            }
            loadingGUID = chunk.nextGUID;
            if (loadingGUID != null) {
                chunkLoader.loadChunk(loadingGUID, this);
            }
        }
        for (byte[] buf : chunk.images) {
            frames.add(buf);
        }
    }

    private GUID loadingGUID;
    private int currentFrameCount = 0;
    private ArrayList<byte[]> frames = new ArrayList<>();

    public void rewind() {
        currentFrameCount = 0;
    }
}
