package com.example.songle;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Paul Moore on 25-Oct-17.
 *
 * Defines methods to save, add, remove and get Songs from SharedPreferences
 */

public class SharedPreference {

    private static final String TAG = "SharedPreference";
    private static final String PREFS_NAME = "SONGLE_APP";
    private static final String SONGS = "Songs";
    private static final String MAP_1 = "Map1";
    private static final String MAP_2 = "Map2";
    private static final String MAP_3 = "Map3";
    private static final String MAP_4 = "Map4";
    private static final String MAP_5 = "Map5";
    private static final String TIMESTAMP = "Timestamp";
    private static final String CURRENT_SONG_NUMBER = "Current_song_number";
    private static final String CURRENT_DIFFICULTY_LEVEL = "Current_difficulty_level";

    public SharedPreference(){
        super();
    }

    public void saveSongs(Context context, List<Song> newSongs){
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
                newSongs.set(i, oldSongs.get(i));
            }

        }

        Gson gson = new Gson();
        String jsonSongs = gson.toJson(newSongs);

        editor.putString(SONGS, jsonSongs);
        editor.apply();

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

    /**
     * Update song status accordingly (will be "N" automatically so don't input that)
     * @param context
     * @param song
     * @param status - must be "I" (incomplete) or "C" complete
     */
    public void saveSongStatus(Context context, Song song, String status){
        ArrayList<Song> songs = getSongs(context);
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
        saveSongs(context, songs);

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

    /**
     * Saves a list of placemarks for a map to the SharedPreferences
     * @param context
     * @param placemarks
     * @param num - the map number that should be saved to
     */
    public void saveMap(Context context, List<Placemark> placemarks, int num){
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonSongs = gson.toJson(placemarks);

        if (num == 1){
            editor.putString(MAP_1, jsonSongs);
        } else if (num == 2) {
            editor.putString(MAP_2, jsonSongs);
        } else if (num == 3) {
            editor.putString(MAP_3, jsonSongs);
        } else if (num == 4) {
            editor.putString(MAP_4, jsonSongs);
        } else if (num == 5) {
            editor.putString(MAP_5, jsonSongs);
        } else {
            Log.e(TAG, "Unexpected number received in maps");
        }
        editor.apply();

    }

    public ArrayList<Placemark> getMap(Context context, int num){
        SharedPreferences settings;
        List<Placemark> placemarks;

        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String targetMap;
        if (num == 1){
            targetMap = MAP_1;
        } else if (num == 2){
            targetMap = MAP_2;
        } else if (num == 3){
            targetMap = MAP_3;
        } else if (num == 4){
            targetMap = MAP_4;
        } else if (num == 5){
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
}
