/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.webcam;

import edu.rutgers.winlab.jmfapi.GUID;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author ubuntu
 */
public class WebCamProperties {

    private static final String DEFAULT_START_GUID = "262144"; // 0x40000
    public static final String SETTING_NEXT_GUID = "nextGUID";
    public static final String DEFAULT_RESULT_FOLDER = "videos";
    public static final String SETTING_RESULT_FOLDER = "resultFolder";
    public static final String SETTING_CLIENTS = "clients";

    private final Properties settings = new Properties();

    public WebCamProperties() {
    }

    public void load(String fileName) {
        try (FileInputStream fis = new FileInputStream(fileName)) {
            settings.load(fis);
        } catch (IOException ex) {
            System.out.printf("Error in reading properties file: %s%n", fileName);
            ex.printStackTrace(System.out);
        }
        System.out.printf("NextGUID: %s%n", getNextGUID());
        System.out.printf("VideoFolder: %s%n", getVideoFolder());
        System.out.printf("Clients: %s%n", Arrays.toString(getClients()));
    }

    public GUID getNextGUID() {
        try {
            String strNextGUID = settings.getProperty(SETTING_NEXT_GUID, DEFAULT_START_GUID);
            return new GUID(Integer.parseInt(strNextGUID));
        } catch (Exception ex) {
            System.out.printf("Error in getting next GUID, set to default: %s%n", DEFAULT_START_GUID);
            ex.printStackTrace(System.out);
            settings.setProperty(SETTING_NEXT_GUID, DEFAULT_START_GUID);
            return new GUID(Integer.parseInt(DEFAULT_START_GUID));
        }
    }

    public void setNextGUID(GUID nextGUID) {
        System.out.println(nextGUID);
        settings.setProperty(SETTING_NEXT_GUID, Integer.toString(nextGUID.getGUID()));
    }

    public String getVideoFolder() {
        String folder = settings.getProperty(SETTING_RESULT_FOLDER);
        if (folder == null) {
            settings.setProperty(SETTING_RESULT_FOLDER, DEFAULT_RESULT_FOLDER);
            return DEFAULT_RESULT_FOLDER;
        }
        return folder;
    }

    public void setVideoFolder(String videoFolder) {
        settings.setProperty(SETTING_RESULT_FOLDER, videoFolder);
    }

    public GUID[] getClients() {
        try {
            String[] strClients = settings.getProperty(SETTING_CLIENTS, "").split(",");
            ArrayList<GUID> tmp = new ArrayList<>();
            for (String client : strClients) {
                String c = client.trim();
                if (c.length() == 0) {
                    continue;
                }
                tmp.add(new GUID(Integer.parseInt(c)));
            }
            GUID[] ret = new GUID[tmp.size()];
            return tmp.toArray(ret);
        } catch (Exception ex) {
            System.out.println("Error in getting clients, set to default.");
            ex.printStackTrace(System.out);
            settings.setProperty(SETTING_CLIENTS, "");
            return new GUID[0];
        }
    }

    public void setClients(GUID[] clients) {
        int[] tmp = new int[clients.length];
        for (int i = 0; i < clients.length; i++) {
            tmp[i] = clients[i].getGUID();
        }
        String val = Arrays.toString(tmp);
        val = val.substring(1, val.length() - 1);
        settings.setProperty(SETTING_CLIENTS, val);
    }

    public void save(String fileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            settings.store(fos, "");
        }

    }
}
