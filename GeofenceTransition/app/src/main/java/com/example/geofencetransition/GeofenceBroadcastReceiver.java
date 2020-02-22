package com.example.geofencetransition;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.lang.String;

import static com.example.geofencetransition.MapsActivity.drawGeofence;
import static com.example.geofencetransition.MapsActivity.geofenceArrayList;


public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();
    static final String ACTION_RECEIVE_GEOFENCE = "com.example.geofencetransition.action.RECEIVE_GEOFENCE";
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.i(TAG, "OnReceive Called");

//        String action = intent.getAction();
//        Log.i(TAG, action);
        geofencingClient = LocationServices.getGeofencingClient(context);



        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.i("TRIGGERED GEOFENCE ", geofencingEvent.getTriggeringGeofences().toString());
        String id = geofencingEvent.getTriggeringGeofences().get(0).getRequestId();


        int index;

        index = MapsActivity.geofenceIDs.indexOf(id);

//        LatLng centre = geofencingEvent.
        String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, geofencingEvent.getTriggeringGeofences());
        Log.i(TAG, "GEOFENCE_TRANS_DETAILS: " + geofenceTransitionDetails);

        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Toast.makeText(context, "User entered Geofence ".concat(id), Toast.LENGTH_LONG).show();
                drawGeofence(Color.YELLOW, index);

                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Toast.makeText(context, "User inside Geofence ".concat(id), Toast.LENGTH_LONG).show();
                drawGeofence(Color.GREEN, index);

                Toast.makeText(context,"REMOVED GEOFENCE", Toast.LENGTH_LONG).show();
                geofencingClient.removeGeofences(MapsActivity.geofenceIDs);

                // set a timer of 20sec, then add geofences
                new CountDownTimer(60*1000, 5000){
                    public void onTick(long milliSecondsUntilDone){
//                        Toast.makeText(context, "no geofence",Toast.LENGTH_LONG).show();
                    }

                    public void onFinish(){

                        Toast.makeText(context, "Timer Finished, adding geofence", Toast.LENGTH_LONG).show();

                        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(context))
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.i("ADD_GEOFENCE", "SUCCESSFUL TO CREATE LOGICAL GEOFENCES");
//
                                        // Geofences created, now draw those on Map
                                        Toast.makeText(context, "Geofence created successfully", Toast.LENGTH_LONG).show();
                                        for (int i = 0; i < geofenceArrayList.size(); i++)
                                        {
                                            drawGeofence(Color.RED, i);
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                        Log.i("ADD_GEOFENCE", "FAILED TO CREATE LOGICAL GEOFENCES");
                                        Toast.makeText(context,"Failed to create geofence",Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }.start();

                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Toast.makeText(context, "User Exited Geofence ".concat(id), Toast.LENGTH_LONG).show();
                drawGeofence(Color.RED, index);
                break;

            default:
                Log.i(TAG, "ERROR OCCURRED");
//                MapsActivity.drawGeofence("red", "Library");
//                MapsActivity.drawGeofence("red", "Old mess");
        }


    }

    private GeofencingRequest getGeofencingRequest () {
        if (geofenceArrayList == null || geofenceArrayList.size() == 0) {
//            Toast.makeText(getApplicationContext(), "ArrayList is Empty", Toast.LENGTH_LONG).show();
            return null;
        }
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL ); //to reduce spams of continued stay in location
        builder.addGeofences(geofenceArrayList);
        return builder.build();
    }


    private PendingIntent getGeofencePendingIntent (Context context) {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }


    private String getGeofenceTransitionDetails(int geofenceTransition, List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }


    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "ENTERED GEOFENCE";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "EXITED GEOFENCE";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "INSIDE GEOFENCE";
            default:
                return null;
        }
    }

}

// Test that the reported transition was of interest.
//        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ||geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ) {
//
//            // Get the geofences that were triggered. A single event can trigger
//            // multiple geofences.
//            List<Geofence> triggeringGeofences;
//            triggeringGeofences = geofencingEvent.getTriggeringGeofences();
//
//            // Get the transition details as a String.
//            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition,
//                    triggeringGeofences);
//
//            // Send notification and log the transition details.
//           // sendNotification(geofenceTransitionDetails);
//            //Toast.makeText(C, geofenceTransitionDetails,Toast.LENGTH_LONG).show();
//            Log.i("USER AT",geofenceTransitionDetails);
//        } else {
//            // Log the error.
//            Log.i(TAG,"ERROR OCCURRED");
//        }
//    }



    /*
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
//    private void sendNotification(String notificationDetails) {
//        // Get an instance of the Notification manager
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(android.content.Context,NotificationManager.class);
//
//        // Android O requires a Notification Channel.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = Integer.toString(R.string.app_name);
//            // Create the channel for the notification
//            NotificationChannel mChannel =
//                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
//
//            // Set the Notification Channel for the Notification Manager.
//            mNotificationManager.createNotificationChannel(mChannel);
//        }
//
//        // Create an explicit content Intent that starts the main Activity.
//        Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);
//
//        // Construct a task stack.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//
//        // Add the main Activity to the task stack as the parent.
//        stackBuilder.addParentStack(MapsActivity.class);
//
//        // Push the content Intent onto the stack.
//        stackBuilder.addNextIntent(notificationIntent);
//
//        // Get a PendingIntent containing the entire back stack.
//        PendingIntent notificationPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // Get a notification builder that's compatible with platform versions >= 4
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//
//        // Define the notification settings.
//        builder.setSmallIcon(R.drawable.ic_launcher)
//                // In a real app, you may want to use a library like Volley
//                // to decode the Bitmap.
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
//                        R.drawable.ic_launcher))
//                .setColor(Color.RED)
//                .setContentTitle(notificationDetails)
//                .setContentText(getString(R.string.geofence_transition_notification_text))
//                .setContentIntent(notificationPendingIntent);
//
//        // Set the Channel ID for Android O.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            builder.setChannelId(CHANNEL_ID); // Channel ID
//        }
//
//        // Dismiss notification once the user touches it.
//        builder.setAutoCancel(true);
//
//        // Issue the notification
//        mNotificationManager.notify(0, builder.build());
//    }

