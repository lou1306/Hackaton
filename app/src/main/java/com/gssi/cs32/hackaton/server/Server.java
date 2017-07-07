package com.gssi.cs32.hackaton.server;

import com.esri.arcgisruntime.geometry.Geometry;

import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.internal.jni.CoreSimpleMarkerSymbol;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.gssi.cs32.hackaton.models.Building;
import com.gssi.cs32.hackaton.util.GeoJsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.io.File;
import java.util.Objects;

/**
 * Created by luca on 01/06/17.
 */

public class Server implements IServer {

    private List<GeoElement> mops;

    public List<GeoElement> getMops() {
        return mops;
    }

    public List<GeoElement> getQgis() {
        return qgis;
    }

    private List<GeoElement> qgis;


    public Server(String mopsStr, String qgisStr) {
        GeoJsonParser parser = new GeoJsonParser();
        SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0, 10);
        parser.setSymbol(markerSymbol);
        this.mops = parser.parseFeatures(mopsStr);
        this.qgis = parser.parseFeatures(qgisStr);
        android.util.Log.i("MOPS geoJson", Integer.toString(mops.size()));
        android.util.Log.i("QGIS geoJson", Integer.toString(qgis.size()));
    }

    @Override
    public List<Building> GetBuildings() {


        return null;
    }
}
