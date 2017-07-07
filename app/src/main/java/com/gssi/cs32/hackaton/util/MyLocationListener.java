package com.gssi.cs32.hackaton.util;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by luca on 07/07/17.
 */

public class MyLocationListener implements LocationListener {
    private static final int THRESHOLD = 50;
    private Location previous = null;

    @Override
    public void onLocationChanged(Location location) {
        if(previous == null)
        {
            previous = location;
        }
        else
        {
            if(previous.distanceTo(location) > THRESHOLD)
            {
                previous = location;
                Log.i("LOCATION", location.toString());
                // TODO update polygons
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
