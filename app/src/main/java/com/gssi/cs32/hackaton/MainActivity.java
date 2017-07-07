package com.gssi.cs32.hackaton;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.internal.jni.CoreRequest;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.location.AndroidLocationDataSource;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.UniqueValueRenderer;
import com.esri.arcgisruntime.util.ListenableList;
import com.gssi.cs32.hackaton.server.IServer;
import com.gssi.cs32.hackaton.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cs32.gssi.hackaton.R;

public class MainActivity extends AppCompatActivity {
    private MapView mMapView;
    private int requestCode = 2;

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
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
        {

        }
        else
        {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.mapView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        try {
            InputStream mopsStream = getAssets().open("mops.geojson");
            InputStream qgisStream = getAssets().open("QGIS.geojson");
            AsyncTask<InputStream, Integer, IServer> task = new LoadServerTask().execute(mopsStream, qgisStream);
            Server server = (Server) task.get();


            long LOCATION_REFRESH_TIME = 10000;
            float LOCATION_REFRESH_DISTANCE = 150f;

            Location loc = new Location("");
            loc.setLatitude(42.35);
            loc.setLongitude(13.5);

            String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
                    .ACCESS_COARSE_LOCATION};
            boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED;
            boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED;

            if (!(permissionCheck1 && permissionCheck2)) {
                // If permissions are not already granted, request permission from the user.
                ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
            }




            Log.i("LOC", loc.toString());

            ArcGISMap map = new ArcGISMap(Basemap.Type.OPEN_STREET_MAP, loc.getLatitude(), loc.getLongitude(), 15);
            mMapView.setMap(map);

            AndroidLocationDataSource lds = new AndroidLocationDataSource(this);

            Criteria crit = new Criteria();
            crit.setAccuracy(Criteria.ACCURACY_FINE);

            lds.requestLocationUpdates(crit, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE);

            GraphicsOverlay go = new GraphicsOverlay(GraphicsOverlay.RenderingMode.STATIC);
            ListenableList<GraphicsOverlay> overlays = mMapView.getGraphicsOverlays();
            overlays.add(go);

            for (GeoElement geoElement : server.getMops()){
                go.getGraphics().add((Graphic) geoElement);
            }
            mMapView.invalidate();
            mMapView.forceLayout();
//            SimpleFillSymbol symbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.RED, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 2));
//            UniqueValueRenderer renderer = new UniqueValueRenderer();
//            renderer.setDefaultSymbol(symbol);
//            go.setRenderer(renderer);
            Log.i("MSG", "done");




        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause(){
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }

}
