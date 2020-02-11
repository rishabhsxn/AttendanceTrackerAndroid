package com.example.geofencetransition;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;
import java.lang.String;

import static androidx.core.content.ContextCompat.getSystemService;


public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"OnReceive Called");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;

        }
        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        switch(geofenceTransition){
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Toast.makeText(context,"User entered Geofence",Toast.LENGTH_LONG).show();
               MapsActivity.drawGeofence("yellow");
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Toast.makeText(context,"User inside Geofence",Toast.LENGTH_LONG).show();
                MapsActivity.drawGeofence("green");
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Toast.makeText(context,"User Exited Geofence",Toast.LENGTH_LONG).show();
                MapsActivity.drawGeofence("red");
                break;

            default:
                Log.i(TAG,"ERROR OCCURRED");
                MapsActivity.drawGeofence("red");

            //TODO: default red ,enter yellow , dwell green ,exit red


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
    }


    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
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

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
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
