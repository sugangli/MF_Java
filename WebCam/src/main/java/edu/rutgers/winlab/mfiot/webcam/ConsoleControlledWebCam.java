/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.webcam;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class ConsoleControlledWebCam implements Runnable {

    private final VideoCapturer videoCapturer;
    private final WebCamProperties properties = new WebCamProperties();

    public ConsoleControlledWebCam(String settingFileName) {
        final String mySettingFileName = settingFileName;
        properties.load(settingFileName);
        videoCapturer = new VideoCapturer(new File(properties.getVideoFolder()), properties.getNextGUID());
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    videoCapturer.stop();
                } catch (Exception ex) {
                    Logger.getLogger(ConsoleControlledWebCam.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("saving settings...");
                properties.setNextGUID(videoCapturer.getNextGUID());
                try {
                    properties.save(mySettingFileName);
                } catch (IOException ex) {
                    Logger.getLogger(ConsoleControlledWebCam.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            boolean stop = false;
            do {
                System.out.println("waiting to start...");
                if (parseLine(reader)) {
                    break;
                }
                videoCapturer.startGetImage();
                System.out.println("waiting to stop...");
                if (parseLine(reader)) {
                    stop = true;
                }
                System.out.println("stopping...");
                videoCapturer.stopGetImage();
                System.out.println("stopped...");
            } while (!stop);
            System.exit(0);
        } catch (Exception e) {

        }
    }

    private static boolean parseLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        return "s".equals(line);
    }
}
