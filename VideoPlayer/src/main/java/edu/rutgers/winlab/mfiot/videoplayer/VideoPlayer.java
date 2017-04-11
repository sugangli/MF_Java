/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.videoplayer;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.jmfapi.JMFException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author ubuntu
 */
public class VideoPlayer {

    public static void main(String[] args) throws IOException, JMFException {
        startMFVideoPlayer(args);
//        startLocalVideoPlayer(args);
    }

    public static void startMFVideoPlayer(String[] args) throws JMFException {
        int myGUID, serverGUID;
        try {
            myGUID = Integer.parseInt(args[0]);
            serverGUID = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("usage: java -jar VideoPlayer.jar %myGUID% %serverGUID%");
            return;
        }
        MFChunkLoader fcl = new MFChunkLoader(new GUID(serverGUID), new GUID(myGUID));
        final VideoPlayerFrame lvp = new VideoPlayerFrame(fcl);

        fcl.addNewChunkNotifiedHandler(new MFChunkLoader.NewChunkNotifiedHandler() {

            @Override
            public void newChunkNotified(GUID chunkGUID) {
                lvp.addGUID(chunkGUID);
            }
        });

        lvp.setVisible(true);

    }

    public static void startLocalVideoPlayer(String[] args) throws IOException {
        String folder;
        try {
            folder = args[0];
        } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
            System.out.println("usage: java -jar VideoPlayer.jar %folder%");
            return;
        }
//        ChunkLoader fcl = new FileChunkLoader(folder);
        ChunkLoader fcl = new FileWaitChunkLoader(folder);

        VideoPlayerFrame lvp = new VideoPlayerFrame(fcl);
        lvp.setVisible(true);
        try (InputStreamReader isr = new InputStreamReader(System.in)) {
            try (BufferedReader br = new BufferedReader(isr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    try {
                        int val = Integer.parseInt(line);
                        GUID toAdd = new GUID(val);
                        lvp.addGUID(toAdd);
                        System.out.printf("Added GUID: %s%n", toAdd);
                    } catch (Exception e) {
                        System.out.println("Not added");
                    }
                }
            }
        }

    }
}
