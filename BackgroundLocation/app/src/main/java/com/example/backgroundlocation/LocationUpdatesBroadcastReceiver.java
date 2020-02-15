package com.example.backgroundlocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.util.List;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {

    final String TAG = LocationUpdatesBroadcastReceiver.class.getSimpleName();
    final static String ACTION_PROCESS_UPDATES = "com.example.backgroundlocation.action.PROCESS_UPDATES";

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
//                    Utils.setLocationUpdatesResult(context, locations);
//                    Utils.sendNotification(context, Utils.getLocationResultTitle(context, locations));
//                    Log.i(TAG, Utils.getLocationUpdatesResult(context));
                }
                else
                    Log.i(TAG, "NULL RESULT RECEIVED\n");
            }
        }
    }
}
