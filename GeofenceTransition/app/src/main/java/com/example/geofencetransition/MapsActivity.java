package com.example.geofencetransition;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
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


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GeofencingClient geofencingClient;
    private GoogleMap mMap;
    private static final int REQUEST_CODE_BACKGROUND = 200;
    private static final int LOITERING_TIME = 900000;//Dwell interval = 15mins
    private static final int DURATION = 60 * 60 * 1000;//geofence existing interval = 1hour
    private static final float RADIUS = 40.0f;//radius for geofence
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final LatLng manipalLib = new LatLng(26.8415517, 75.565365);
    private PendingIntent geofencePendingIntent;
    ArrayList<Geofence> geofenceArrayList = new ArrayList<>();
    private Marker geoFenceMarker;

    final int LOCATION_REQUEST_CODE = 1000;
    final int SET_TIME_INTERVAL = 20 * 1000;
    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    boolean isCalledFromOnCreate = false;
    boolean isAlertVisible = false;
    private LatLng user;
    private Marker UserMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        geofencingClient = LocationServices.getGeofencingClient(this); //client is a medium between maps and geofence object

        isCalledFromOnCreate = true;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
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
                        Log.i("UserPosition", user.toString());
                        setuserMarker(user);

                    }
                }
            }
                @Override
                public void onLocationAvailability (LocationAvailability locationAvailability){
                    super.onLocationAvailability(locationAvailability);
                    if (!locationAvailability.isLocationAvailable()) {
                        if (!isLocationEnabled()) {
                            if (!isAlertVisible)
                                checkAndGetLocationEnabled("onLocationAvailability");
                        }
                    }
                }
        };
        // TODO (DONE): after this is successful, then only run requestLocationUpdates()
        startGettingLocation("onCreate");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if(mapFragment !=null)

        {
            mapFragment.getMapAsync(this);
        }


        //creating a geofence object
        createGeofenceObject();

        //before addGeofence() need to check permissions;
        getGeofence();

    }

        //Initialize a geofence object with values
        private void createGeofenceObject () {
            Log.i("Info: ", "Inside Create object geofence method");
            geofenceArrayList.add(
                    new Geofence.Builder()
                            .setRequestId(GEOFENCE_REQ_ID) // Geofence ID
                            .setCircularRegion(manipalLib.latitude, manipalLib.longitude, RADIUS) // defining fence region
                            .setExpirationDuration(DURATION) // expiring date
                            // Transition types that it should look for
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                            .setLoiteringDelay(LOITERING_TIME)
                            .build()
            );
        }

        //Specify geofences and initial triggers
        private GeofencingRequest getGeofencingRequest () {
            if (geofenceArrayList == null || geofenceArrayList.size() == 0) {
                Toast.makeText(getApplicationContext(), "ArrayList is Empty", Toast.LENGTH_LONG).show();
                return null;
            }
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL); //to reduce spams of continued stay in location
            builder.addGeofences(geofenceArrayList);
            return builder.build();
        }


        //A BroadcastReceiver gets updates when an event occurs, such as a transition into or out of a geofence,
        // and can start long-running background work.
        //
        //The following snippet shows how to define a PendingIntent that starts a BroadcastReceiver:
        private PendingIntent getGeofencePendingIntent () {
            // Reuse the PendingIntent if we already have it.
            if (geofencePendingIntent != null) {
                return geofencePendingIntent;
            }
            Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
            // calling addGeofences() and removeGeofences().
            geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT);
            return geofencePendingIntent;
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
        public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults){
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
                        startGettingLocation("onRequestPermissionsResult");
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                        // write code to display non-functioning of app here
                    }
            }
        }


        //add a geofence using geofenceclient
        //
        //Note: *** addGeofence will not work if request permissions are ont granted for api 10 or above
        private void getGeofence () {

//        if (geofencingClient == null || geofence == null) {
//            Log.i("Info:", "Could not create geofence ob. or client");
//            return;
//        }

//        else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions()) {
                // permission already given
                try {
                    addGeofence();

                } catch (SecurityException e) {
                    Log.i("GET_GEOFENCE", "ERROR IN CALLING ADD_GEOFENCE()");
                    e.printStackTrace();
                }
            } else {
                Log.i("GET_GEOFENCE", "INSIDE ELSE OF GET_GEOFENCE()");
                requestPermissions();

//                    if (shouldShowRequestPermissionRationale("Device cannot work without permission"))
//                    {   Log.i("info: "," request rationale permission returned true");
//                    }
//                    else{
//                        Log.i("Request permission","DEnied!!!");
//                    }
            }
//            }
//        }
        }

//TODO: CHECK GPS AND NETWORK ENABLED FOR HIGH ACCURACY MODE AND A PROMPT FOR USER

        //adds geofence
        private void addGeofence () {
            final String TAG = "Geofence attempt: ";
            geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences added
                            // ...
                            Log.i(TAG, "Successful to ADD");
                            markerForGeofence(manipalLib);
                            drawGeofence();
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to add geofences
                            // ...
                            e.printStackTrace();
                            Log.i(TAG, "FAILED TO ADD ");
                        }
                    });
        }


        // Draw Geofence circle on GoogleMap
        private Circle geoFenceLimits;
        private void drawGeofence () {
            Log.d("Inside func", "drawGeofence()");

            if (geoFenceLimits != null)
                geoFenceLimits.remove();

            CircleOptions circleOptions = new CircleOptions()
                    .center(geoFenceMarker.getPosition())
                    .strokeColor(Color.argb(50, 0, 0, 255))
                    .fillColor(Color.argb(100, 204, 204, 255))
                    .radius(RADIUS);
            geoFenceLimits = mMap.addCircle(circleOptions);
        }

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera. In this case,
         * we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to install
         * it inside the SupportMapFragment. This method will only be triggered once the user has
         * installed Google Play services and returned to the app.
         */


        // Create a marker for the geofence creation
        private void markerForGeofence (LatLng latLng){
            Log.i("Marker for Geo", "markerForGeofence(" + latLng + ")");
            // Define marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .title("Geofence Centre static -Library");
            if (mMap != null) {
                // Remove last geoFenceMarker
                if (geoFenceMarker != null)
                    geoFenceMarker.remove();

                geoFenceMarker = mMap.addMarker(markerOptions);
                geoFenceMarker.setVisible(false);
            }
        }


//TODO: In main project also add transition services function to handle intents by broadcast reciever
//TODO: In main project add remove geofence after 1 hour duration


        // Create a marker for the user
        private void setuserMarker (LatLng latLng){
            Log.i("Marker for USER", "SetuserMarker(" + latLng + ")");
            // Define marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title("Your location");
            if (mMap != null) {
                // Remove last geoFenceMarker
                if (UserMarker != null)
                    UserMarker.remove();

                UserMarker = mMap.addMarker(markerOptions);
            }
        }


        public void startGettingLocation (String callsBy){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (checkPermissionsforfineLocation()) {
                    // permission already given

                    // TODO (DONE): this code needs to a separate function - to enable checking isLocationEnabled all the time
                    checkAndGetLocationEnabled("StartGettingLocation , Parent: " + callsBy); // cannot move further if location is not enabled
                    getLastLocation();
                } else {
                    // TODO (DONE): ask for permission
                    requestPermissionsforfineLoaction();
                }
            } else {
                checkAndGetLocationEnabled("API smaller than 23");
                getLastLocation();
            }

        }


        @Override
        protected void onResume () {
            super.onResume();
            Log.i("ON_RESUME", "IN ON_RESUME() FUNCTION");

            if (isCalledFromOnCreate)
                isCalledFromOnCreate = false;
            else {
                if (!isAlertVisible) {
                    startGettingLocation("onResume");
                }
            }
        }


        // TODO: write code when the lastLocation is not available
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

            // TODO: convert this foreground location updates to background location updates
            // this version of requestLocationUpdates is only suitable for foreground purposes
            // for background location updates use PendingIntent
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }


        // TODO: customize the AlertDialog to tell user why to enable location, also add that High Accuracy is to be set
        public void checkAndGetLocationEnabled (String calledBy){
            if (!isLocationEnabled()) {
                // TODO (DONE): implement onResume() method so that location permissions and fetching can be started after returning from settings
                // TODO (DONE): write code to display AlertDialog and display why location is necessary and give options to open setting to enable location
                // TODO (DONE): write function to handle situation when user click on cancel from AlertDialog (show warning or close app) - I have set it to be not cancellable

                AlertDialog.Builder alertDialog;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    alertDialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                else
                    alertDialog = new AlertDialog.Builder(this);

                isAlertVisible = true;
                Log.i("CHECK_AND_GET", "it is called by " + calledBy);
                Log.i("ALERT", "ALERT IS SET TO VISIBLE");
                alertDialog
                        .setTitle("Enable Location!!")
                        .setMessage("The application needs these permissions to work.\nPlease enable or exit.")
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAffinity();
                                System.exit(0);
                            }
                        })
                        .setCancelable(false)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                isAlertVisible = false;
                                Log.i("ALERT", "ALERT IS SET TO FALSE");
                            }
                        })
                        .show();
            }
        }


        @Override
        protected void onDestroy () {
            super.onDestroy();
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }


        // TODO: add the code to check permission for ACCESS_BACKGROUND_LOCATION when needed
        public boolean checkPermissionsforfineLocation () {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        }


        // TODO: add the permission request for ACCESS_BACKGROUND_LOCATION when needed
        public void requestPermissionsforfineLoaction () {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }


        public boolean isLocationEnabled () {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;

            try {
                if (lm != null) {
                    gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("ERROR", "COULDN'T CHECK IF LOCATION WAS ENABLED");
            }

            Log.i("LOCATIONS", "GPS_ENABLED: " + gps_enabled);
            Log.i("LOCATIONS", "NETWORK_ENABLED: " + network_enabled);

            return (gps_enabled && network_enabled);    // check if OR is needed
        }


        @Override
        public void onMapReady (GoogleMap googleMap){
            mMap = googleMap;

            // Add a marker in Sydney and move the camera
            LatLng hostel = new LatLng(26.8416183, 75.5602043);
           // mMap.addMarker(new MarkerOptions().position(hostel).title("New Door Hostels"));
            Float zoom = 14f;
//        mMap.Camera(CameraUpdateFactory.newLatLng(hostel));
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(hostel, zoom);
            mMap.animateCamera(cameraUpdate);

        }
    }

