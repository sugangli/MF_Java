/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videoplayer;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import edu.rutgers.winlab.mfiot.videoplayer.ChunkLoader.ChunkLoadedHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class FileChunkLoader implements ChunkLoader {

    private final File folder;
    private final HashMap<GUID, LinkedList<ChunkLoadedHandler>> handlers = new HashMap<>();
    private final Timer loadFileTimer = new Timer("FileLoader", true);

    private class TimerTaskLoadFile extends TimerTask {

        @Override
        public void run() {
            HashMap<GUID, LinkedList<ChunkLoadedHandler>> tmp;
            synchronized (handlers) {
                tmp = new HashMap<>(handlers);
                handlers.clear();
            }
            for (Map.Entry<GUID, LinkedList<ChunkLoadedHandler>> tasks : tmp.entrySet()) {
                GUID guid = tasks.getKey();
                LinkedList<ChunkLoadedHandler> callbacks = tasks.getValue();
                Chunk c;
                try (FileInputStream fis = new FileInputStream(Chunk.getFileNameFromGUID(folder, guid))) {
                    c = Chunk.loadChunk(fis);
                } catch (IOException ex) {
                    Logger.getLogger(FileChunkLoader.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }
                System.out.printf("Load chunk GUID: %s%n", c.guid);
                for (ChunkLoadedHandler handler : callbacks) {
                    handler.chunkLoaded(c);
                }

            }
        }
    }

    public FileChunkLoader(String folder) {
        this.folder = new File(folder);
        loadFileTimer.scheduleAtFixedRate(new TimerTaskLoadFile(), 0, 100);
    }

    public String getFolder() {
        return folder.getAbsolutePath();
    }

    @Override
    public void loadChunk(GUID guid, ChunkLoadedHandler callback) {
        synchronized (handlers) {
            LinkedList<ChunkLoadedHandler> existingHandlers = handlers.get(guid);
            if (existingHandlers == null) {
                handlers.put(guid, existingHandlers = new LinkedList<>());
            }
            existingHandlers.addLast(callback);
        }
    }
}
