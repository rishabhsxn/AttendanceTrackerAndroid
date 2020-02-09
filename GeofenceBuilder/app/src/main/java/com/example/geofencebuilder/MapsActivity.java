package com.example.geofencebuilder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GeofencingClient geofencingClient;
    private Geofence geofence;
    private GoogleMap mMap;
    private static final int REQUEST_CODE_BACKGROUND = 200;
    private static final int LOITERING_TIME = 900000;//Dwell interval = 15mins
    private static final int DURATION = 60*60*1000;//geofence existing interval = 1hour
    private static final float RADIUS = 500.0f;//radius for geofence
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final LatLng manipalLib = new LatLng(26.8953255,75.5293635);
    private PendingIntent geofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geofencingClient = LocationServices.getGeofencingClient(this); //client is a medium between mapsand geofence object

        //creating a geofence object
       createGeofenceObject();

       //before addGeofence() need to check permissions;
        getGeofence();

    }

    //Initialize a geofence object with values
    private void createGeofenceObject(){
        Log.i("Info: ","Inside Create object geofence method");
        geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID) // Geofence ID
                .setCircularRegion( manipalLib.latitude, manipalLib.longitude, RADIUS) // defining fence region
                .setExpirationDuration( DURATION ) // expiring date
                // Transition types that it should look for
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_DWELL )
                .setLoiteringDelay(LOITERING_TIME)
                .build();
    }

    //Specify geofences and initial triggers
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL); //to reduce spams of continued stay in location
        builder.addGeofence((geofence));
        return builder.build();
    }


    //A BroadcastReceiver gets updates when an event occurs, such as a transition into or out of a geofence,
    // and can start long-running background work.
    //
    //The following snippet shows how to define a PendingIntent that starts a BroadcastReceiver:
    private PendingIntent getGeofencePendingIntent() {
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
    public boolean checkPermissions(){
        Log.i("info: ","Inside check permission");
        boolean result = true;
        if(Build.VERSION.SDK_INT >= 29) {
            result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        Log.i("CHECK_PERMISSION", "PERMISSION: "+result);

        return result;
    }


    public void requestPermissions(){
        Log.i("info: ","Inside Request permission");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_BACKGROUND);
    }


    // this function is called when user responds to a requested permission

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case REQUEST_CODE_BACKGROUND:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("ON REQUEST PERMISSIONS", "START_BACKGROUND () CALLED FROM ON_REQUEST_PERMISSIONS_RESULT");
                    addGeofence();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


//    @Override
//    public void onRequestPermissionsResult(int REQUEST_CODE_BACKGROUND, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(REQUEST_CODE_BACKGROUND, permissions, grantResults);
//
//        switch (){
//            case REQUEST_CODE_BACKGROUND:
//                // if request is cancelled, the result arrays are empty
//                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                    Log.i("ON REQUEST PERMISSIONS", "START_BACKGROUND () CALLED FROM ON_REQUEST_PERMISSIONS_RESULT");
//                    getGeofence();
//                }
//                else {
//                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
//                    requestPermissions();
//                    // write code to display non-functioning of app here
//                }
//                break;
//
//        }
//    }

//add a geofence using geofenceclient
    //
    //Note: *** addGeofence will not work if request permissions are ont granted for api 10 or above
    private void getGeofence() {

        if (geofencingClient == null || geofence == null) {
            Log.i("Info:", "Could not create geofence ob. or client");
            return;
        }

        else {
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
        }
    }



    //adds geofence
    private void addGeofence(){
        final String TAG = "Geofence attempt: ";
        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        // ...
                        Log.i(TAG, "Successful to ADD");
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



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng hostel = new LatLng(26.8416183,75.5602043);
        mMap.addMarker(new MarkerOptions().position(hostel).title("New Door Hostels"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(hostel));

    }
}
