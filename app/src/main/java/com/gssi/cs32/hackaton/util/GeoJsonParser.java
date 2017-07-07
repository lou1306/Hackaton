package com.gssi.cs32.hackaton.util;

/**
 * Created by luca on 06/07/17.
 * Adapted from https://github.com/Esri/arcgis-runtime-demo-java
 */


import android.graphics.Color;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.ImmutablePartCollection;
import com.esri.arcgisruntime.geometry.MultipartBuilder;
import com.esri.arcgisruntime.geometry.Multipoint;
import com.esri.arcgisruntime.geometry.MultipointBuilder;
import com.esri.arcgisruntime.geometry.Part;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointBuilder;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.PolygonBuilder;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.PolylineBuilder;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.internal.jni.CoreRequest;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * A parser that reads data in <a href="http://geojson.org">GeoJSON</a> format, and returns
 * a collection of {@link Feature} or {@link Geometry}.
 *
 * <p>
 * To parse a
 * <a href="http://geojson.org/geojson-spec.html#feature-collection-objects">FeatureCollection</a>
 *  use {@link #parseFeatures(String)}; to parse a
 * <a href="http://geojson.org/geojson-spec.html#feature-collection-objects">GeometryCollection</a>
 *  use {@link #parseGeometries(String)}.
 *
 * <p>
 * Limitations:
 * <ul>
 * <li> No support for the GeoJSON coordinate reference system. All input geometries are
 * assumed to be in CRS84.
 * </ul>
 * @since 10.2.4
 */
public final class GeoJsonParser {

    // symbology to be used for all the features
    private Symbol symbol = null;

    private Symbol polygonSymbol;


    // dependency on the Jackson parser library to parse JSON
    private final ObjectMapper mapper = new ObjectMapper();

    // geometries in GeoJSON are assumed to be in CRS84 (Esri Wkid = 4326)
    private final SpatialReference inSR = SpatialReference.create(4326);

    // output CRS can be configured to be different
    private SpatialReference outSR = null;

    // field names defined in the GeoJson spec
    private final static String FIELD_COORDINATES = "coordinates";
    private final static String FIELD_FEATURE = "Feature";
    private final static String FIELD_FEATURES = "features";
    private final static String FIELD_FEATURE_COLLECTION = "FeatureCollection";
    private final static String FIELD_GEOMETRY = "geometry";
    private final static String FIELD_GEOMETRIES = "geometries";
    private final static String FIELD_GEOMETRY_COLLECTION = "GeometryCollection";
    private final static String FIELD_PROPERTIES = "properties";
    private final static String FIELD_TYPE = "type";

    private enum GeometryType {
        POINT("Point"),
        MULTI_POINT("MultiPoint"),
        LINE_STRING("LineString"),
        MULTI_LINE_STRING("MultiLineString"),
        POLYGON("Polygon"),
        MULTI_POLYGON("MultiPolygon");

        private final String val;

        GeometryType(String val) {
            this.val = val;
        }

        public static GeometryType fromString(String val) {
            for (GeometryType type : GeometryType.values()) {
                if (type.val.equals(val)) {
                    return type;
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Public methods
    // ------------------------------------------------------------------------


    public GeoJsonParser() {
        SimpleLineSymbol outlineSymbol =
                new SimpleLineSymbol(
                        SimpleLineSymbol.Style.SOLID,
                        Color.argb(255, 0, 0, 128), 1.0f);
        this.polygonSymbol =
                new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID,
                        Color.argb(255, 0, 0, 128),
                        outlineSymbol);

    }

    public GeoJsonParser setSymbol(Symbol symbol) {
        this.symbol = symbol;
        return this;
    }

    public GeoJsonParser setOutSpatialReference(SpatialReference outSR) {
        this.outSR = outSR;
        return this;
    }

    public List<GeoElement> parseFeatures(File file) {
        try {
            JsonParser parser = new JsonFactory().createJsonParser(file);
            return parseFeatures(parser);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<GeoElement> parseFeatures(String str) {
        try {
            JsonParser parser = new JsonFactory().createJsonParser(str);
            return parseFeatures(parser);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Geometry> parseGeometries(File file) {
        try {
            JsonParser parser = new JsonFactory().createJsonParser(file);
            return parseGeometries(parser);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Geometry> parseGeometries(String str) {
        try {
            JsonParser parser = new JsonFactory().createJsonParser(str);
            return parseGeometries(parser);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // ------------------------------------------------------------------------
    // Private methods
    // ------------------------------------------------------------------------

    private List<GeoElement> parseFeatures(JsonParser parser) {
        try {
            JsonNode node = mapper.readTree(parser);
            String type = node.path(FIELD_TYPE).getTextValue();
            if (type.equals(FIELD_FEATURE_COLLECTION)) {
                ArrayNode jsonFeatures = (ArrayNode) node.path(FIELD_FEATURES);
                return parseFeatures(jsonFeatures);
            } else if (type.equals(FIELD_GEOMETRY_COLLECTION)) {
                ArrayNode jsonFeatures = (ArrayNode) node.path(FIELD_GEOMETRIES);
                List<Geometry> geometries = parseGeometries(jsonFeatures);
                List<GeoElement> features = new LinkedList<GeoElement>();

                for (Geometry g : geometries) {
                    features.add(new Graphic(g, symbol));
                }
                return features;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return Collections.emptyList();
    }

    private List<GeoElement> parseFeatures(ArrayNode jsonFeatures) {
        List<GeoElement> features = new LinkedList<GeoElement>();
        for (JsonNode jsonFeature : jsonFeatures) {
            String type = jsonFeature.path(FIELD_TYPE).getTextValue();
            if (!FIELD_FEATURE.equals(type)) {
                continue;
            }
            Geometry g = parseGeometry(jsonFeature.path(FIELD_GEOMETRY));
            if (outSR != null && outSR.getWkid() != 4326) {
                g = GeometryEngine.project(g, outSR);
            }
            Map<String, Object> attributes = parseProperties(jsonFeature.path(FIELD_PROPERTIES));
            Symbol s = symbol;
            if (g.getGeometryType() == com.esri.arcgisruntime.geometry.GeometryType.POLYGON)
            {
                s = polygonSymbol;
            }
            GeoElement ge = new Graphic(g, attributes, s);
            features.add(ge);
        }
        return features;
    }

    private List<Geometry> parseGeometries(JsonParser parser) {
        try {
            JsonNode node = mapper.readTree(parser);
            String type = node.path(FIELD_TYPE).getTextValue();
            if (type.equals(FIELD_GEOMETRY_COLLECTION)) {
                ArrayNode jsonFeatures = (ArrayNode) node.path(FIELD_GEOMETRIES);
                return parseGeometries(jsonFeatures);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return Collections.emptyList();
    }

    private List<Geometry> parseGeometries(ArrayNode jsonGeometries) {
        List<Geometry> geometries = new LinkedList<Geometry>();
        for (JsonNode jsonGeometry : jsonGeometries) {
            Geometry g = parseGeometry(jsonGeometry);
            if (outSR != null && outSR.getWkid() != 4326) {
                g = GeometryEngine.project(g, outSR);
            }
            geometries.add(g);
        }
        return geometries;
    }

    private Map<String, Object> parseProperties(JsonNode node) {
        Map<String, Object> properties = new HashMap<String, Object>();
        Iterator<Map.Entry<String, JsonNode>> propertyInterator = node.getFields();
        while (propertyInterator.hasNext()) {
            Map.Entry<String, JsonNode> property = propertyInterator.next();
            JsonNode jsonValue = property.getValue();
            if (jsonValue.isInt()) {
                properties.put(property.getKey(), property.getValue().asInt());
            } else if (jsonValue.isDouble()) {
                properties.put(property.getKey(), property.getValue().asDouble());
            } else if (jsonValue.isTextual()) {
                properties.put(property.getKey(), property.getValue().asText());
            }
        }
        return properties;
    }

    /**
     * { "type": "Point", "coordinates": [100.0, 0.0] }
     * @return a geometry.
     * @throws IOException
     * @throws JsonParseException
     */
    private Geometry parseGeometry(JsonNode node) {
        GeometryType type = GeometryType.fromString(node.path(FIELD_TYPE).getTextValue());
        return parseCoordinates(type, node.path(FIELD_COORDINATES));
    }

    private Geometry parseCoordinates(GeometryType type, JsonNode node) {
        Geometry g = null;
        switch (type) {
            default:
            case POINT:
                g = parsePointCoordinates(node);
                break;
            case MULTI_POINT:
                g = parseMultiPointCoordinates(node);
                break;
            case LINE_STRING:
                g = parseLineStringCoordinates(node);
                break;
            case MULTI_LINE_STRING:
                g = parseMultiLineStringCoordinates(node);
                break;
            case POLYGON:
                g = parsePolygonCoordinates(node);
                break;
            case MULTI_POLYGON:
                g = parseMultiPolygonCoordinates(node);
                break;
        }
        return g;
    }

    /**
     * Parses a point
     * Example:
     * [101.0, 0.0].
     * @return a point.
     * @throws Exception
     */
    private Point parsePointCoordinates(JsonNode node) {
        PointBuilder builder = new PointBuilder(node.get(0).asDouble(), node.get(1).asDouble());
        if (node.size() == 3) {
            builder.setZ(node.get(2).asDouble());
        }
        return builder.toGeometry();
    }

    /**
     * Parses a multipoint
     * Example:
     * [ [100.0, 0.0], [101.0, 1.0] ].
     * @return a multipoint.
     * @throws Exception
     */
    private Multipoint parseMultiPointCoordinates(JsonNode node) {

        ArrayNode jsonPoints = (ArrayNode) node;
        LinkedList<Point> points = new LinkedList<Point>();
        for (JsonNode jsonPoint : jsonPoints) {
            Point point = parsePointCoordinates(jsonPoint);
            points.add(point);
        }
        MultipointBuilder builder = new MultipointBuilder(points);
        return builder.toGeometry();
    }

    /**
     * Parses a line string
     * Example:
     * [ [100.0, 0.0], [101.0, 1.0] ].
     * @return a polyline.
     * @throws Exception
     */
    private Polyline parseLineStringCoordinates(JsonNode node) {

        PointCollection collection = new PointCollection(new LinkedList<Point>());
        PolylineBuilder builder = new PolylineBuilder(collection);

        //boolean first = true;
        ArrayNode pointsArray = (ArrayNode) node;
        for (JsonNode point : pointsArray) {
            Point p = parsePointCoordinates(point);
            builder.addPoint(p);
//            if (first) {
//                g.startPath(p);
//                //first = false;
//            } else {
//                g.lineTo(p);
//            }
        }

        return builder.toGeometry();
    }

    /**
     * Parses a multi line string
     * Example:
     * [
     *   [ [100.0, 0.0], [101.0, 1.0] ],
     *   [ [102.0, 2.0], [103.0, 3.0] ]
     * ]
     * @return a polyline
     * @throws Exception
     */
    private Polyline parseMultiLineStringCoordinates(JsonNode node) {
        PointCollection collection = new PointCollection(new LinkedList<Point>());
        PolylineBuilder builder = new PolylineBuilder(collection);

        ArrayNode jsonLines = (ArrayNode) node;
        for (JsonNode jsonLine : jsonLines) {
            Polyline line = parseLineStringCoordinates(jsonLine);
            builder.addPoints(line.getParts().getPartsAsPoints());
        }
        return builder.toGeometry();
    }

    /**
     * Example:
     * [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
     * @return a polygon
     * @throws JsonParseException
     * @throws IOException
     */
    private Polygon parseSimplePolygonCoordinates(JsonNode node) {
        PointCollection collection = new PointCollection(new LinkedList<Point>());
        PolygonBuilder builder = new PolygonBuilder(collection);
        ArrayNode points = (ArrayNode) node;
        for (JsonNode point : points) {
            Point p = parsePointCoordinates(point);
            builder.addPoint(p);
        }
        return builder.toGeometry();
    }

    /**
     * Parses a polygon string
     * Example:
     * without holes:
     * [
     *   [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
     * ]
     *
     * with holes:
     * [
     *   [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],
     *   [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2] ]
     * ]
     * @return a polygon
     * @throws Exception
     */
    private Polygon parsePolygonCoordinates(JsonNode node) {
        PointCollection collection = new PointCollection(new LinkedList<Point>());
        PolygonBuilder builder = new PolygonBuilder(collection);
        ArrayNode jsonPolygons = (ArrayNode) node;
        for (JsonNode jsonPolygon : jsonPolygons) {
            Polygon simplePolygon = parseSimplePolygonCoordinates(jsonPolygon);
            builder.addPart();
            builder.addPoints(simplePolygon.getParts().getPartsAsPoints());
        }
        return builder.toGeometry();
    }

    /**
     * Parses a multi polygon string
     * Example:
     *  [
     *   [[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]],
     *   [[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]],
     *    [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]
     *  ]
     * @return a polygon
     * @throws Exception
     */
    private Polygon parseMultiPolygonCoordinates(JsonNode node) {
        PointCollection collection = new PointCollection(new LinkedList<Point>());
        PolygonBuilder builder = new PolygonBuilder(collection);
        ArrayNode jsonPolygons = (ArrayNode) node;
        for (JsonNode jsonPolygon : jsonPolygons) {
            Polygon simplePolygon = parsePolygonCoordinates(jsonPolygon);
            builder.addPart();
            builder.addPoints(simplePolygon.getParts().getPartsAsPoints());
        }
        return builder.toGeometry();
    }
}
