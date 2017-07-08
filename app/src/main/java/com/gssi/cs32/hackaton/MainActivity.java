package com.gssi.cs32.hackaton;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.util.ListenableList;
import com.gssi.cs32.hackaton.server.IServer;
import com.gssi.cs32.hackaton.server.Server;
import com.gssi.cs32.hackaton.util.MyLocationListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private MapView mMapView;
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;

    private float[] oldR = new float[0];
    private float[] oldI = new float[0];


    public static float swRoll;
    public static float swPitch;
    public static float swAzimuth;


    public static SensorManager mSensorManager;
    public static Sensor accelerometer;
    public static Sensor magnetometer;
    private static LocationManager locManager;
    private static Server server;

    public static float[] mAccelerometer = null;
    public static float[] mGeomagnetic = null;


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
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
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

        } else {

        }
    }



    private ActionBarDrawerToggle setupDrawerToggle() {

        // NOTE: Make sure you pass in a valid toolbar reference.  ActionBarDrawToggle() does not require it
        // and will not render the hamburger icon without it.

        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);

    }

    private void startQuestionActivity(){
        Intent intent = new Intent(this, QuestionActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();

        mMapView = (MapView) findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.OPEN_STREET_MAP, 42.35, 13.4, 14);
        mMapView.setMap(map);
        mMapView.forceLayout();


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQuestionActivity();
            }
        });

        try {
            InputStream mopsStream = getAssets().open("mops.geojson");
            InputStream qgisStream = getAssets().open("QGIS.geojson");
            AsyncTask<InputStream, Integer, IServer> task = new LoadServerTask().execute(mopsStream, qgisStream);
            server = (Server) task.get();


            long LOCATION_REFRESH_TIME = 10000;
            float LOCATION_REFRESH_DISTANCE = 10f;


            String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
                    .ACCESS_COARSE_LOCATION};
            boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED;
            boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED;

            if (!(permissionCheck1 && permissionCheck2)) {
                // If permissions are not already granted, request permission from the user.
                ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, 2);
            }

            locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            LocationListener locListener = new MyLocationListener(mMapView);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, locListener);

            Criteria crit = new Criteria();
            crit.setAccuracy(Criteria.ACCURACY_FINE);

            GraphicsOverlay go = new GraphicsOverlay(GraphicsOverlay.RenderingMode.STATIC);
            ListenableList<GraphicsOverlay> overlays = mMapView.getGraphicsOverlays();
            overlays.add(go);

            for (GeoElement geoElement : server.getMops()) {
                go.getGraphics().add((Graphic) geoElement);
            }
            mMapView.invalidate();
            mMapView.forceLayout();
            Log.i("MSG", "done");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onPause() {
        mMapView.pause();
        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, magnetometer);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    private float manhattanDist(float[] p1, float[] p2)
    {
        if (p1.length != p2.length) return 0;
        float out = 0;
        for (int i = 0; i < p1.length; i++)
        {
            out += Math.abs(p1[i] - p2[i]);
        }
        return out;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float EPS = 1e-6f;
        // onSensorChanged gets called for each sensor so we have to remember the values

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometer = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mAccelerometer != null && mGeomagnetic != null) {

            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);

            if (success) {
                if (oldR.length != 0 && manhattanDist(oldR, R) < EPS) return;
                oldR = R;
                oldI = I;
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // at this point, orientation contains the azimuth(direction), pitch and roll values.
                double azimuth = Math.toDegrees(orientation[0]);
                double pitch = 180 * orientation[1] / Math.PI;
                double roll = 180 * orientation[2] / Math.PI;
                if (locManager != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.i("ERR", "No permission");
                        return;
                    }
                    Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    GeoElement elem = getElement(loc, azimuth, server.getQgis(), 100.0d, 0.0d);
                    if (elem != null) {
                        int codGis = (int) elem.getAttributes().get((Object) "cod_gis");

                        Snackbar.make(mMapView,
                                Integer.toString(codGis), Snackbar.LENGTH_LONG).show();


                        Log.i("elem", Integer.toString(codGis));
                    }
                }
            }
        }
    }

    private GeoElement getElement(Location loc, double azimuth, List<GeoElement> elements, double distance, double prevDistance)
    {
        ArrayList<Point> pointsArray = new ArrayList<Point>(2);
        Point p1 = new Point(loc.getLongitude(), loc.getLatitude(), SpatialReference.create(4326));
        pointsArray.add(p1);

        try {
            Point p2 = GeometryEngine.moveGeodetic(p1, distance, new LinearUnit(LinearUnitId.METERS), azimuth, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);
            pointsArray.add(p2);
        } catch (ArcGISRuntimeException e) {
            Log.e("EXC", e.getAdditionalMessage());
            Log.e("EXC", e.getMessage());
        }


        // Create a segment with the 2 points created above
        PointCollection points = new PointCollection(pointsArray);
        PolylineBuilder pb = new PolylineBuilder(points);

        Polyline line = pb.toGeometry();
        // Filter out geometries that intersect the segment
        List<GeoElement> filtered = new ArrayList<GeoElement>();
        for (GeoElement g : elements)
        {
            if (!GeometryEngine.disjoint(line, g.getGeometry())) {
                filtered.add(g);
            }
        }

        switch (filtered.size()){
            case 0:
                if(prevDistance == 0 || distance < 5) {
                    return null;
                }
                else {
                    return getElement(loc, azimuth, elements, (distance + prevDistance) / 2, distance);
                }
            case 1:
                return filtered.get(0);
            default:
                return getElement(loc, azimuth, filtered, distance/2, distance);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
