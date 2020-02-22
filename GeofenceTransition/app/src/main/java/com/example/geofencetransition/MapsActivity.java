package com.example.geofencetransition;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

// TODO: manually turn on location for the app, as it is not asked for this application
// TODO: resolve the issue in action in Broadcaster


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GeofencingClient geofencingClient;
    private static GoogleMap mMap;
    private static final int REQUEST_CODE_BACKGROUND = 200;
    private static final int LOITERING_TIME = 15*1000;           //Dwell interval = 1mins
    private static final int DURATION = 60 * 60 * 1000;        //geofence existing interval = 1hour
    private static final float RADIUS = 40.0f;            //radius for geofence
    private PendingIntent geofencePendingIntent;

    private static int red = 255;
    private static int blue = 180;
    private static int green = 180;

    final int LOCATION_REQUEST_CODE = 1000;
    final int SET_TIME_INTERVAL = 5 * 1000;
    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    boolean isCalledFromOnCreate = false;
    private LatLng user;
    private Marker UserMarker;

    static ArrayList<Geofence> geofenceArrayList = new ArrayList<>();
    static ArrayList<LatLng> geofenceCentres = new ArrayList<>();
    static final ArrayList<String> geofenceIDs = new ArrayList<>();
    static Circle[] geofenceCircles = new Circle[10];

    private static final LatLng manipalLib = new LatLng(26.8415517, 75.565365);
    private static final LatLng oldMesss = new LatLng(26.8429, 75.5653);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        geofencingClient = LocationServices.getGeofencingClient(this); //client is a medium between maps and geofence object

        // add more Centres for Geofences here
        geofenceCentres.add(manipalLib);
        geofenceIDs.add("Manipal Library");
        geofenceCentres.add(oldMesss);
        geofenceIDs.add("Old Mess");

        isCalledFromOnCreate = true;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if(mapFragment !=null)

        {
            mapFragment.getMapAsync(this);
        }


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setUpLocationRequest();

        setUpGeofenceArray();

    }

    @Override
    public void onMapReady (GoogleMap googleMap){
        mMap = googleMap;

        LatLng manipalLibrary = new LatLng(26.8415517, 75.565365);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(manipalLibrary, 18);
        mMap.animateCamera(cameraUpdate);

        getGeofence();
        getLastLocation();

    }


    private void setUpGeofenceArray () {
        Log.i("Info: ", "INSIDE SETUP_GEOFENCE_ARRAY");

        for(int i = 0; i< geofenceCentres.size() ;i++) {

            geofenceArrayList.add(
                    new Geofence.Builder()
                            .setRequestId(geofenceIDs.get(i))
                            .setCircularRegion(geofenceCentres.get(i).latitude, geofenceCentres.get(i).longitude, RADIUS) // defining fence region
                            .setExpirationDuration(DURATION)
                            // Transition types that it should look for
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setLoiteringDelay(LOITERING_TIME)
                            .build()
            );
        }
    }


    //Specify geofences and initial triggers
    private GeofencingRequest getGeofencingRequest () {
            if (geofenceArrayList == null || geofenceArrayList.size() == 0) {
                Toast.makeText(getApplicationContext(), "ArrayList is Empty", Toast.LENGTH_LONG).show();
                return null;
            }
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL ); //to reduce spams of continued stay in location
            builder.addGeofences(geofenceArrayList);
            return builder.build();
        }


    private PendingIntent getGeofencePendingIntent () {
            // Reuse the PendingIntent if we already have it.
            if (geofencePendingIntent != null) {
                return geofencePendingIntent;
            }
            Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
            // calling addGeofences() and removeGeofences().
            geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            return geofencePendingIntent;
    }


    private void getGeofence () {

        if (geofencingClient == null || geofenceArrayList == null) {
            Log.i("Info:", "COULDN'T GET GEOFENCE, GEOFENCE_CLIENT OR GEOFENCE_ARRAYLIST WAS NULL");
            return;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions()) {
                // permission already given
                addGeofence();
            }
            else {
                Log.i("GET_GEOFENCE", "REQUESTING PERMISSIONS");
                requestPermissions();
            }
        }
        else{
            addGeofence();
        }

    }


    // Creates logical geofences
    private void addGeofence () {

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("ADD_GEOFENCE", "SUCCESSFUL TO CREATE LOGICAL GEOFENCES");

                        // Geofences created, now draw those on Map
                        for (int i = 0; i < geofenceArrayList.size(); i++)
                        {
                            drawGeofence(Color.RED, i);
                        }

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        e.printStackTrace();
                        Log.i("ADD_GEOFENCE", "FAILED TO CREATE LOGICAL GEOFENCES");
                    }
                });
    }


    static void drawGeofence(int color, int index){

        String title = geofenceIDs.get(index);
        Log.i("DRAW_GEOFENCE", "DRAWING GEOFENCE: "+title);

        LatLng center = geofenceCentres.get(index);

        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(center)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);

        CircleOptions circleOptions =  new CircleOptions()
                .center(center)
                .radius(RADIUS)
                .fillColor(color)
                .strokeColor(color);


        if (mMap != null) {

            mMap.addMarker(markerOptions);

            if(geofenceCircles[index] != null){
                geofenceCircles[index].remove();
            }

            Circle circle = mMap.addCircle(circleOptions);
            geofenceCircles[index] = circle;
        }
    }


    // Create a marker for the user
    private void setuserMarker (LatLng latLng){
            // Define marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    .title("Your location");
            if (mMap != null) {
                // Remove last geoFenceMarker
                if (UserMarker != null)
                    UserMarker.remove();

                UserMarker = mMap.addMarker(markerOptions);

            }
        }


    //returns boolean to check if permission is granted:
    public boolean checkPermissions () {
        Log.i("info: ", "Inside check permission");
        boolean result = true;
        if (Build.VERSION.SDK_INT >= 29) {
            result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        Log.i("CHECK_PERMISSION", "PERMISSION: " + result);

        return result;
    }


    public void requestPermissions () {
        Log.i("info: ", "Inside Request permission");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_BACKGROUND);
    }


    // this function is called when user responds to a requested permission
    @Override
    public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_BACKGROUND:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ON REQUEST PERMISSIONS", "START_BACKGROUND () CALLED FROM ON_REQUEST_PERMISSIONS_RESULT");
                    addGeofence();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case LOCATION_REQUEST_CODE:
                // if request is cancelled, the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ON REQUEST PERMISSIONS", "START_GETTING_LOCATION() CALLED FROM ON_REQUEST_PERMISSIONS_RESULT");
//                    startGettingLocation("onRequestPermissionsResult");
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    // write code to display non-functioning of app here
                }
        }
    }


    private void setUpLocationRequest(){
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(SET_TIME_INTERVAL);

        locationCallback = new LocationCallback() {
            // this function is not called when location is off
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null)
                    return;

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        user = new LatLng(location.getLatitude(), location.getLongitude());
                        setuserMarker(user);

                    }
                }
            }
//            @Override
//            public void onLocationAvailability (LocationAvailability locationAvailability){
//                super.onLocationAvailability(locationAvailability);
//                if (!locationAvailability.isLocationAvailable()) {
//                    if (!isLocationEnabled()) {
//                        if (!isAlertVisible)
//                            checkAndGetLocationEnabled("onLocationAvailability");
//                    }
//                }
//            }
        };
    }


    public void getLastLocation () {
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            user = new LatLng(location.getLatitude(), location.getLongitude());
                            setuserMarker(user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // write code here to handle when lastLocation is not available
                    }
                });

        // this version of requestLocationUpdates is only suitable for foreground purposes
        // for background location updates use PendingIntent
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }


    // TODO: remove Geofences, in this case when application is closed, otherwise after class hours
    @Override
    protected void onDestroy () {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

}
