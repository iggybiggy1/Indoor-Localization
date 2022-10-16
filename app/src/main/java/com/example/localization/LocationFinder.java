package com.example.localization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LocationFinder {

    ArrayList<iBeacon> connectedBeacons;
    int myFloor = 0;
    double floorDistance = 3;
    Location nextLocation;
    Location lastLocation;
    Location currentLocation;
    private double lastError = 0.0;

    final double stepSize = 0.00001;

    /**
     * Calculates the distance between two coordinate pairs
     * @param loc1 coordinate pair of the first beacon
     * @param loc2 coordinate pair of the second beacon
     * @return distance between two beacons
     */
    public double calculateDistance(Location loc1, Location loc2){
        //calculate distance between two coordinates
        final double R = 6371000;
        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double latDiff = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double longDiff = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.pow(Math.sin(latDiff / 2), 2) + Math.pow(Math.sin(longDiff / 2), 2) *
                Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    private double calculateDistanceCircle(Location loc1, Location loc2, double radius){
        double distance = calculateDistance(loc1, loc2);
        return distance - radius;
    }

    /**
     * Calculate the average location using the list with active beacons
     * @param beacons ArrayList of active beacons
     * @return average location derived from the list of active beacons
     */
    private Location averageLocation(ArrayList<iBeacon> beacons) {    // arrayList of active beacons
        Location thisLocation = new Location();
        double avgLat = 0;
        double avgLon = 0;
        int N = beacons.size();

        for(iBeacon beacon: beacons){
            avgLat += beacon.getLocation().getLatitude();
            avgLon += beacon.getLocation().getLongitude();
        }
        avgLat/=N;
        avgLon/=N;
        thisLocation.setLatitude(avgLat);
        thisLocation.setLongitude(avgLon);

        return thisLocation;
    }

    private double calculateError(Location location, ArrayList<iBeacon> beacons) {
        double error = 0.0;
        int N = beacons.size();
        for(iBeacon beacon: beacons){
            double distance = calculateDistanceCircle(location, beacon.getLocation(), beacon.getDistance());
            error += distance*distance;
        }
        error/= N;
        return error;
    }

    /**
     * Method for determining, where should the localization be updated
     * @param location coordinate pair of the beacon
     * @param error value of the error calculated
     * @param beacons list of currently available beacons
     * @return location where the localization should be updated
     */
    private Location compareNeighbours(Location location, double error, ArrayList<iBeacon> beacons){
        HashMap<Double, Location> beaconList = new HashMap<>();
        ArrayList<Double> errorList = new ArrayList<>();

        //calculate position of 4 neighbours using stepSize
        Location NeighbourNorth = new Location(location.getLongitude(), location.getLatitude()+stepSize);
        Location NeighbourSouth = new Location(location.getLongitude(), location.getLatitude()-stepSize);
        Location NeighbourEast = new Location(location.getLongitude()+stepSize, location.getLatitude());
        Location NeighbourWest = new Location(location.getLongitude()-stepSize, location.getLatitude());

        //calculate the four errors
        double errorNorth = calculateError(NeighbourNorth, beacons);
        double errorSouth = calculateError(NeighbourSouth, beacons);
        double errorEast = calculateError(NeighbourEast, beacons);
        double errorWest = calculateError(NeighbourWest, beacons);

        // adds the potential neighbours with their errors to a hashmap (creates a pair of (errorValue,Neighbour))
        beaconList.put(errorNorth, NeighbourNorth);
        beaconList.put(errorSouth, NeighbourSouth);
        beaconList.put(errorEast, NeighbourEast);
        beaconList.put(errorWest, NeighbourWest);
        beaconList.put(error, location);

        // adds the errors to the arrayList
        errorList.add(errorNorth);
        errorList.add(errorSouth);
        errorList.add(errorEast);
        errorList.add(errorWest);
        errorList.add(error);

        // extracts the beacon with the smallest error value
        Location nextLocation = beaconList.get(Collections.min(errorList));
        this.nextLocation = nextLocation;

        lastError = Collections.min(errorList);

        // clears the arraylist and hashmap
        beaconList.clear();
        errorList.clear();

        // return the next location
        return nextLocation;
    }
    // TODO: gotta check for the floor in the excel sheet
    // maybe method replace cannot be used here
    private int findFloor(ArrayList<iBeacon> beacons){
        HashMap<Integer,Double> floorMap = new HashMap<>();
        int currentFloor = -1;
        double currentPower;
        floorMap.put(1,0.0);
        floorMap.put(2,0.0);
        floorMap.put(3,0.0);
        floorMap.put(4,0.0);
        floorMap.put(5,0.0);
        for (iBeacon beacon: beacons) {
            currentPower = Math.pow(10, beacon.getRssi()/10);
            double power = floorMap.get(beacon.getFloor()) + currentPower;
            floorMap.remove(beacon.getFloor());
            floorMap.put(beacon.getFloor(), power);
        }
        Set<Double> powers = new HashSet<>(floorMap.values());
        double highestPower = Collections.max(powers);
        try{
            currentFloor = getKeyByValue(floorMap, highestPower);
        } catch (NullPointerException e) {
            System.out.println("Exception in class LocationFinder, method findFloor");
        }
        return currentFloor;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private ArrayList<iBeacon> floorCorrection(ArrayList<iBeacon> beacons){
        int floorDifference;
        for(iBeacon beacon: beacons){
            if(myFloor == 0){
                System.out.println("[ERROR:] floorCorrection is called while myFloor is not set");
            }
            else{
                floorDifference = beacon.getFloor() - myFloor;
                double newDistance = Math.sqrt(Math.pow(beacon.getDistance(), 2)-Math.pow((floorDifference*floorDistance), 2));
                beacon.setDistance(newDistance);
            }
        }
        return beacons;
    }

    public Location optimisation(ArrayList<iBeacon> beacons) {
        //Fill list with new beacons
        connectedBeacons = beacons;

        //Find on which floor you are
        myFloor = findFloor(beacons);

        //Correct the distance to other floors
        beacons = floorCorrection(beacons);

        //find average location of beacons
        lastLocation = averageLocation(beacons);
        lastError = calculateError(lastLocation, beacons);

        //check neighbours of starting location error
        currentLocation = compareNeighbours(lastLocation, lastError, beacons);

        //keep checking neighbours until input location is the same as output location
        //this will only happen when all neighbours have more error
        //meaning we found the point with lowest error and our best guess of our location
        while(!(lastLocation.getLongitude()==currentLocation.getLongitude()&&
                lastLocation.getLatitude()== currentLocation.getLatitude())){
            lastLocation.setLongitude(currentLocation.getLongitude());
            lastLocation.setLatitude(currentLocation.getLatitude());
            currentLocation = compareNeighbours(lastLocation, lastError, beacons);
        }
        return currentLocation;
    }
}
