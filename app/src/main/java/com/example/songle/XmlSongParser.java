package com.example.songle;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Moore on 18-Oct-17.
 * Parses an XML containing (hopefully) songs.
 */

class XmlSongParser {
    private static final String TAG = "XmlSongParser";
    private final SharedPreference sharedPreference;
    private final Context mContext;
    //Don't use namespaces
    private static final String ns = null;

    XmlSongParser(Context context){
        this.mContext = context;
        sharedPreference = new SharedPreference(context);
    }

    void parse(InputStream in) throws XmlPullParserException,
            IOException{
        try {
            Log.d(TAG, "parse called");
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            List<Song> songs =  readSongs(parser);
            if(songs != null){
                sharedPreference.saveSongs(songs);
            }
        } finally {
            in.close();
            Log.d(TAG, "input stream closed");
        }
    }

    private List<Song> readSongs(XmlPullParser parser) throws
            XmlPullParserException, IOException{
        Log.d(TAG, "readSongs called");
        String timestamp = readTimestamp(parser);
        Log.d(TAG, "timestamp: " + timestamp);

        //get oldTimestamp to compare and check.
        String oldTimestamp = sharedPreference.getMostRecentTimestamp();
        if (oldTimestamp == null){
            oldTimestamp = mContext.getString(R.string.default_timestamp);
        }

        Log.d(TAG, "Comparing with old timestamp: " + oldTimestamp);
        if (timestamp.equals(oldTimestamp)){
            Log.d(TAG, "Matches old timestamp. Stop parsing");
            return null;
        } else {
            List<Song> songs = new ArrayList<>();
            parser.require(XmlPullParser.START_TAG, ns, "Songs");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                //Starts by looking for the entry tag
                if (name.equals("Song")) {
                    songs.add(readSong(parser));
                } else {
                    skip(parser);
                }
            }
            Log.d(TAG, "Finished parsing achievements");
            //update timestamp
            sharedPreference.saveMostRecentTimestamp(timestamp);
            return songs;
        }
    }

    // Reads a single song from the XML
    private Song readSong(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Song");
        String title = null; String number = null; String link = null; String artist = null;
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            switch (name) {
                case "Number":
                    number = readNumber(parser);
                    break;
                case "Artist":
                    artist = readArtist(parser);
                    break;
                case "Title":
                    title = readTitle(parser);
                    break;
                case "Link":
                    link = readLink(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Song(number, artist, title, link);
    }

    private String readNumber(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Number");
        String number = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Number");
        return number;
    }

    private String readArtist(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Artist");
        String artist = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Artist");
        return artist;
    }

    private String readTitle(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Title");
        return title;
    }

    private String readLink(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "Link");
        String link = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "Link");
        return link;
    }


    private String readText(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT){
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG){
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0){
            switch (parser.next()){
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    private String readTimestamp(XmlPullParser parser) throws IOException,
        XmlPullParserException{

        String timestamp = "";
        parser.require(XmlPullParser.START_TAG, ns, "Songs");
        String tag = parser.getName();

        if (tag.equals("Songs")){
            timestamp = parser.getAttributeValue(null, "timestamp");

        }

        return timestamp;

    }

}
