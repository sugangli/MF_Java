/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.webcam;

import com.github.sarxos.webcam.Webcam;
import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import static edu.rutgers.winlab.mfiot.videocommon.Chunk.DEFAULT_VIDEO_SIZE;
import static edu.rutgers.winlab.mfiot.videocommon.Chunk.FRAMES_PER_FILE;
import edu.rutgers.winlab.mfiot.videocommon.IGUIDHandler;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class VideoCapturer {
    private static final WebCamChunk CLOSING_CHUNK = new WebCamChunk(new GUID());

    private final File resultDirectory;
    private GUID nextGUID;
    private final Webcam cam = Webcam.getDefault();
    private final LinkedBlockingQueue<Chunk> pendingSavings = new LinkedBlockingQueue<>();
    private final Timer timerCameraRetriever = new Timer("CameraRetriever", true);
    private WebCamChunk currentChunk;
    private final LinkedList<IGUIDHandler> chunkSavedHandlers = new LinkedList<>();
            
    
    private final Thread threadChunkSaver = new Thread("ChunkSaver") {

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            while (true) {
                try {
                    Chunk toSave = pendingSavings.take();
                    if (toSave == CLOSING_CHUNK) {
                        System.out.println("Chunk saver exitting...");
                        break;
                    }
                    try (FileOutputStream fos = new FileOutputStream(Chunk.getFileNameFromGUID(resultDirectory, toSave.guid))) {
                        toSave.save(fos);
                        System.out.printf("[%d] Save: %s, frame count: %d, next: %s%n", System.currentTimeMillis(), toSave.guid, toSave.images.size(), toSave.nextGUID);
                        fireChunkSavedEvent(toSave.guid);
                    } catch (IOException ex) {
                        Logger.getLogger(VideoCapturer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(VideoCapturer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    };

    //    private long lastTime = 0;
    private final TimerTask timerTaskCaptureImage = new TimerTask() {

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//            long currentTime = System.currentTimeMillis();
//            System.out.println(currentTime - lastTime);
//            lastTime = currentTime;
            synchronized (VideoCapturer.this) {
                if (currentChunk == null) {
                    return;
                }
                captureImage();
            }
        }
    };

    private void captureImage() {
        BufferedImage currentImage = cam.getImage();
        int count = currentChunk.addImage(currentImage);
        if (count >= FRAMES_PER_FILE) {
            currentChunk.nextGUID = nextGUID;
            pendingSavings.offer(currentChunk);
            GUID currentGUID = nextGUID;
            getAndAdvanceNextGUID();
            currentChunk = new WebCamChunk(currentGUID);
        }
    }

    public File getResultDirectory() {
        return resultDirectory;
    }

    public boolean isRunning() {
        return currentChunk != null;
    }

    public GUID getNextGUID() {
        return nextGUID;
    }

    public VideoCapturer(File resultDirectory, GUID nextGUID) {
        this.nextGUID = nextGUID;
        this.resultDirectory = resultDirectory;
        System.out.printf("nextGUID: %s, folder: %s%n", this.nextGUID, this.resultDirectory.getAbsolutePath());

        cam.setViewSize(DEFAULT_VIDEO_SIZE);
        cam.open(true);
        cam.getImage();

        timerCameraRetriever.scheduleAtFixedRate(timerTaskCaptureImage, 0, Chunk.SLEEP_PER_FRAME);
        _startChunkSaver();
        System.out.println("Camera ready");
    }
    
    public void addChunkSavedHandler(IGUIDHandler handler) {
        synchronized(chunkSavedHandlers) {
            chunkSavedHandlers.add(handler);
        }
    }
    
    public void removeChunkSavedHandler(IGUIDHandler handler) {
        synchronized(chunkSavedHandlers) {
            chunkSavedHandlers.remove(handler);
        }
    }
    
    private void fireChunkSavedEvent(GUID guid) {
        synchronized(chunkSavedHandlers) {
            for (IGUIDHandler h : chunkSavedHandlers) {
                h.handleGUID(guid);
            }
        }
    }

    private void _startChunkSaver() {
        threadChunkSaver.start();
    }

    private GUID getAndAdvanceNextGUID() {
        nextGUID = new GUID(nextGUID.getGUID() + 1);
        return nextGUID;
    }

    public GUID startGetImage() {
        synchronized (this) {
            if (currentChunk != null) {
                return null;
            }
            GUID currentGUID = nextGUID;
            currentChunk = new WebCamChunk(currentGUID);
            getAndAdvanceNextGUID();
            captureImage();
            return currentGUID;
        }
    }

    public void stopGetImage() {
        synchronized (this) {
            if (currentChunk != null) {
                pendingSavings.offer(currentChunk);
                currentChunk = null;
            }
        }
    }

    public void stop() {
        timerCameraRetriever.cancel();
        stopGetImage();
        pendingSavings.offer(CLOSING_CHUNK);
        try {
            threadChunkSaver.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(VideoCapturer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
