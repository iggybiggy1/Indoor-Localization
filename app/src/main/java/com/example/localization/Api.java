package com.example.localization;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is responsible for fetching all beacons from the API
 */
public class Api implements Runnable {
    private static final String API = "https://locvis.group16.nl/beacons";
    private Set<iBeacon> allBeacons; // Stores all beacon objects retrieved from the Excel document

    private static final int MAC_INDEX = 0;
    private static final int LONGITUDE_INDEX = 1;
    private static final int LATITUDE_INDEX = 2;
    private static final int FLOOR_INDEX = 3;

    /**
     * This function initializes a new object of this class and initializes the allBeacons set
     */
    public Api() {
        this.allBeacons = new HashSet<>();
    }

    /**
     * This function fetches all the beacons from the API and puts them in the allBeacons set.
     * The beacons are casted to iBeacon objects.
     * @throws IOException
     * @throws JSONException
     */
    private void fetchAllBeacons() throws IOException, JSONException {
        // Fetch JSON from API
        JSONArray json = new JSONArray(IOUtils.toString(new URL(API), StandardCharsets.UTF_8));

        String mac_address;
        double longitude;
        double latitude;
        int floor;

        // Iterate through the JSON array
        for (int i = 0; i < json.length(); i++) {
            mac_address = json.getJSONArray(i).get(MAC_INDEX).toString();
            longitude = Double.parseDouble(json.getJSONArray(i).get(LONGITUDE_INDEX).toString());
            latitude = Double.parseDouble(json.getJSONArray(i).get(LATITUDE_INDEX).toString());
            floor = Integer.parseInt(json.getJSONArray(i).get(FLOOR_INDEX).toString());

            this.allBeacons.add(new iBeacon(mac_address, new Location(longitude, latitude), floor));
        }
    }

    /**
     * This function returns the beacons set
     * @return set of all iBeacons from the API
     */
    public Set<iBeacon> getAllBeacons() {
        return this.allBeacons;
    }

    @Override
    public void run() {
        try {
            this.fetchAllBeacons();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
