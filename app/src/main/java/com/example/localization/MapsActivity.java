package com.example.localization;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class contains the main Google Maps activity.
 */
public class MapsActivity extends AppCompatActivity {

    private static final String IBEACON = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String FILENAME = "beacon_list.xlsx";
    private static final int ZOOM_LEVEL = 20;
    private static final int THRESHOLD = -90;
    private static final double USER_RADIUS = 0.75;

    private SupportMapFragment smf;
    private GoogleMap map;
    private BeaconManager beaconManager;
    private ExcelReader excelReader;
    private Api api;
    private LocationFinder locationFinder;
    private ArrayList<iBeacon> connectedBeacons;
    private Location currentLocation;
    private int currentFloor;
    private Circle tracker;
    private Set<Circle> markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.smf = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);

        Dexter.withContext(getApplicationContext())
                .withPermissions(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            System.out.println("[SYSTEM] PERMISSION GRANTED!");
                            init();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    /**
     * Initialization method for this class.
     * This method sets up permissions, tasks and initializes event listeners and managers.
     */
    private void init() {
        this.smf.getMapAsync(this::onMapReady);

        this.connectedBeacons = new ArrayList<>();
        this.api = new Api();
        this.markers = new HashSet<>();
        this.locationFinder = new LocationFinder();
        this.currentFloor = 5;

        // Start API fetching on separate thread and wait for it to finish
        Thread apiThread = new Thread(api);
        apiThread.start();
        try {
            apiThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the BeaconManager and range notifier
     */
    private void setupBeaconDetection() {
        this.beaconManager =  BeaconManager.getInstanceForApplication(this);
        this.beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON));
        this.beaconManager.addRangeNotifier((beacons, region) -> {
            if (beacons.size() > 0) {
                // Keep track of whether or not the connected beacons set has been updated
                AtomicBoolean updated = new AtomicBoolean(false);

                // Clear the markers
                for (Circle marker : this.markers) {
                    marker.remove();
                }
                this.markers.clear();

                for (Beacon beacon : beacons) {
                    System.out.println("[SYSTEM] FOUND DEVICE WITH RSSI " + beacon.getRssi() + " WITH ADDRESS " + beacon.getBluetoothAddress()
                    + " WITH ID3 " + beacon.getId3());
                    Optional<iBeacon> value = this.connectedBeacons.stream().filter(x -> x.getMac().equals(beacon.getBluetoothAddress())).findFirst();

                    if (beacon.getRssi() <= THRESHOLD && this.connectedBeacons.size() > 3) {
                        // Deletes the iBeacon if it exists in the set of active beacons
                        value.ifPresent(iBeacon -> {
                            this.connectedBeacons.remove(iBeacon);
                            updated.set(true);
                        });
                    } else {
                        // Update beacon information if it already exists in the set of all active beacons
                        if (value.isPresent()) {
                            iBeacon currentBeacon = value.get();
                            currentBeacon.setDistance(beacon.getDistance());
                            currentBeacon.setRssi(beacon.getRssi());
                            updated.set(true);
                        } else { // Retrieve beacon from excel reader or api and put it in the active set of beacons
                            Optional<iBeacon> foundBeacon = this.api.getAllBeacons().stream().filter(x -> x.getMac().equals(beacon.getBluetoothAddress())).findFirst();
                            foundBeacon.ifPresent(iBeacon -> {
                                iBeacon.setDistance(beacon.getDistance());
                                iBeacon.setRssi(beacon.getRssi());
                                this.connectedBeacons.add(iBeacon);
                                updated.set(true);
                            });
                        }
                    }
                }

                // Set markers on the map for connected beacons
                for (iBeacon beacon : this.connectedBeacons) {
                    this.setBeaconMarker(beacon);
                }

                // If the connected beacons set has been updated, a new current position will be calculated and updated on the map
                if (updated.get()) {
                    System.out.println("[SYSTEM] LIST: " + this.connectedBeacons.size());
                    this.currentLocation = this.locationFinder.optimisation(this.connectedBeacons);
                    onLocationChange();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        beaconManager.startRangingBeacons(new Region("myRangingUniqueId", null, null, null));
    }

    /**
     * @param googleMap maps object passed when maps is ready
     */
    private void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        this.currentLocation = new Location(52.239346220076186, 6.856276336536508);
        this.onLocationChange();
        this.setupBeaconDetection();
    }

    /**
     * This function gets called whenever a location is changed, which will update the map and floor number and put a marker
     */
    private void onLocationChange() {
        LatLng currentPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, ZOOM_LEVEL));

        //Here you can take the snapshot or whatever you want
        IndoorBuilding building = map.getFocusedBuilding();
        if(building != null) {
            List<IndoorLevel> levels = building.getLevels();
            //active the level you want to display on the map
            levels.get(levels.size() - currentFloor).activate();
        }

        // Shows current position of user on the map
        if (this.tracker == null) {
            this.tracker = this.map.addCircle(new CircleOptions()
                    .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                    .radius(USER_RADIUS)
                    .strokeColor(Color.MAGENTA)
                    .strokeWidth(7f)
                    .fillColor(Color.WHITE));
        } else {
            this.tracker.setCenter(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
    }

    private void setBeaconMarker(iBeacon beacon) {
        // Shows position of a beacon
        CircleOptions circle = new CircleOptions()
                .center(new LatLng(beacon.getLocation().getLatitude(), beacon.getLocation().getLongitude()))
                .radius(beacon.getDistance())
                .strokeColor(Color.CYAN)
                .strokeWidth(7f)
                .fillColor(Color.TRANSPARENT);

        this.markers.add(map.addCircle(circle));
    }
}