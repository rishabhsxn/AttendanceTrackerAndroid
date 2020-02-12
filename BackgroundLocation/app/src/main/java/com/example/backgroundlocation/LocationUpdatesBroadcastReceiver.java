package com.example.backgroundlocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

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
                LocationResult result = LocationResult.extractResult(intent);

                if(result != null){
                    List<Location> locations = result.getLocations();
//                    Utils.setLocationUpdatesResult(context, locations);
//                    Utils.sendNotification(context, Utils.getLocationResultTitle(context, locations));
//                    Log.i(TAG, Utils.getLocationUpdatesResult(context));
                }
            }
        }
    }
}
