/*
 *  * Copyright © Wynntils - 2020.
 */

package com.wynntils.webapi.profiles.music;

import com.wynntils.core.utils.objects.Location;

import java.util.ArrayList;
import java.util.HashMap;

public class MusicLocationsProfile {

    HashMap<String, String> dungeons = new HashMap<>();
    HashMap<String, String> entries = new HashMap<>();

    ArrayList<MusicAreaProfile> areas = new ArrayList<>();

    public MusicLocationsProfile() { }

    public String getDungeonTrack(String name) {
        return dungeons.getOrDefault(name, null);
    }

    public String getEntryTrack(String name) {
        return entries.getOrDefault(name, null);
    }

    public MusicAreaProfile getAreaTrack(Location loc) {
        for (MusicAreaProfile area : areas) {
            if (!area.getRegion().isInside(loc)) continue;

            return area;
        }

        return null;
    }

    public MusicAreaProfile getAreaTrack(String id) {
        for (MusicAreaProfile area : areas) {
            if (!area.getId().equalsIgnoreCase(id)) continue;

            return area;
        }

        return null;
    }

}