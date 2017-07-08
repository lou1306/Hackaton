package com.gssi.cs32.hackaton.util;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.view.MapView;

/**
 * Created by luca on 07/07/17.
 */

public class MyLocationListener implements LocationListener {
    private static final int THRESHOLD = 50;
    private static SpatialReference WGS84 = SpatialReference.create(4326);
    private Location previous = null;
    private MapView mview;

    public MyLocationListener(MapView mv)
    {
        this.mview = mv;
    }


    @Override
    public void onLocationChanged(Location location) {
        mview.setViewpointCenterAsync(new Point(location.getLongitude(), location.getLatitude(), WGS84));
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
