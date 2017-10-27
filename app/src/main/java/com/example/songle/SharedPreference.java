package com.example.songle;

import android.content.Context;
import android.content.SharedPreferences;


import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Paul Moore on 25-Oct-17.
 *
 * Defines methods to save, add, remove and get Songs from SharedPreferences
 */

public class SharedPreference {


    public static final String PREFS_NAME = "SONGLE_APP";
    public static final String SONGS = "Songs";
    public static final String TIMESTAMP = "Timestamp";

    public SharedPreference(){
        super();
    }

    public void saveSongs(Context context, List<Song> songs){
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        // this part sets the new song list so that any old ones remain in place
        // important so that progress like completing a song is not overwritten when a new list is downloaded
        // IMPORTANT: Assumes song numbers don't change!
        ArrayList<Song> oldSongs = getSongs(context);
        if (oldSongs != null){
            int oldLength = oldSongs.size();
            for (int i = 0; i < oldLength; i++){
                songs.set(i, oldSongs.get(i));
            }

        }

        Gson gson = new Gson();
        String jsonSongs = gson.toJson(songs);

        editor.putString(SONGS, jsonSongs);
        editor.commit();

    }

    public void addSong(Context context, Song song){
        ArrayList<Song> songs = getSongs(context);
        if (songs == null){
            songs = new ArrayList<Song>();
        }
        songs.add(song);
        saveSongs(context, songs);
    }

    public void removeSong(Context context, Song song){
        ArrayList<Song> songs = getSongs(context);
        if (songs != null){
            songs.remove(song);
            saveSongs(context, songs);
        }
    }

    public ArrayList<Song> getSongs(Context context){
        SharedPreferences settings;
        List<Song> songs;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(SONGS)){
            String jsonSongs = settings.getString(SONGS, null);
            Gson gson = new Gson();
            Song[] songsArray = gson.fromJson(jsonSongs, Song[].class);
            songs = Arrays.asList(songsArray);
            songs = new ArrayList<Song>(songs);
            return (ArrayList<Song>) songs;

        } else {
            return  null;
        }
    }

    public void saveTimestamp(Context context, String timestamp){
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        editor.putString(TIMESTAMP, timestamp);
        editor.commit();

    }

    public String getTimestamp(Context context){
        SharedPreferences settings;
        String timestamp;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(TIMESTAMP)){
            timestamp = settings.getString(TIMESTAMP, null);
            return timestamp;
        } else {
            return null;
        }
    }
}
