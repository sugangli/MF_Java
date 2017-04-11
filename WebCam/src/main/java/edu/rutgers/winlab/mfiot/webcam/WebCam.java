/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.webcam;

import edu.rutgers.winlab.jmfapi.JMFException;
import java.io.IOException;

/**
 *
 * @author ubuntu
 */
public class WebCam {

    public static final String DEFAULT_SETTINGS_FILE = "webcam.properties";

    public static void main(String[] args) throws IOException, JMFException {
        String settingsFile = DEFAULT_SETTINGS_FILE;
        if (args.length == 0) {
            System.out.printf("Using default settings file: %s%n", settingsFile);
        } else {
            settingsFile = args[0];
        }
//        ConsoleControlledWebCam webcam = new ConsoleControlledWebCam(settingsFile);
        SensorControlledWebCam webcam = new SensorControlledWebCam(settingsFile);
        webcam.run();

    }
}
