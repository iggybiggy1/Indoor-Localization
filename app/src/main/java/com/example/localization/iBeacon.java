package com.example.localization;

/**
 * This class represents an iBeacon object from the Excel document
 */
public class iBeacon {
    private int id;
    private String name;
    private String mac;
    private Location location;
    private int floor;
    private double distance;
    private int rssi;

    /**
     * Disallowing empty constructor usage.
     */
    private iBeacon() {}

    /**
     * This constructor initializes a beacon with the given properties.
     * Mainly used by the ExcelReader class.
     * @param id device_id
     * @param name device_name
     * @param mac mac_address
     * @param location Location object contains longitude and latitude
     * @param floor floor number
     */
    public iBeacon(int id, String name, String mac, Location location, int floor) {
        this.id = id;
        this.name = name;
        this.mac = mac;
        this.location = location;
        this.floor = floor;
    }

    /**
     * This constructor initializes a beacon with the given properties.
     * Mainly used by the API class.
     * @param mac mac_address
     * @param location Location object contains longitude and latitude
     * @param floor floor number
     */
    public iBeacon(String mac, Location location, int floor) {
        this.mac = mac;
        this.location = location;
        this.floor = floor;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public Location getLocation() {
        return location;
    }

    public int getFloor() {
        return floor;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
