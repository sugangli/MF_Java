/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.webcam;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.jmfapi.JMFAPI;
import edu.rutgers.winlab.jmfapi.JMFException;
import edu.rutgers.winlab.mfiot.videocommon.Chunk;
import edu.rutgers.winlab.mfiot.videocommon.Constants;
import edu.rutgers.winlab.mfiot.videocommon.IGUIDHandler;
import edu.rutgers.winlab.mfiot.videocommon.StreamHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class SensorControlledWebCam implements Runnable {

//    private static final int MY_GUID = 0x10102;
    private static final int MAX_MSG_SIZE = 10 * 1024 * 1024;
    private static final long RECORDING_TIMEOUT = 3000; // 3 seconds

    private final VideoCapturer videoCapturer;
    private final JMFAPI handle;
    private final LinkedBlockingDeque<GUID> savedChunks = new LinkedBlockingDeque<>();
    private final HashMap<GUID, LinkedList<GUID>> pendingRequests = new HashMap<>();
    private final WebCamProperties properties = new WebCamProperties();
    private final GUID myGUID;
//    private final Timer timerRecordingTimeout = new Timer("RecordingTimeout");
    private final ScheduledExecutorService recordingTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    private long recordingTimeout = 0;
    private final GUID[] clients;
    private final String settingFileName;

    public SensorControlledWebCam(String settingFileName) throws JMFException {
        this.settingFileName = settingFileName;
        properties.load(settingFileName);
        clients = properties.getClients();
        myGUID = properties.getMyGUID();

        videoCapturer = new VideoCapturer(new File(properties.getVideoFolder()), properties.getNextGUID());
        videoCapturer.addChunkSavedHandler(new IGUIDHandler() {
            @Override
            public void handleGUID(GUID guid) {
                savedChunks.offer(guid);
            }
        });

        handle = new JMFAPI();
        handle.jmfopen("basic", myGUID);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                SensorControlledWebCam.this.stop();
            }
        });
    }

    @Override
    public void run() {
        threadSendSavedChunks.start();
        byte[] data = new byte[MAX_MSG_SIZE];
        while (true) {
            try {
                GUID sourceGUID = new GUID();
                int val = handle.jmfrecv_blk(sourceGUID, data, MAX_MSG_SIZE);
                if (val <= 0) {
                    System.out.println("Cannot get data from Hoststack.");
                    continue;
                }
                switch (data[0]) {
                    case Constants.TYPE_SENSOR_NOTIFICATION:
                        handleNotification(sourceGUID, data, val);
                        break;
                    case Constants.TYPE_CONTENT_REQUEST:
                        handleRequest(sourceGUID, data, val);
                        break;
                    default:
                        System.out.printf("Cannot parse packet with type: %d%n", data[0]);
                        break;
                }

            } catch (JMFException ex) {
                Logger.getLogger(SensorControlledWebCam.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void handleNotification(GUID sourceGUID, byte[] data, int length) {
        if (data.length < 2) {
            System.out.printf("Error reading content request data from %s, len: %d%n", sourceGUID, length);
            return;
        }

        boolean motion = data[1] != 0;
        System.out.printf("Received sensor notification from %s, motion=%s%n", sourceGUID, motion ? "TRUE" : "FALSE");
        if (motion) {
            synchronized (recordingTimeoutExecutor) {
                recordingTimeout = System.currentTimeMillis() + RECORDING_TIMEOUT;
            }
            GUID imageGUID = videoCapturer.startGetImage();
            // triggers a video recording. need to send a msg
            if (imageGUID != null) {
                System.out.printf("Start capture GUID=%s%n", imageGUID);
                recordingTimeoutExecutor.schedule(recordingTimeoutTimerTask, recordingTimeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                ByteBuffer bb = ByteBuffer.allocate(1 + Integer.SIZE);
                bb.put(Constants.TYPE_CAMERA_NOTIFICATION);
                bb.putInt(1, imageGUID.getGUID());
                byte[] toSend = bb.array();
                // send a message to notify the clients
                for (GUID client : clients) {
                    System.out.printf("Send camera notification to=%s for content %s%n", client, imageGUID);
                    try {
                        handle.jmfsend(toSend, toSend.length, client);
                    } catch (JMFException ex) {
                        String errorMsg = String.format("Error sending notification: imageGUID=%s, dst=%s", imageGUID, client);
                        Logger.getLogger(SensorControlledWebCam.class.getName()).log(Level.SEVERE, errorMsg, ex);
                    }
                }
            }
        }
//        Soft state, ignore stop notifications
//        else {
//            videoCapturer.stopGetImage();
//        }

    }

    private final Runnable recordingTimeoutTimerTask = new Runnable() {
        @Override
        public void run() {
            synchronized (recordingTimeoutExecutor) {
                if (System.currentTimeMillis() < recordingTimeout) {
                    recordingTimeoutExecutor.schedule(this, recordingTimeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    return;
                }
                videoCapturer.stopGetImage();
            }
        }
    };

    private byte[] getContent(GUID guid) {
        File targetFile = Chunk.getFileNameFromGUID(videoCapturer.getResultDirectory(), guid);

        Chunk c;
        try (FileInputStream fis = new FileInputStream(targetFile)) {
            c = Chunk.loadChunk(fis);
        } catch (IOException ex) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            baos.write(Constants.TYPE_CONTENT_RESPONSE);
            c.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            return null;
        }
    }

    private void handleRequest(GUID sourceGUID, byte[] data, int length) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data, 1, length - 1)) {
            byte[] response;
            LinkedList<GUID> prsForContent;
            GUID contentGUID = new GUID(StreamHelper.readInt(bais));
            System.out.printf("Received content request from %s, contentGUID=%s%n", sourceGUID, contentGUID);
            synchronized (pendingRequests) {
                prsForContent = pendingRequests.get(contentGUID);
                if (prsForContent != null) { // someone is already pending, add to the pending list
                    prsForContent.add(sourceGUID);
                    return;
                }
                response = getContent(contentGUID);
                if (response == null) { // the content is not there, add to pending requests
                    pendingRequests.put(contentGUID, prsForContent = new LinkedList<>());
                    prsForContent.add(sourceGUID);
                    return;
                }
            }
            //content found
            try {
                System.out.printf("I: Send content %s to %s%n", contentGUID, sourceGUID);
                handle.jmfsend(response, response.length, sourceGUID);
            } catch (JMFException ex) {
                String errorMsg = String.format("Error sending data: contentGUID=%s, dst=%s", contentGUID, sourceGUID);
                Logger.getLogger(WebCam.class.getName()).log(Level.SEVERE, errorMsg, ex);
            }
        } catch (Exception e) {
            System.out.printf("Error reading content request data from %s, len: %d%n", sourceGUID, length);
            e.printStackTrace(System.out);
        }
    }
    private static final GUID CLOSING_GUID = new GUID();

    private final Thread threadSendSavedChunks = new Thread() {
        @Override
        public void run() {
            LinkedList<GUID> prsForGUID;
            byte[] response;
            while (true) {
                try {
                    GUID contentGUID = savedChunks.take();
                    synchronized (pendingRequests) {
                        prsForGUID = pendingRequests.remove(contentGUID);
                        if (prsForGUID == null) {
                            continue;
                        }
                        response = getContent(contentGUID);
                        if (response == null) {
                            throw new IllegalStateException(String.format("Should not reach here. Cannot get content %s but it has just been saved...", contentGUID));
                        }
                    }
                    for (GUID requesterGUID : prsForGUID) {
                        try {
                            System.out.printf("P: Send content %s to %s%n", contentGUID, requesterGUID);
                            handle.jmfsend(response, response.length, requesterGUID);
                        } catch (JMFException ex) {
                            String errorMsg = String.format("Error sending data: contentGUID=%s, dst=%s", contentGUID, requesterGUID);
                            Logger.getLogger(WebCam.class.getName()).log(Level.SEVERE, errorMsg, ex);
                        }
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(SensorControlledWebCam.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    };

    private void stop() {
        videoCapturer.stop();
        recordingTimeoutExecutor.shutdownNow();
        System.out.println("saving settings...");
        properties.setNextGUID(videoCapturer.getNextGUID());
        try {
            properties.save(settingFileName);
        } catch (IOException ex) {
            Logger.getLogger(ConsoleControlledWebCam.class.getName()).log(Level.SEVERE, null, ex);
        }
        savedChunks.offer(CLOSING_GUID);
        try {
            threadSendSavedChunks.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(SensorControlledWebCam.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    private class TimerTaskRespondLogic extends TimerTask {
//
//        @Override
//        public void run() {
//            HashMap<GUID, LinkedList<GUID>> tmpPendingRequests;
//            // get the current pending requests from the request list and then release the pending requests
//            synchronized (pendingRequests) {
//                tmpPendingRequests = new HashMap<>(pendingRequests);
//                pendingRequests.clear();
//            }
//
//            // iterate through the pending requests and see which ones can be satisfied
//            LinkedList<GUID> finishedGUIDs = new LinkedList<>();
//            for (Map.Entry<GUID, LinkedList<GUID>> entrySet : tmpPendingRequests.entrySet()) {
//                GUID contentGUID = entrySet.getKey();
//                byte[] buf = getContent(contentGUID);
//                if (buf != null) {
//                    for (GUID dst : entrySet.getValue()) {
//                        try {
//                            handle.jmfsend(buf, buf.length, dst);
//                            System.out.printf("Send content %s to %s%n", contentGUID, dst);
//                        } catch (JMFException ex) {
//                            String errorMsg = String.format("Error sending data: contentGUID=%s, dst=%s", contentGUID, dst);
//                            Logger.getLogger(WebCam.class.getName()).log(Level.SEVERE, errorMsg, ex);
//                        }
//                    }
//                    finishedGUIDs.add(contentGUID);
//                }
//            }
//            for (GUID guid : finishedGUIDs) {
//                tmpPendingRequests.remove(guid);
//            }
//
//            // add the unsatisfied requests back to the pending request table
//            synchronized (pendingRequests) {
//                for (Map.Entry<GUID, LinkedList<GUID>> entrySet : tmpPendingRequests.entrySet()) {
//                    GUID guid = entrySet.getKey();
//                    LinkedList<GUID> oldPending = entrySet.getValue();
//                    LinkedList<GUID> currentPending;
//                    currentPending = pendingRequests.get(guid);
//                    if (currentPending == null) {
//                        pendingRequests.put(guid, oldPending);
//                    } else {
//                        currentPending.addAll(oldPending);
//                    }
//                }
//            }
//        }
//    }
}
