package cs32.gssi.hackaton.functionalities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by A.Balestrucci on 04/06/2017.
 */

public class GeoCoord {

    @SuppressWarnings("finally")
    public static String getAddressByGpsCoordinates(String lat, String lng)
            throws MalformedURLException, IOException, ParseException {


        URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng="
                + lat + "," + lng + "&sensor=true");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        String formattedAddress = "";

        try {
            BufferedReader reader;
            String result;
            String line;
            org.json.simple.parser.JSONParser parser;
            JSONObject rsp;
            try (InputStream in = url.openStream()) {
                reader = new BufferedReader(new InputStreamReader(in));
            }
            line = reader.readLine();
            result = line;
            while ((line = reader.readLine()) != null) {
                result += line;
            }

            parser = new JSONParser();
            rsp = (JSONObject) parser.parse(result);

            if (rsp.containsKey("results")) {
                JSONArray matches = (JSONArray) rsp.get("results");
                JSONObject data = (JSONObject) matches.get(0); //TODO: check if idx=0 exists
                formattedAddress = (String) data.get("formatted_address");
            }

            return "";
        } finally {
            urlConnection.disconnect();
            return formattedAddress;
        }
    }
}
