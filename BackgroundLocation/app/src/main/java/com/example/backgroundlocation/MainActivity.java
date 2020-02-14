package com.example.backgroundlocation;

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
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    final String TAG = MainActivity.class.getSimpleName();


    TextView latTextView;
    TextView lonTextView;
    TextView locationCountTextView;
    Button requestUpdatesButton;
    Button removeUpdatesButton;

    final int LOCATION_REQUEST_CODE = 1000;
    final int UPDATE_INTERVAL = 5 * 1000;
    final int MAX_WAIT_TIME = 2 * UPDATE_INTERVAL;

    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;

    SharedPreferences sharedPreferences;

    int locationCount = 0;

    Snackbar locationSnackbar;


    boolean firstTimeOpen;
    boolean userGoneToSettings = false;
    final String KEY_FIRST_TIME_OPEN = "firstTimeOpen";

    // TODO (DONE): implement onResume so that checkPermission will be checked when user come back from settings
    // TODO (DONE): issue - request button is enabled from starting, even when permissions are not given
    // TODO (DONE): check isLocationEnabled when the RequestUpdates button is clicked, if not show Snackbar

    // TODO: check dynamically if the location is enabled and set to High Accuracy Mode - when locations are fetched and when request update button is pressed
    // TODO: implement and show locations on Log (in Background) and in TextView when the application is opened (onResume)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latTextView = (TextView) findViewById(R.id.latTextView);
        lonTextView = (TextView) findViewById(R.id.lonTextView);
        locationCountTextView = (TextView) findViewById(R.id.locationCountTextView);

        requestUpdatesButton = (Button) findViewById(R.id.requestUpdatesButton);
        removeUpdatesButton = (Button) findViewById(R.id.removeUpdatesButton);
        requestUpdatesButton.setEnabled(false);
        removeUpdatesButton.setEnabled(false);

        setupLocationSnackbar();

        sharedPreferences = getSharedPreferences("com.example.backgroundlocation", MODE_PRIVATE);
        firstTimeOpen = sharedPreferences.getBoolean(KEY_FIRST_TIME_OPEN, true);  // (done) remember to make it false after checking and save


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationRequest();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermissions())
                requestPermissions();
            else
                requestUpdatesButton.setEnabled(true);
        }
        else
            requestUpdatesButton.setEnabled(true);

    }



    @Override
    protected void onResume() {
        super.onResume();

        if(userGoneToSettings){
            userGoneToSettings = false;
            // check if now the permissions are Granted, if not then again request
            if(checkPermissions()) {

                if (isLocationEnabled()) {
                    requestUpdatesButton.setEnabled(true);
                    if(locationSnackbar.isShown())
                        locationSnackbar.dismiss();
                }
                else {
                    // TODO (DONE): can show, that the user has still not enabled the location
                    locationSnackbar.show();
                }

            }
            else{
                requestPermissions();
            }

        }
    }



    public void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setMaxWaitTime(MAX_WAIT_TIME);

        // TODO: set FastestInterval if required
    }



    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }



    public void startLocationUpdates(){

//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
        Log.i(TAG, "REQUESTING LOCATION UPDATES");
        // TODO: remember to remove locationUpdates
    }



    public void requestUpdates(View view){
        checkAndGetLocationEnabled("REQUEST_UPDATES BUTTON");
        if(isLocationEnabled()) {
            startLocationUpdates();
            requestUpdatesButton.setEnabled(false);
            removeUpdatesButton.setEnabled(true);
        }
    }



    public void removeUpdates(View view){
        Log.i(TAG, "REMOVED LOCATION UPDATES");
        removeUpdatesButton.setEnabled(false);
        requestUpdatesButton.setEnabled(true);
//        fusedLocationProviderClient.removeLocationUpdates(getPendingIntent());
    }



    // TODO (DONE): add the code to check permission for ACCESS_BACKGROUND_LOCATION when needed
    public boolean checkPermissions() {
        int haveFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int haveBackgroundLocationPermission = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            haveBackgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        boolean result = (haveFineLocationPermission == PackageManager.PERMISSION_GRANTED) && (haveBackgroundLocationPermission == PackageManager.PERMISSION_GRANTED);
        Log.i(TAG, "CHECKED PERMISSIONS, RESULT IS: "+result);
        return result;
    }



    // TODO (DONE): add the permission request for ACCESS_BACKGROUND_LOCATION when needed
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



    public void checkAndGetLocationEnabled(String calledBy){
        if(!isLocationEnabled()) {

            AlertDialog.Builder alertDialog;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                alertDialog = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
            else
                alertDialog = new AlertDialog.Builder(MainActivity.this);

//            isAlertVisible = true;
            Log.i("CHECK_AND_GET", "it is called by "+calledBy);
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

}