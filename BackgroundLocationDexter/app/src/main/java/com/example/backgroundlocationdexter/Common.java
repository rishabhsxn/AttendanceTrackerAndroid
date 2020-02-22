package com.example.backgroundlocationdexter;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

public class Common {
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "LocationUpdateEnable";

    public static String getLocationText(Location location) {

        return location == null ? "Unknown Location" : new StringBuilder()
                .append(location.getLatitude())
                .append("/")
                .append(location.getLongitude())
                .toString();
    }

    public static CharSequence getLocationTitle(MyBackgroundService myBackgroundService) {
        return String.format("Location Updated: %1$s", DateFormat.getDateInstance().format(new Date()));
    }

    public static void setRequestingLocationUpdates(Context context, boolean value) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, value)
                .apply();
    }

    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);

    }
}
