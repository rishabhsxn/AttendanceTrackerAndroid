package com.example.backgroundlocationgeofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationResult;

import java.util.List;

// TODO: check the actions in the google project
public class MyBroadcastReceiver extends BroadcastReceiver {

    final String TAG = MyBroadcastReceiver.class.getSimpleName();
    final static String ACTION_RECEIVED_LOCATION = "com.example.backgroundlocationgeofence.action.RECEIVED_LOCATION";
    final static String ACTION_RECEIVED_GEOFENCE_EVENT = "com.example.backgroundlocationgeofence.action.RECEIVED_GEOFENCE_EVENT";

    final static String CHANNEL_ID = "channel_01";

    static String locationDetails = "No locations";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null){
            String action = intent.getAction();

            if(action != null){

                switch (action){
                    case ACTION_RECEIVED_LOCATION:
                        Log.i("VOLD", "ACTION MATCHED: "+ACTION_RECEIVED_LOCATION);
                        LocationResult locationResult = LocationResult.extractResult(intent);

                        if(locationResult != null){
                            List<Location> locations = locationResult.getLocations();
                            int count = locations.size();
                            Location lastLocation = locations.get(count-1);
                            locationDetails = "Locations received: "+count+"\nLattitude: "+lastLocation.getLatitude()+"\nLongitude: "+lastLocation.getLongitude();
                        }
                        else
                            locationDetails = "No locations received";

                        Toast.makeText(context, locationDetails, Toast.LENGTH_SHORT).show();
                        Log.i("LOCATION_RECEIVER", locationDetails);

                        break;


                    case ACTION_RECEIVED_GEOFENCE_EVENT:
                        Log.i("VOLD", "ACTION MATCHED: "+ACTION_RECEIVED_GEOFENCE_EVENT);

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

                        String notificationText= "";


                        switch (geofenceTransition) {
                            case Geofence.GEOFENCE_TRANSITION_ENTER:
//                                Toast.makeText(context, "User entered Geofence ".concat(id), Toast.LENGTH_LONG).show();
                                notificationText = "User entered Geofence ".concat(id);
                                break;


                            case Geofence.GEOFENCE_TRANSITION_DWELL:
//                                Toast.makeText(context, "User dwell Geofence ".concat(id), Toast.LENGTH_LONG).show();
                                notificationText = "User dwell Geofence ".concat(id);
                                break;


                            case Geofence.GEOFENCE_TRANSITION_EXIT:
//                                Toast.makeText(context, "User Exited Geofence ".concat(id), Toast.LENGTH_LONG).show();
                                notificationText = "User Exited Geofence ".concat(id);
                                break;
                        }


                        // send the notification
                        sendNotification(context, notificationText);

                        break;
                }
            }
        }
    }

    static void sendNotification(Context context, String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context, MainActivity.class);

        notificationIntent.putExtra("from_notification", true);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(locationDetails)
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);

            // Channel ID
            builder.setChannelId(CHANNEL_ID);
        }

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


}
