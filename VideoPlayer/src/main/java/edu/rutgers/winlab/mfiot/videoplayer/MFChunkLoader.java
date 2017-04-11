/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videoplayer;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.jmfapi.JMFAPI;
import edu.rutgers.winlab.jmfapi.JMFException;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import edu.rutgers.winlab.mfiot.videocommon.Constants;
import edu.rutgers.winlab.mfiot.videoplayer.ChunkLoader.ChunkLoadedHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class MFChunkLoader implements ChunkLoader {

    public static interface NewChunkNotifiedHandler {

        public void newChunkNotified(GUID chunkGUID);
    }

    public static final int MAX_MSG_SIZE = 10 * 1024 * 1024;

    private final JMFAPI handle;
    private final GUID serverGUID;
    private final HashMap<GUID, LinkedList<ChunkLoadedHandler>> handlers = new HashMap<>();
    private final LinkedList<NewChunkNotifiedHandler> notifyHandlers = new LinkedList<>();

    public void addNewChunkNotifiedHandler(NewChunkNotifiedHandler h) {
        notifyHandlers.add(h);
    }

    public void removeNewChunkNotifiedHandler(NewChunkNotifiedHandler h) {
        notifyHandlers.remove(h);
    }

    private class MFListenThread extends TimerTask {

        @Override
        public void run() {
            byte[] data = new byte[MAX_MSG_SIZE];
            GUID srcGUID = new GUID();
            while (true) {
                try {
                    int ret = handle.jmfrecv_blk(srcGUID, data, MAX_MSG_SIZE);
                    System.out.printf("Received %d bytes from %s%n", ret, srcGUID);
                    if (!srcGUID.equals(serverGUID)) {
                        continue;
                    }
                    switch (data[0]) {
                        case Constants.TYPE_CAMERA_NOTIFICATION:
                            ByteBuffer bb = ByteBuffer.wrap(data, 1, ret - 1);
                            GUID contentGUID = new GUID(bb.getInt());
                            System.out.printf("Got notification for GUID: %s%n", contentGUID);
                            for (NewChunkNotifiedHandler h : notifyHandlers) {
                                h.newChunkNotified(contentGUID);
                            }
                            break;
                        case Constants.TYPE_CONTENT_RESPONSE:
                            try (ByteArrayInputStream bais = new ByteArrayInputStream(data, 1, ret - 1)) {
                                Chunk c = Chunk.loadChunk(bais);
                                System.out.printf("Got chunk for GUID: %s%n", c.guid);
                                LinkedList<ChunkLoadedHandler> hs;
                                synchronized (handlers) {
                                    hs = handlers.remove(c.guid);
                                }
                                if (hs != null) {
                                  for (ChunkLoadedHandler h : hs) {
                                      h.chunkLoaded(c);
                                  }
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(MFChunkLoader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;

                    }

                } catch (JMFException ex) {
                    Logger.getLogger(MFChunkLoader.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }
    }

    public MFChunkLoader(GUID serverGUID, GUID myGUID) throws JMFException {
        handle = new JMFAPI();
        this.serverGUID = serverGUID;
        handle.jmfopen("basic", myGUID);
        Timer t = new Timer();
        t.schedule(new MFListenThread(), 0);
    }

    @Override
    public void loadChunk(GUID guid, ChunkLoadedHandler callback) {
        synchronized (handlers) {
            LinkedList<ChunkLoadedHandler> existingHandlers = handlers.get(guid);
            if (existingHandlers == null) {
                handlers.put(guid, existingHandlers = new LinkedList<>());
                existingHandlers.addLast(callback);
                ByteBuffer buffer = ByteBuffer.allocate(1 + Integer.SIZE);
                buffer.put(Constants.TYPE_CONTENT_REQUEST);
                buffer.putInt(1, guid.getGUID());
                byte[] buf = buffer.array();
                try {
                    handle.jmfsend(buf, buf.length, serverGUID);
                    System.out.printf("request for GUID %s sent%n", guid);
                } catch (JMFException ex) {
                    Logger.getLogger(MFChunkLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                existingHandlers.addLast(callback);
            }
        }
    }

}
