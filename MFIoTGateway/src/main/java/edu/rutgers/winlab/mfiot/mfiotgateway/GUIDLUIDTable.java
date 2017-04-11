/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.mfiot.mfiotgateway;

import edu.rutgers.winlab.jmfapi.GUID;
import edu.rutgers.winlab.mfiot.packets.LUID;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author ubuntu
 */
public class GUIDLUIDTable {

    private final HashMap<GUID, LUID> table = new HashMap<>();
    private final HashMap<LUID, LinkedList<GUID>> inverseTable = new HashMap<>();

    public void put(GUID guid, LUID luid) {
        if (table.containsKey(guid)) {
            throw new IllegalArgumentException("LUID " + guid + " already exists in the table.");
        }
        table.put(guid, luid);

        LinkedList<GUID> guids = inverseTable.get(luid);
        if (guids == null) {
            inverseTable.put(luid, guids = new LinkedList<>());
        }
        guids.add(guid);
    }

    public LUID remove(GUID guid) {
        LUID tmpLUID = table.remove(guid);
        if (tmpLUID != null) {
            LinkedList<GUID> guids = inverseTable.get(tmpLUID);
            if (guids != null) {
                guids.remove(guid);
                if (guids.isEmpty()) {
                    inverseTable.remove(tmpLUID);
                }
            }
        }
        return tmpLUID;
    }

    public LinkedList<GUID> remove(LUID luid) {
        LinkedList<GUID> guids = inverseTable.remove(luid);
        if (guids != null) {
            for (GUID guid : guids) {
                table.remove(guid);
            }
        }
        return guids;
    }

    public LUID getLUID(GUID guid) {
        return table.get(guid);
    }

    public GUID[] getGUIDs(LUID luid) {
        LinkedList<GUID> guids = inverseTable.get(luid);
        GUID[] ret = new GUID[guids.size()];
        return guids.toArray(ret);
    }
}
