package com.example.locationtrackingwithmap;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
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
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    final int LOCATION_REQUEST_CODE = 1000;
    final int SET_TIME_INTERVAL = 5*1000;

    final int CIRCLE_RADIUS = 2;
    final int CIRCLE_STROKE_WIDTH = 5;
    final int MAP_ZOOM_OFFSET = 19;
    final String yourLocation = "Your Location";
    final String yourLastLocation = "Last Location";

    private FusedLocationProviderClient fusedLocationProviderClient;

    LocationRequest locationRequest;
    LocationCallback locationCallback;

    int locationCount = 0;

    boolean isCalledFromOnCreate = false;
    boolean isAlertVisible = false;

    LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        isCalledFromOnCreate = true;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationHandlingSetup();

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        startGettingLocation("onCreate");
    }



    public void addCircleToMap(LatLng userLocation, String title){
        mMap.clear();
//        mMap.addMarker(new MarkerOptions().position(userLocation).title(title));
        mMap.addCircle(new CircleOptions()
                .center(userLocation)
                .radius(CIRCLE_RADIUS)
                .strokeColor(Color.WHITE)
                .strokeWidth(CIRCLE_STROKE_WIDTH)
                .fillColor(Color.BLUE)
                );
        if(title.equals(yourLastLocation))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, MAP_ZOOM_OFFSET));
//        else
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
    }



    public void currentLocation(View view){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, MAP_ZOOM_OFFSET));
        Toast.makeText(getApplicationContext(), "Current Location", Toast.LENGTH_LONG).show();
    }


    public void locationHandlingSetup(){
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(SET_TIME_INTERVAL);

        locationCallback = new LocationCallback(){

            // this function is not called when location is off
            @Override
            public void onLocationResult(LocationResult locationResult){
                if(locationResult == null)
                    return;

                for(Location location: locationResult.getLocations()){
                    if (location != null) {

                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        String lat = "Latitude: "+userLocation.latitude;
                        String lon = "Longitude: "+userLocation.latitude;

                        locationCount++;
                        String locationCountString = "Location Count: "+locationCount;

                        Log.i("COUNT", locationCountString);
                        Log.i("LATITUDE", lat);
                        Log.i("LONGITUDE", lon);

                        // TODO (DONE): update marker on MAP
                        currentLocation = userLocation;
                        addCircleToMap(userLocation, yourLocation);
                    }
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                if(!locationAvailability.isLocationAvailable()){
                    if(!isLocationEnabled()) {
                        if(!isAlertVisible)
                            checkAndGetLocationEnabled("onLocationAvailability");
                    }
                }
            }
        };
    }


    public void startGettingLocation(String callsBy){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){
            if(checkPermissions()){
                checkAndGetLocationEnabled("StartGettingLocation , Parent: "+callsBy); // cannot move further if location is not enabled
                getLastLocation();
            }
            else
                requestPermissions();
        }
        else{
            checkAndGetLocationEnabled("API smaller than 23");
            getLastLocation();
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ON_RESUME", "IN ON_RESUME() FUNCTION");

        if(isCalledFromOnCreate)
            isCalledFromOnCreate = false;
        else {
            if(!isAlertVisible) {
                startGettingLocation("onResume");
            }
        }
    }


    public void getLastLocation(){
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            String lat = "Latitude: "+ lastLocation.latitude;
                            String lon = "Longitude: "+ lastLocation.longitude;
                            Log.i("LAST_LOCATION", "Lat: "+lat+" Lon: "+lon);

                            // TODO (DONE): Set location on MAP
                            currentLocation = lastLocation;
                            addCircleToMap(lastLocation, yourLastLocation);
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
    public void checkAndGetLocationEnabled(String calledBy){
        if(!isLocationEnabled()) {

            AlertDialog.Builder alertDialog;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                alertDialog = new AlertDialog.Builder(MapsActivity.this, android.R.style.Theme_Material_Dialog_Alert);
            else
                alertDialog = new AlertDialog.Builder(MapsActivity.this);

            isAlertVisible = true;
            Log.i("CHECK_AND_GET", "it is called by "+calledBy);
            Log.i("ALERT", "ALERT IS SET TO VISIBLE");
            alertDialog
                    .setTitle("Enable Location!!")
                    .setMessage("The application needs these permissions to work.\nRemember to set MODE: High Accuracy\nPlease enable or exit.")
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
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }



    // this function is called when user responds to a requested permission
    // TODO: read about handling permissions - link in oneNote
    // TODO: add the code to display a message to user showing that not granting permissions will result in non-functioning of app - when permissions is asked when app is opened for first time
    // TODO: also add case for ACCESS_BACKGROUND_LOCATION when it is implemented
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case LOCATION_REQUEST_CODE:
                // if request is cancelled, the result arrays are empty
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("ON REQUEST PERMISSIONS", "START_GETTING_LOCATION() CALLED FROM ON_REQUEST_PERMISSIONS_RESULT");
                    startGettingLocation("onRequestPermissionsResult");
                }
                else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    // write code to display non-functioning of app here
                }
                break;

            case 2000:
                // write code to execute code when ACCESS_BACKGROUND_LOCATION is Granted
                break;
        }
    }



    // TODO: add the code to check permission for ACCESS_BACKGROUND_LOCATION when needed
    public boolean checkPermissions(){
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }



    // TODO: add the permission request for ACCESS_BACKGROUND_LOCATION when needed
    public void requestPermissions(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }



    public boolean isLocationEnabled () {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            if(lm != null) {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.i("ERROR", "COULDN'T CHECK IF LOCATION WAS ENABLED");
        }

        Log.i("LOCATIONS", "GPS_ENABLED: "+gps_enabled);
        Log.i("LOCATIONS", "NETWORK_ENABLED: "+network_enabled);

        return (gps_enabled && network_enabled);    // check if OR is needed
    }
}
