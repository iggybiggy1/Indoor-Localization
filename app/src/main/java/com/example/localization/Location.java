package com.example.localization;

/**
 * This class represents a location on the map as an object containing longitude and latitude.
 */
public class Location {
    double longitude;
    double latitude;

    /**
     * Disallowing use of empty constructor
     */
    public Location() {
        this.longitude = 0;
        this.latitude = 0;
    }

    /**
     * Create an instance of this location object.
     * @param longitude longitude
     * @param latitude latitude
     */
    public Location(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLatitude() {
        return this.latitude;
    }
}
