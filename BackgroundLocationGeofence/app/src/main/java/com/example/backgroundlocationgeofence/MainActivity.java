package com.example.backgroundlocationgeofence;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final String TAG = MainActivity.class.getSimpleName();

    Button requestUpdatesButton;
    Button removeUpdatesButton;
    Button addGeofences;
    Button removeGeofences;

    final int LOCATION_REQUEST_CODE = 1000;
    final int UPDATE_INTERVAL = 20 * 1000;
    final int MAX_WAIT_TIME = UPDATE_INTERVAL;

    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;

    private GeofencingClient geofencingClient;
    private static final int LOITERING_TIME = 20*1000;           //Dwell interval = 1mins
    private static final int DURATION = 60 * 60 * 1000;        //geofence existing interval = 1hour
    private static final float RADIUS = 40.0f;            //radius for geofence
    private PendingIntent geofencePendingIntent;

    static ArrayList<Geofence> geofenceArrayList = new ArrayList<>();
    static ArrayList<LatLng> geofenceCentres = new ArrayList<>();
    static final ArrayList<String> geofenceIDs = new ArrayList<>();

    private static final LatLng manipalLib = new LatLng(26.8415517, 75.565365);
    private static final LatLng oldMesss = new LatLng(26.8429, 75.5653);
    private static final LatLng voldRoom = new LatLng(26.8422,75.5611);

    SharedPreferences sharedPreferences;

//    int locationCount = 0;

    Snackbar locationSnackbar;


    boolean firstTimeOpen;
    boolean userGoneToSettings = false;
    final String KEY_FIRST_TIME_OPEN = "firstTimeOpen";
    final String KEY_IS_REQUESTING_UPDATES = "isRequestingUpdates";
    final String KEY_GEOFENCES_ADDED = "geofencesAdded";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        requestUpdatesButton = (Button) findViewById(R.id.requestUpdatesButton);
        removeUpdatesButton = (Button) findViewById(R.id.removeUpdatesButton);
        requestUpdatesButton.setEnabled(false);
        removeUpdatesButton.setEnabled(false);
        addGeofences = (Button) findViewById(R.id.addGeofenceButton);
        removeGeofences = (Button) findViewById(R.id.removeGeofenceButton);
        addGeofences.setEnabled(false);
        removeGeofences.setEnabled(false);

        setupLocationSnackbar();

        sharedPreferences = getSharedPreferences("com.example.backgroundlocation", MODE_PRIVATE);
        firstTimeOpen = sharedPreferences.getBoolean(KEY_FIRST_TIME_OPEN, true);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationRequest();

        geofencingClient = LocationServices.getGeofencingClient(this); //client is a medium between maps and geofence object
        geofenceCentres.add(manipalLib);
        geofenceIDs.add("Manipal Library");
        geofenceCentres.add(oldMesss);
        geofenceIDs.add("Old Mess");
        geofenceCentres.add(voldRoom);
        geofenceIDs.add("Vold's Room");

        setUpGeofenceArray();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermissions())
                requestPermissions();
            else {
                requestUpdatesButton.setEnabled(true);
                addGeofences.setEnabled(true);
            }
        }
        else {
            requestUpdatesButton.setEnabled(true);
            addGeofences.setEnabled(true);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();

        boolean isRequestingUpdates = sharedPreferences.getBoolean(KEY_IS_REQUESTING_UPDATES, false);
        boolean geofencesAddedAlready = sharedPreferences.getBoolean(KEY_GEOFENCES_ADDED, false);
        if(isRequestingUpdates) {
            requestUpdatesButton.setEnabled(false);
            removeUpdatesButton.setEnabled(true);
        }

        if(geofencesAddedAlready){
            addGeofences.setEnabled(false);
            removeGeofences.setEnabled(true);
        }

        if(userGoneToSettings){
            userGoneToSettings = false;
            // check if now the permissions are Granted, if not then again request
            if(checkPermissions()) {

                if (isLocationEnabled()) {
                    requestUpdatesButton.setEnabled(true);
                    addGeofences.setEnabled(true);
                    if(locationSnackbar.isShown())
                        locationSnackbar.dismiss();
                }
                else
                    locationSnackbar.show();
            }
            else
                requestPermissions();
        }
    }


    private void setUpGeofenceArray () {
        Log.i("Info: ", "INSIDE SETUP_GEOFENCE_ARRAY");

        for(int i = 0; i< geofenceCentres.size() ;i++) {

            geofenceArrayList.add(
                    new Geofence.Builder()
                            .setRequestId(geofenceIDs.get(i))
                            .setCircularRegion(geofenceCentres.get(i).latitude, geofenceCentres.get(i).longitude, RADIUS) // defining fence region
                            .setExpirationDuration(DURATION)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setLoiteringDelay(LOITERING_TIME)
                            .build()
            );
        }
    }


    public void setupLocationSnackbar(){
        View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userGoneToSettings = true;
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };
        locationSnackbar = Snackbar.make(findViewById(R.id.activity_main), R.string.location_not_enabled, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.enable_location, mOnClickListener);
    }


    public void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setMaxWaitTime(MAX_WAIT_TIME);
//        locationRequest.setFastestInterval(20*1000);

        // TODO: set FastestInterval if required
    }


    private PendingIntent getLocationPendingIntent() {
        Intent intent = new Intent(this, MyBroadcastReceiver.class);
        intent.setAction(MyBroadcastReceiver.ACTION_RECEIVED_LOCATION);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public void startLocationUpdates(){
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getLocationPendingIntent());
        Log.i(TAG, "REQUESTING LOCATION UPDATES");
    }


    public void requestUpdates(View view){
        checkAndGetLocationEnabled();
        if(isLocationEnabled()) {
            startLocationUpdates();
            sharedPreferences.edit().putBoolean(KEY_IS_REQUESTING_UPDATES, true).apply();
            requestUpdatesButton.setEnabled(false);
            removeUpdatesButton.setEnabled(true);
        }
    }


    public void removeUpdates(View view){
        Log.i(TAG, "REMOVED LOCATION UPDATES");
        removeUpdatesButton.setEnabled(false);
        requestUpdatesButton.setEnabled(true);
        fusedLocationProviderClient.removeLocationUpdates(getLocationPendingIntent());
        sharedPreferences.edit().putBoolean(KEY_IS_REQUESTING_UPDATES, false).apply();
    }


    public void addGeofences(View view){
        Log.i(TAG, "GEOFENCES ADDITION INITIALIZED");
        addGeofences.setEnabled(false);
        removeGeofences.setEnabled(true);
        addLogicalGeofences();
        sharedPreferences.edit().putBoolean(KEY_GEOFENCES_ADDED, true).apply();
    }


    public void removeGeofences(View view){
        Log.i(TAG, "GEOFENCES REMOVED");
        addGeofences.setEnabled(true);
        removeGeofences.setEnabled(false);
        geofencingClient.removeGeofences(getGeofencePendingIntent());
        sharedPreferences.edit().putBoolean(KEY_GEOFENCES_ADDED, false).apply();
    }


    public void addLogicalGeofences(){
        if (geofencingClient == null || geofenceArrayList == null) {
            Log.i("Info:", "COULDN'T GET GEOFENCE, GEOFENCE_CLIENT OR GEOFENCE_ARRAYLIST WAS NULL");
            return;
        }

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("ADD_GEOFENCE", "SUCCESSFUL TO CREATE LOGICAL GEOFENCES");
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


    private GeofencingRequest getGeofencingRequest () {
        if (geofenceArrayList == null || geofenceArrayList.size() == 0) {
            Toast.makeText(getApplicationContext(), "ArrayList is Empty", Toast.LENGTH_LONG).show();
            return null;
        }
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL );
        builder.addGeofences(geofenceArrayList);
        return builder.build();
    }


    private PendingIntent getGeofencePendingIntent () {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, MyBroadcastReceiver.class);
        intent.setAction(MyBroadcastReceiver.ACTION_RECEIVED_GEOFENCE_EVENT);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }


    public boolean checkPermissions() {
        int haveFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int haveBackgroundLocationPermission = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            haveBackgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        boolean result = (haveFineLocationPermission == PackageManager.PERMISSION_GRANTED) && (haveBackgroundLocationPermission == PackageManager.PERMISSION_GRANTED);
        Log.i(TAG, "CHECKED PERMISSIONS, RESULT IS: "+result);
        return result;
    }


    public void requestPermissions() {
        // Case 1: asking for 1st time - shouldShowRequestPermissionRationale returns false
        // Case 2: User denied previously without checking Don't ask again - shouldShowRequestPermissionRationale return true
        // Case 3: User denied previously with checking Don't ask again - shouldShowRequestPermissionRationale returns false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean flag;
            boolean flagFineLocation = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
            boolean flagBackgroundLocation = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                flagBackgroundLocation = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION);

            flag = flagFineLocation && flagBackgroundLocation;

            if (flag) {

                Log.i(TAG, "CASE 2: RATIONALE IS SHOWN");
                //case 2

                //TODO: if needed convert this to an AlertDialog

                Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                                else
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .show();

            }
            else {
                // case 1 or 3
                if (firstTimeOpen) {
                    Log.i(TAG, "CASE 1 : OPENED FOR FIRST TIME");
                    // case 1
                    firstTimeOpen = false;
                    sharedPreferences.edit().putBoolean(KEY_FIRST_TIME_OPEN, firstTimeOpen).apply();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                    else
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_REQUEST_CODE);
                }

                else {
                    // case 3
                    Log.i(TAG, "CASE 3: USER DENIED WITH CHECKING DON'T SHOW AGAIN");

                    AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                    alertDialog
                            .setTitle("Enable Location Permissions!!")
                            .setMessage("The application needs these permissions to work.\nPlease enable by clicking permissions -> Location.")
                            .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    userGoneToSettings = true;
                                    startActivity(intent);
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
                            .show();
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            }
            else {
                boolean result = false;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                        // Permission was granted.
                        result = true;
                    }
                }
                else {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Permission was granted.
                        result = true;
                    }
                }

                if(result) {
                    Log.i(TAG, "GOT THE PERMISSIONS");
                    requestUpdatesButton.setEnabled(true);
                    addGeofences.setEnabled(true);
//                requestLocationUpdates(null);
                }
                else {
                    Log.i(TAG, "DID NOT GET PERMISSIONS");
                    Log.i(TAG, "REQUESTING PERMISSIONS AGAIN FROM ON_REQUEST_PERMISSIONS_RESULT");
                    requestPermissions();
                }
            }
        }
    }


    public void checkAndGetLocationEnabled(){
        if(!isLocationEnabled()) {

            AlertDialog.Builder alertDialog;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                alertDialog = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
            else
                alertDialog = new AlertDialog.Builder(MainActivity.this);

            String msgLocationRequired = "This app requires Location to Track your Attendance. Please enable High Accuracy mode.";
            SpannableString msgLocationRequiredFormatted = new SpannableString(msgLocationRequired);
            ForegroundColorSpan fcsRED = new ForegroundColorSpan(Color.RED);
            msgLocationRequiredFormatted.setSpan(fcsRED, 67, 85, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            alertDialog
                    .setTitle("Enable Location!!")
                    .setMessage(msgLocationRequiredFormatted)
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            userGoneToSettings = true;
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
                    .show();
        }
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

        return (gps_enabled && network_enabled);
    }


}
