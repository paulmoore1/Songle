package com.example.songle;

import android.util.Log;
import android.util.Xml;

import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Moore on 18-Oct-17.
 * Parses an input KML file into placemarks which can be used to create markers later.
 */

class XmlMapParser {
    private static final String TAG = "XmlMapParser";
    //Don't use namespaces
    private static final String ns = null;

    List<Placemark> parse(InputStream in) throws XmlPullParserException,
            IOException {
        try {
            Log.d(TAG, "Parsing started");
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readDocument(parser);
        } finally {
            in.close();
        }
    }

    private List<Placemark> readDocument(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        Log.d(TAG, "readDocument called");
        parser.require(XmlPullParser.START_TAG, ns, "kml");
        List<Placemark> placemarks = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            Log.v(TAG, "name = " + name);
            //Starts by looking for the Document tag
            if (name.equals("Document")) {
                placemarks = readPlacemarks(parser);
            } else {
                skip(parser);
            }
        }
        return placemarks;
    }

    private List<Placemark> readPlacemarks(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        Log.d(TAG, "readPlacemarks called");
        List<Placemark> placemarks = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, "Document");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            //Starts by looking for the first Placemark tag
            if (name.equals("Placemark")) {
                placemarks.add(readPlacemark(parser));
            } else {
                skip(parser);

            }
        }

        return placemarks;
    }

    private Placemark readPlacemark(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Placemark");
        String key = null;
        String description = null;
        LatLng location = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            switch (name) {
                case "name":
                    key = readName(parser);
                    break;
                case "description":
                    description = readDescription(parser);
                    break;
                case "Point":
                    location = readPoint(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Placemark(key, description, location);

    }

    private String readName(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String key = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return key;
    }

    private String readDescription(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "description");
        String description = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "description");
        return description;
    }

    private LatLng readPoint(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Point");
        LatLng location = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("coordinates")) {
                location = readLocation(parser);
            } else {
                skip(parser);
            }
        }
        return location;
    }

    private LatLng readLocation(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "coordinates");
        String coordinates = readText(parser);
        String[] split = coordinates.split(",");
        double[] latlng = new double[2];
        latlng[0] = Double.parseDouble(split[1]);
        latlng[1] = Double.parseDouble(split[0]);
        LatLng location = new LatLng(latlng[0], latlng[1]);
        parser.require(XmlPullParser.END_TAG, ns, "coordinates");
        return location;
    }


    private String readText(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
