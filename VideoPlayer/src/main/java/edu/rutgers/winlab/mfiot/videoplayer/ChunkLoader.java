/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videoplayer;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;

/**
 *
 * @author ubuntu
 */
public interface ChunkLoader {

    public static interface ChunkLoadedHandler {

        public void chunkLoaded(Chunk chunk);
    }

    public void loadChunk(GUID guid, ChunkLoadedHandler callback);
}
