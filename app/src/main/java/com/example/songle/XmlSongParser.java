package com.example.songle;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Moore on 18-Oct-17.
 */

public class XmlSongParser {

    //Don't use namespaces
    private static final String ns = null;

    public List<Song> parse(InputStream in) throws XmlPullParserException,
            IOException{
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readSongs(parser);
        } finally {
            in.close();
        }
    }

    // gets the value of the song which comes last in the songs list
    // won't use here unless we have time - could save on downloading entire XML
    public int lastSongNumber(List<Song> songs){
        Song lastSong = songs.get(songs.size() - 1);
        int lastNum = Integer.parseInt(lastSong.getNumber());
        return lastNum;
    }

    private List<Song> readSongs(XmlPullParser parser) throws
            XmlPullParserException, IOException{
        List<Song> songs = new ArrayList<Song>();
        parser.require(XmlPullParser.START_TAG, ns, "Songs");
        while (parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String name = parser.getName();
            //Starts by looking for the entry tag
            if (name.equals("Song")){
                songs.add(readSong(parser));
            } else {
                skip(parser);
            }
        }
        return songs;
    }

    private Song readSong(XmlPullParser parser) throws
            XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Song");
        String title = null; String number = null; String link = null; String artist = null;
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if(name.equals("Number")){
                number = readNumber(parser);
            } else if (name.equals("Artist")){
                artist = readArtist(parser);
            } else if (name.equals("Title")){
                title = readTitle(parser);
            } else if (name.equals("Link")){
                link = readLink(parser);
            } else {
                skip(parser);
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
        String link = "";
        parser.require(XmlPullParser.START_TAG, ns, "Link");
        String tag = parser.getName();
        String relType = parser.getAttributeValue(null, "href");
        if (tag.equals("Link")){
            if (relType.equals("alternate")){
                link = parser.getAttributeValue(null, "href");
                parser.nextTag();
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link");
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

    public String getXmlTimestamp(InputStream in) throws XmlPullParserException,
    IOException{
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readTimestamp(parser);
        } finally {
            in.close();
        }
    }

    private String readTimestamp(XmlPullParser parser) throws IOException,
        XmlPullParserException{
        String timestamp = "";
        parser.require(XmlPullParser.START_TAG, ns, "Songs");
        String tag = parser.getName();
        if (tag.equals("Songs")){
            timestamp = parser.getAttributeValue("timestamp", null);
        }
        parser.require(XmlPullParser.END_TAG, ns, "Songs");
        return timestamp;

    }

}
