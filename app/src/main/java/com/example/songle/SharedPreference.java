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

/**
 * Created by Paul Moore on 25-Oct-17.
 *
 * Defines methods to save, add, remove and get Songs from SharedPreferences
 */

public class SharedPreference {
    private Context context;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private static final String TAG = "SharedPreference";
    private static final String PREFS_NAME = "SONGLE_APP";
    private static final String SONGS = "Songs";
    //lyrics numbered 0-4 instead of 1-5 as that made indexing easier which was often necessary
    private static final String LYRICS_0 = "Lyrics0";
    private static final String LYRICS_1 = "Lyrics1";
    private static final String LYRICS_2 = "Lyrics2";
    private static final String LYRICS_3 = "Lyrics3";
    private static final String LYRICS_4 = "Lyrics4";
    private static final String LYRIC_STATUS = "LyricStatus";
    private static final String LYRIC_SIZE = "LyricSize";
    private static final String NEXT_LYRIC_LOCATION = "NextLyricLocation";
    private static final String SONG_SIZES = "SongSizes";
    private static final String MAP_1 = "Map1";
    private static final String MAP_2 = "Map2";
    private static final String MAP_3 = "Map3";
    private static final String MAP_4 = "Map4";
    private static final String MAP_5 = "Map5";
    private static final String MAP_1_NUM = "Map1Num";
    private static final String MAP_2_NUM = "Map2Num";
    private static final String MAP_3_NUM = "Map3Num";
    private static final String MAP_4_NUM = "Map4Num";
    private static final String MAP_5_NUM = "Map5Num";
    private static final String TIMESTAMP = "Timestamp";
    private static final String CURRENT_SONG = "CurrentSong";
    private static final String CURRENT_SONG_NUMBER = "CurrentSongNumber";
    private static final String CURRENT_DIFFICULTY_LEVEL = "CurrentDifficultyLevel";

    public SharedPreference(Context context){
        super();
        this.context = context;
        this.settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.editor = settings.edit();
    }

    //used so that a listener can effectively be registerd and unregistered without needing
    //a separate sharedPreferences object.
    public void registerOnSharedPreferenceChangedListener(SharedPreferences.OnSharedPreferenceChangeListener listener){
        settings.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangedListener(SharedPreferences.OnSharedPreferenceChangeListener listener){
        settings.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void saveSongs(List<Song> newSongs){
        Log.d(TAG, "saveSongs called");
        // this part sets the new song list so that any old ones remain in place
        // important so that progress like completing a song is not overwritten when a new list is downloaded
        // IMPORTANT: Assumes song numbers don't change!
        ArrayList<Song> oldSongs = getAllSongs();
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
     * @param song
     * @param status - must be "I" (incomplete) or "C" complete
     */
    public void saveSongStatus(Song song, String status){
        Log.d(TAG, "saveSongStatus called");
        ArrayList<Song> songs = getAllSongs();

        if (songs == null){
            Log.e(TAG, "No songs found when trying to update status");
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

    public ArrayList<Song> getAllSongs(){
        Log.d(TAG, "getAllSongs called");
        List<Song> songs = new ArrayList<Song>();
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

    public ArrayList<Song> getOldSongs(){
        Log.d(TAG, "getOldSongs called");
        List<Song> songs;

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

    public void saveMostRecentTimestamp(String timestamp){
        editor.putString(TIMESTAMP, timestamp);
        editor.apply();

    }

    public String getMostRecentTimestamp(){
        String timestamp;
        if (settings.contains(TIMESTAMP)){
            timestamp = settings.getString(TIMESTAMP, null);
            return timestamp;
        } else {
            return null;
        }
    }

    //following are methods for saving and getting the current song number
    //needed to check if maps or lyrics need to be redownloaded.
    public void saveCurrentSongNumber(String songNum){
        editor.putString(CURRENT_SONG_NUMBER, songNum);
        editor.apply();
    }

    public String getCurrentSongNumber(){
        String songNum = null;
        if (settings.contains(CURRENT_SONG_NUMBER)){
            songNum = settings.getString(CURRENT_SONG_NUMBER, null);
        }
        return songNum;
    }

    //following are methods for saving and getting the current difficulty level

    public void saveCurrentDifficultyLevel(String diffLevel){
        //check diffLevel is one of the appropriate ones for difficulty level
        if (diffLevel.equals(context.getString(R.string.difficulty_insane))
                || diffLevel.equals(context.getString(R.string.difficulty_hard))
                || diffLevel.equals(context.getString(R.string.difficulty_moderate))
                || diffLevel.equals(context.getString(R.string.difficulty_easy))
                || diffLevel.equals(context.getString(R.string.difficulty_very_easy))){
            editor.putString(CURRENT_DIFFICULTY_LEVEL, diffLevel);
            editor.apply();
        } else {
            Log.e(TAG, "Unexpected difficulty level not saved");
        }
    }

    public String getCurrentDifficultyLevel(){
        String diffLevel = null;
        if (settings.contains(CURRENT_DIFFICULTY_LEVEL)){
            diffLevel = settings.getString(CURRENT_DIFFICULTY_LEVEL, null);

        }
        return diffLevel;
    }

    public void saveCurrentSong(Song song){
        Log.d(TAG, "saveCurrentSong called");
        Gson gson = new Gson();
        String jsonSong = gson.toJson(song);

        editor.putString(CURRENT_SONG, jsonSong);
        editor.apply();
    }

    public Song getCurrentSong(){
        Log.d(TAG, "getCurrentSong called");
        Song song = null;

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
     * @param placemarks
     * @param mapNumber - the map number that should be saved to
     * @param songNumber - the song number of this map
     */
    public void saveMap(List<Placemark> placemarks, String mapNumber, String songNumber){

        Gson gson = new Gson();
        String jsonPlacemarks = gson.toJson(placemarks);

        if (mapNumber.equals("1")){
            editor.putString(MAP_1, jsonPlacemarks);
            editor.putString(MAP_1_NUM, songNumber);
        } else if (mapNumber.equals("2")) {
            editor.putString(MAP_2, jsonPlacemarks);
            editor.putString(MAP_2_NUM, songNumber);
        } else if (mapNumber.equals("3")) {
            editor.putString(MAP_3, jsonPlacemarks);
            editor.putString(MAP_3_NUM, songNumber);
        } else if (mapNumber.equals("4")) {
            editor.putString(MAP_4, jsonPlacemarks);
            editor.putString(MAP_4_NUM, songNumber);
        } else if (mapNumber.equals("5")) {
            editor.putString(MAP_5, jsonPlacemarks);
            editor.putString(MAP_5_NUM, songNumber);
        } else {
            Log.e(TAG, "Unexpected number received in maps");
            return;
        }
        editor.apply();

    }

    public ArrayList<Placemark> getMap(String num){
        List<Placemark> placemarks;
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

    /**
     * Returns true if the song number is already in the lyrics, false otherwise
     * @param songNumber
     * @return
     */
    public boolean checkLyricSongNumberStatus(String songNumber){
        Gson gson = new Gson();
        // if there is no lyric status object already stored, make one
        if (!settings.contains(LYRIC_STATUS)){
            ArrayList<Integer> statuses = new ArrayList<Integer>();
            for (int i = 0; i < 5; i++){
                statuses.add(0);
            }
            String jsonStatuses = gson.toJson(statuses);
            editor.putString(LYRIC_STATUS, jsonStatuses);
            //false since the statuses must have been empty
            return false;
        }
        // there is a lyric status object already
        else {
            ArrayList<Integer> currentStatuses = getLyricStatuses();
            int target = Integer.parseInt(songNumber);
            //check if the song number is already in the statuses
            if (currentStatuses.contains(target)) return true;
            else return false;
        }
    }

    /**
     * Check if a lyrics file will be overwritten when saved
     * @return true if the lyrics are full, false if there's space
     */
    public boolean willOverwrite(){
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        //at least one status is zero, so that will be picked.
        if (currentStatuses.contains(0)) return false;
        else return true;
    }

    /**
     * Returns the song number that will be overwritten if the lyrics are saved
     * @return String of next songNumber to be overwritten
     */
    public String nextSongOverwritten(){
        int nextLocation = getNextLyricLocation();
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        int songNum = currentStatuses.get(nextLocation);
        //if number is e.g. 1, return "01"
        if (songNum < 10){
            return "0" + String.valueOf(songNum);
        } else {
            return String.valueOf(songNum);
        }
    }

    private ArrayList<Integer> getLyricStatuses(){
        Gson gson = new Gson();
        String jsonCurrentStatus = settings.getString(LYRIC_STATUS, null);
        Type type = new TypeToken<ArrayList<Integer>>(){}.getType();
        return gson.fromJson(jsonCurrentStatus, type);
    }

    private void saveLyricStatuses(ArrayList<Integer> statuses){
        Gson gson = new Gson();
        String jsonStatuses = gson.toJson(statuses);
        editor.putString(LYRIC_STATUS, jsonStatuses);
        editor.apply();

    }


    public int getNextLyricLocation(){
        //if there was no previously stored location, return 1 (and save this)
        if (!settings.contains(NEXT_LYRIC_LOCATION)) {
            editor.putInt(NEXT_LYRIC_LOCATION, 1);
            editor.apply();
            return 1;
        }
        else {
            int prevLocation = settings.getInt(NEXT_LYRIC_LOCATION, 1);
            ArrayList<Integer> currentStatuses = getLyricStatuses();
            // Check if there are any empty spaces in the lyrics marked by 0 in the status
            // Add 1 since we start with 1.
            int possibleEmptyLocation = currentStatuses.indexOf(0) + 1;
            //no empty spots, return the default next location
            if (possibleEmptyLocation == 0) return prevLocation;
            //empty spot at index + 1, return this
            else return possibleEmptyLocation;

        }
    }

    public void incrementNextLyricLocation(){
        int prevLocation = settings.getInt(NEXT_LYRIC_LOCATION, 1);
        if (prevLocation < 5){
            int newLocation = prevLocation+1;
            editor.putInt(NEXT_LYRIC_LOCATION, newLocation);
            editor.apply();
        } else if (prevLocation == 5){
            //reset to 1
            editor.putInt(NEXT_LYRIC_LOCATION, 1);
            editor.apply();
        } else {
            Log.e(TAG, "Unexpected Location found: " + prevLocation);
        }

    }

    /**
     * Stores the lyrics file. If there is already a lyrics file, overrides it.
     * @param lyrics
     */
    public void saveLyrics(HashMap<String, ArrayList<String>> lyrics, String songNumber){
        Gson gson = new Gson();
        String jsonLyrics = gson.toJson(lyrics);

        int nextLyricLocation = getNextLyricLocation();
        //save in the next location indicated
        switch(nextLyricLocation) {
            case 1:
                editor.putString(LYRICS_0, jsonLyrics);
            case 2:
                editor.putString(LYRICS_1, jsonLyrics);
            case 3:
                editor.putString(LYRICS_2, jsonLyrics);
            case 4:
                editor.putString(LYRICS_3, jsonLyrics);
            case 5:
                editor.putString(LYRICS_4, jsonLyrics);
        }
        editor.apply();

        //update the statuses with the song number to indicate that the song is in there
        String jsonCurrentStatus = settings.getString(LYRIC_STATUS, null);
        if (jsonCurrentStatus != null) {
            Type type = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> currentStatuses = gson.fromJson(jsonCurrentStatus, type);
            //subtract 1 since the next location starts at 1, not 0
            currentStatuses.add(nextLyricLocation - 1, Integer.parseInt(songNumber));
            //add one to the next location
            incrementNextLyricLocation();
        } else {

        }

    }


    public HashMap<String, ArrayList<String>> getLyrics(String songNumber){
        HashMap<String, ArrayList<String>> lyrics;
        ArrayList<Integer> currentLyricStatuses = getLyricStatuses();
        int songNum = Integer.parseInt(songNumber);
        int lyricLocation = currentLyricStatuses.indexOf(songNum) + 1;
        //return null if the song is not there.
        if (lyricLocation == 0) {
            Log.e(TAG, "Song lyrics not found for song #" + songNumber);
            return null;
        }
        Gson gson = new Gson();
        String jsonLyrics = null;
        Type type = new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType();
        switch(lyricLocation){
            case 1:
                jsonLyrics = settings.getString(LYRICS_0, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 2:
                jsonLyrics = settings.getString(LYRICS_1, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 3:
                jsonLyrics = settings.getString(LYRICS_2, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 4:
                jsonLyrics = settings.getString(LYRICS_3, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 5:
                jsonLyrics = settings.getString(LYRICS_4, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            default:
                Log.e(TAG, "Unexpected lyric location: " + lyricLocation);
                return null;
        }




    }

    public void eraseLyrics(String songNumber){
        ArrayList<Integer> currentLyricStatuses = getLyricStatuses();
        int songNum = Integer.parseInt(songNumber);
        int lyricLocation = currentLyricStatuses.indexOf(songNum) + 1;
        //return if the song is already not there.
        if (lyricLocation == 0) return;

        //song was in statuses, update to 0.
        currentLyricStatuses.set(lyricLocation - 1, 0);
        saveLyricStatuses(currentLyricStatuses);

        switch(lyricLocation){
            case 1:
                editor.putString(LYRICS_0, null);
            case 2:
                editor.putString(LYRICS_1, null);
            case 3:
                editor.putString(LYRICS_2, null);
            case 4:
                editor.putString(LYRICS_3, null);
            case 5:
                editor.putString(LYRICS_4, null);
            default:
                Log.e(TAG, "Unexpected location erased: " + lyricLocation);

        }



    }

    public void saveLyricDimensions(ArrayList<Integer> sizes){
        Gson gson = new Gson();
        String jsonLyrics = gson.toJson(sizes);

        editor.putString(SONG_SIZES, jsonLyrics);
        editor.apply();
    }

    public ArrayList<Integer> getLyricDimensions(){
        ArrayList<Integer> sizes;

        if (settings.contains(SONG_SIZES)){
            String jsonSizes = settings.getString(SONG_SIZES, null);
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Integer>>(){}.getType();
            sizes = gson.fromJson(jsonSizes, type);
            return sizes;
        } else {
            Log.e(TAG, "Song sizes not found");
            return null;
        }
    }


    public String getMapsSongNumber(String mapNumber){
        String songNumber = "";
        String targetMapNumber = null;
        if (mapNumber.equals("1")){
            targetMapNumber = MAP_1_NUM;
        } else if (mapNumber.equals("2")){
            targetMapNumber = MAP_2_NUM;
        } else if (mapNumber.equals("3")){
            targetMapNumber = MAP_3_NUM;
        } else if (mapNumber.equals("4")){
            targetMapNumber = MAP_4_NUM;
        } else if (mapNumber.equals("5")){
            targetMapNumber = MAP_5_NUM;
        } else {
            Log.e(TAG, "Unexpected number requested: " + mapNumber);
            return null;
        }
        if (settings.contains(targetMapNumber)){
            songNumber = settings.getString(targetMapNumber, "");
        }
        return songNumber;
    }
}
