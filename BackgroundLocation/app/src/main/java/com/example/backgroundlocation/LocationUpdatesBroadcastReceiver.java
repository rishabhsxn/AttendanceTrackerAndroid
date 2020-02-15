package com.example.backgroundlocation;

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

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.LocationResult;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {

    final String TAG = LocationUpdatesBroadcastReceiver.class.getSimpleName();
    final static String ACTION_PROCESS_UPDATES = "com.example.backgroundlocation.action.PROCESS_UPDATES";
    final static String CHANNEL_ID = "channel_01";


    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null){
            final String action = intent.getAction();

            if(ACTION_PROCESS_UPDATES.equals(action)){
                // fetch location
                Log.i(TAG, "ACTION STRING IS MATCHED");
                LocationResult result = LocationResult.extractResult(intent);

                if(result != null){
                    List<Location> locations = result.getLocations();
                    int count = 1;
                    Log.i(TAG, "No. of locations received: "+locations.size());
                    for(Location location: locations){
                        Log.i(TAG, "COUNT: "+count);
                        count++;
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        double accuracy = location.getAccuracy();
                        Log.i(TAG,"Latitude: "+lat);
                        Log.i(TAG,"Longitude: "+lon);
                        Log.i(TAG,"Accuracy: "+accuracy+"\n");

                    }

                    sendNotification(context, getLocationResultTitle(context, locations));
//                    Utils.setLocationUpdatesResult(context, locations);
//                    Utils.sendNotification(context, Utils.getLocationResultTitle(context, locations));
//                    Log.i(TAG, Utils.getLocationUpdatesResult(context));
                }
                else
                    Log.i(TAG, "NULL RESULT RECEIVED\n");
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
                .setContentTitle("Location update")
                .setContentText(notificationDetails)
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

    static String getLocationResultTitle(Context context, List<Location> locations) {
        String numLocationsReported = context.getResources().getQuantityString(
                R.plurals.num_locations_reported, locations.size(), locations.size());
        return numLocationsReported + ": " + DateFormat.getDateTimeInstance().format(new Date());
    }
}
