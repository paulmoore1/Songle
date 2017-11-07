package com.example.songle;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Paul Moore on 25-Oct-17.
 *
 * Defines methods to save, add, remove and get Songs from SharedPreferences
 */

public class SharedPreference {

    private static final String TAG = "SharedPreference";
    private static final String PREFS_NAME = "SONGLE_APP";
    private static final String SONGS = "Songs";
    private static final String LYRICS = "Lyrics";
    private static final String SONG_SIZES = "SongSizes";
    private static final String MAP_1 = "Map1";
    private static final String MAP_2 = "Map2";
    private static final String MAP_3 = "Map3";
    private static final String MAP_4 = "Map4";
    private static final String MAP_5 = "Map5";
    private static final String TIMESTAMP = "Timestamp";
    private static final String CURRENT_SONG = "CurrentSong";
    private static final String CURRENT_SONG_NUMBER = "CurrentSongNumber";
    private static final String CURRENT_DIFFICULTY_LEVEL = "CurrentDifficultyLevel";

    public SharedPreference(){
        super();
    }

    //used so that a listener can effectively be registerd and unregistered without needing
    //a separate sharedPreferences object.
    public void registerOnSharedPreferenceChangedListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener){
        SharedPreferences settings;
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangedListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener){
        SharedPreferences settings;
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settings.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void saveSongs(Context context, List<Song> newSongs){
        Log.d(TAG, "saveSongs called");
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        // this part sets the new song list so that any old ones remain in place
        // important so that progress like completing a song is not overwritten when a new list is downloaded
        // IMPORTANT: Assumes song numbers don't change!
        ArrayList<Song> oldSongs = getAllSongs(context);
        if (oldSongs != null){
            int oldLength = oldSongs.size();
            for (int i = 0; i < oldLength; i++){
                newSongs.set(i, oldSongs.get(i));
            }

        }

        Gson gson = new Gson();
        String jsonSongs = gson.toJson(newSongs);

        editor.putString(SONGS, jsonSongs);
        editor.apply();

    }


    /**
     * Update song status accordingly (will be "N" automatically so don't input that)
     * @param context
     * @param song
     * @param status - must be "I" (incomplete) or "C" complete
     */
    public void saveSongStatus(Context context, Song song, String status){
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();
        Log.d(TAG, "saveSongStatus called");
        ArrayList<Song> songs = getAllSongs(context);

        if (songs == null){
            Log.e(TAG, "No songs found when trying to update status");
            return;
        }
        //check the status is not "N"
        if (status.equals("N")){
            Log.e(TAG, "Tried to set song to not started");
            return;
        }

        //update the song's status
        song.setStatus(status);
        //find where in the array list the song belongs. Subtract 1 since the first song is 01
        int targetNum = Integer.parseInt(song.getNumber()) - 1;
        //update the songs list
        songs.set(targetNum, song);

        //save the updated songs
        Gson gson = new Gson();
        String jsonSongs = gson.toJson(songs);

        editor.putString(SONGS, jsonSongs);
        editor.apply();

        Log.d(TAG, "saveSongStatus finished");

    }

    public ArrayList<Song> getAllSongs(Context context){
        Log.d(TAG, "getAllSongs called");
        SharedPreferences settings;
        List<Song> songs = new ArrayList<Song>();

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(SONGS)){
            String jsonSongs = settings.getString(SONGS, null);
            Gson gson = new Gson();
            Type type = new TypeToken<List<Song>>(){}.getType();
            songs = gson.fromJson(jsonSongs, type);
            Log.d(TAG, "getAllSongs returned songs");
            return (ArrayList<Song>) songs;

        } else {
            Log.d(TAG, "getAllSongs returned null");
            return  null;
        }
    }

    public ArrayList<Song> getOldSongs(Context context){
        Log.d(TAG, "getOldSongs called");
        SharedPreferences settings;
        List<Song> songs;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(SONGS)){
            String jsonSongs = settings.getString(SONGS, null);
            Gson gson = new Gson();
            Type type = new TypeToken<List<Song>>(){}.getType();
            songs = gson.fromJson(jsonSongs, type);
            Log.d(TAG, "Full list of songs: " + songs.toString());

            //remove all songs which have not been started, so only complete or incomplete ones are returned.
            Iterator<Song> iter = songs.iterator();
            while (iter.hasNext()){
                Song song = iter.next();
                if (song.isSongNotStarted()) iter.remove();
            }

            Log.d(TAG, "New list of songs: " + songs.toString());
            Log.d(TAG, "getOldSongs returned songs");
            return (ArrayList<Song>) songs;

        } else {
            Log.d(TAG, "getOldSongs return null");
            return  null;
        }
    }

    public void saveMostRecentTimestamp(Context context, String timestamp){
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        editor.putString(TIMESTAMP, timestamp);
        editor.apply();

    }

    public String getMostRecentTimestamp(Context context){
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

    //following are methods for saving and getting the current song number
    //needed to check if maps or lyrics need to be redownloaded.
    public void saveCurrentSongNumber(Context context, String songNum){

        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        editor.putString(CURRENT_SONG_NUMBER, songNum);
        editor.apply();

    }

    public String getCurrentSongNumber(Context context){
        SharedPreferences settings;
        String songNum = null;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(CURRENT_SONG_NUMBER)){
            songNum = settings.getString(CURRENT_SONG_NUMBER, null);
        }
        return songNum;

    }

    //following are methods for saving and getting the current difficulty level

    public void saveCurrentDifficultyLevel(Context context, String diffLevel){
        //check diffLevel is one of the appropriate ones for difficulty level
        if (diffLevel.equals(context.getString(R.string.difficulty_insane))
                || diffLevel.equals(context.getString(R.string.difficulty_hard))
                || diffLevel.equals(context.getString(R.string.difficulty_moderate))
                || diffLevel.equals(context.getString(R.string.difficulty_easy))
                || diffLevel.equals(context.getString(R.string.difficulty_very_easy))){
            SharedPreferences settings;
            SharedPreferences.Editor editor;

            settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            editor = settings.edit();

            editor.putString(CURRENT_DIFFICULTY_LEVEL, diffLevel);
            editor.apply();
        } else {
            Log.e(TAG, "Unexpected difficulty level not saved");
        }


    }

    public String getCurrentDifficultyLevel(Context context){
        SharedPreferences settings;
        String diffLevel = null;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(CURRENT_DIFFICULTY_LEVEL)){
            diffLevel = settings.getString(CURRENT_DIFFICULTY_LEVEL, null);

        }
        return diffLevel;

    }

    public void saveCurrentSong(Context context, Song song){
        Log.d(TAG, "saveCurrentSong called");
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonSong = gson.toJson(song);

        editor.putString(CURRENT_SONG, jsonSong);
        editor.apply();
    }

    public Song getCurrentSong(Context context){
        Log.d(TAG, "getCurrentSong called");
        SharedPreferences settings;
        Song song = null;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.contains(CURRENT_SONG)){
            String jsonSongs = settings.getString(CURRENT_SONG, null);
            Gson gson = new Gson();
            song = gson.fromJson(jsonSongs, Song.class);
            return song;

        } else {
            return  null;
        }

    }

    /**
     * Saves a list of placemarks for a map to the SharedPreferences
     * @param context
     * @param placemarks
     * @param num - the map number that should be saved to
     */
    public void saveMap(Context context, List<Placemark> placemarks, String num){
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonPlacemarks = gson.toJson(placemarks);

        if (num.equals("1")){
            editor.putString(MAP_1, jsonPlacemarks);
        } else if (num.equals("2")) {
            editor.putString(MAP_2, jsonPlacemarks);
        } else if (num.equals("3")) {
            editor.putString(MAP_3, jsonPlacemarks);
        } else if (num.equals("4")) {
            editor.putString(MAP_4, jsonPlacemarks);
        } else if (num.equals("5")) {
            editor.putString(MAP_5, jsonPlacemarks);
        } else {
            Log.e(TAG, "Unexpected number received in maps");
            return;
        }
        editor.apply();

    }

    public ArrayList<Placemark> getMap(Context context, String num){
        SharedPreferences settings;
        List<Placemark> placemarks;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String targetMap;
        if (num.equals("1")){
            targetMap = MAP_1;
        } else if (num.equals("2")){
            targetMap = MAP_2;
        } else if (num.equals("3")){
            targetMap = MAP_3;
        } else if (num.equals("4")){
            targetMap = MAP_4;
        } else if (num.equals("5")){
            targetMap = MAP_5;
        } else {
            Log.e(TAG, "Unexpected number requested");
            return null;
        }

        if (settings.contains(targetMap)){
            String jsonPlacemarks = settings.getString(targetMap, null);
            Gson gson = new Gson();
            Placemark[] placemarksArray = gson.fromJson(jsonPlacemarks, Placemark[].class);
            placemarks = Arrays.asList(placemarksArray);
            placemarks = new ArrayList<Placemark>(placemarks);
            return (ArrayList<Placemark>) placemarks;

        } else {
            Log.e(TAG, "Map not found");
            return  null;
        }
    }

    public void saveLyrics(Context context, HashMap<String, ArrayList<String>> lyrics){
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        Gson gson = new Gson();
        String jsonLyrics = gson.toJson(lyrics);

        editor.putString(LYRICS, jsonLyrics);
        editor.apply();
    }

    public HashMap<String, ArrayList<String>> getLyrics(Context context){
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        HashMap<String, ArrayList<String>> lyrics;

        if(settings.contains(LYRICS)){
            String jsonLyrics = settings.getString(LYRICS, null);
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType();
            lyrics = gson.fromJson(jsonLyrics, type);
            return lyrics;

        } else {
            Log.e(TAG, "Lyrics not found");
            return null;
        }


    }

    public void saveSongDimensions(Context context, int[][] sizes){
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        Gson gson = new Gson();
        String jsonLyrics = gson.toJson(sizes);

        editor.putString(SONG_SIZES, jsonLyrics);
        editor.apply();
    }
}
