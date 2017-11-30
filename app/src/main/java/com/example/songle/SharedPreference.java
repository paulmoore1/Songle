package com.example.songle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Paul Moore on 25-Oct-17.
 *
 * Defines methods to save, add, remove and get Songs from SharedPreferences
 */

public class SharedPreference {
    private final Context context;
    private final SharedPreferences settings;
    private final SharedPreferences.Editor editor;
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
    private static final String NEXT_LYRIC_LOCATION = "NextLyricLocation";
    private static final String NUM_WORDS_FOUND = "WordsFound";
    private static final String NUM_WORDS_AVAILABLE = "WordsAvailable";
    private static final String ARTIST_REVEALED = "ArtistRevealed";
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
    private static final String INCORRECT_GUESS = "IncorrectGuess";

    @SuppressLint("CommitPrefEdits")
    public SharedPreference(Context context){
        super();
        this.context = context;
        this.settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.editor = settings.edit();
    }

    //used so that a listener can effectively be registerd and unregistered without needing
    //a separate sharedPreferences object.
    public void registerOnSharedPreferenceChangedListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener){
        settings.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangedListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener){
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
     * @param songNumber song number to update
     * @param status - must be "N" (not complete), "I" (incomplete) or "C" complete
     */
    public void saveSongStatus(String songNumber, String status){
        Log.d(TAG, "saveSongStatus called");
        ArrayList<Song> songs = getAllSongs();

        Log.d(TAG, songs.toString());
        //Check there is a list of songs
        if (songs == null){
            Log.e(TAG, "No songs found when trying to update status");
            return;
        }
        Song song = getSong(songNumber, songs);
        //Check the song is actually there
        if (song == null){
            Log.e(TAG, "Song number not found in songs list");
            return;
        }
        //update the current song status in the shared preferences if it's the one being updated.
        Song currentSong = getCurrentSong();
        Log.v(TAG, "currentSong before " + currentSong.toString());
        if (currentSong.getNumber().equals(songNumber)){
            currentSong.setStatus(status);
            Log.v(TAG, "currentSong after " + currentSong.toString());
            saveCurrentSong(currentSong);
        }


        //update the song's status
        song.setStatus(status);
        //find where in the array list the song belongs. Subtract 1 since the first song is 01
        int targetNum = Integer.parseInt(songNumber) - 1;
        //update the songs list
        songs.set(targetNum, song);

        //save the updated songs
        Gson gson = new Gson();
        String jsonSongs = gson.toJson(songs);

        editor.putString(SONGS, jsonSongs);
        editor.apply();

        Log.d(TAG, "saveSongStatus finished");

    }

    private Song getSong(String songNumber, ArrayList<Song> songs){
        int size = songs.size();
        int index = Integer.parseInt(songNumber) - 1;
        if (index < size){
            return songs.get(index);
        } else{
            Log.e(TAG, "Tried to access song #" + songNumber + "in array of size " + size);
            return null;
        }
    }

    public ArrayList<Song> getAllSongs(){
        List<Song> songs;
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
            if (songs == null) {
                Log.e(TAG, "No songs found");
                return null;
            }
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
        Log.d(TAG, "saveCurrentSongNumber called");
        editor.putString(CURRENT_SONG_NUMBER, songNum);
        editor.apply();
    }

    public String getCurrentSongNumber(){
        Log.v(TAG, "getCurrentSongNumber called");
        String songNum = null;
        if (settings.contains(CURRENT_SONG_NUMBER)){
            songNum = settings.getString(CURRENT_SONG_NUMBER, null);
        }
        return songNum;
    }

    //following are methods for saving and getting the current difficulty level

    public void saveCurrentDifficultyLevel(String diffLevel){
        Log.d(TAG, "saveCurrentDifficultyLevelNumber called");
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
        Log.d(TAG, "getCurrentDifficultyLevel called");
        String diffLevel = null;
        if (settings.contains(CURRENT_DIFFICULTY_LEVEL)){
            diffLevel = settings.getString(CURRENT_DIFFICULTY_LEVEL, null);

        }
        return diffLevel;
    }

    public String getCurrentDifficultyLevelNumber(){
        Log.d(TAG, "getCurrentDifficultyLevelNumber called");
        if (settings.contains(CURRENT_DIFFICULTY_LEVEL)){
            String text = settings.getString(CURRENT_DIFFICULTY_LEVEL, null);
            if (text != null){
                switch (text){
                    case "Insane":
                        return "1";
                    case "Hard":
                        return "2";
                    case "Moderate":
                        return "3";
                    case "Easy":
                        return "4";
                    case "Very Easy":
                        return "5";
                    default:
                        return "1";
                }
            } else {
                return "1";
            }

        } else {
            //return 1 as default
            return "1";
        }
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
        Song song;

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
     * @param placemarks the placemarks to save
     * @param mapNumber - the map number that should be saved to
     * @param songNumber - the song number of this map
     */
    public void saveMap(List<Placemark> placemarks, String mapNumber, String songNumber){
        Log.v(TAG, "saveMap called");

        Gson gson = new Gson();
        String jsonPlacemarks = gson.toJson(placemarks);

        switch (mapNumber) {
            case "1":
                editor.putString(MAP_1, jsonPlacemarks);
                editor.putString(MAP_1_NUM, songNumber);
                break;
            case "2":
                editor.putString(MAP_2, jsonPlacemarks);
                editor.putString(MAP_2_NUM, songNumber);
                break;
            case "3":
                editor.putString(MAP_3, jsonPlacemarks);
                editor.putString(MAP_3_NUM, songNumber);
                break;
            case "4":
                editor.putString(MAP_4, jsonPlacemarks);
                editor.putString(MAP_4_NUM, songNumber);
                break;
            case "5":
                editor.putString(MAP_5, jsonPlacemarks);
                editor.putString(MAP_5_NUM, songNumber);
                break;
            default:
                Log.e(TAG, "Unexpected number received in maps");
                return;
        }
        editor.apply();

    }

    public ArrayList<Placemark> getMap(String mapNum){
        Log.v(TAG, "getMap called for map #" + mapNum);
        ArrayList<Placemark> placemarks;
        String targetMap;
        switch (mapNum) {
            case "1":
                targetMap = MAP_1;
                break;
            case "2":
                targetMap = MAP_2;
                break;
            case "3":
                targetMap = MAP_3;
                break;
            case "4":
                targetMap = MAP_4;
                break;
            case "5":
                targetMap = MAP_5;
                break;
            default:
                Log.e(TAG, "Unexpected number requested");
                return null;
        }
        if (settings.contains(targetMap)){
            String jsonPlacemarks = settings.getString(targetMap, null);
            Log.e(TAG, "map song number: " + getMapsSongNumber(Integer.parseInt(mapNum)));
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Placemark>>(){}.getType();
            placemarks = gson.fromJson(jsonPlacemarks, type);
            return placemarks;

        } else {
            Log.e(TAG, "Map not found");
            return  null;
        }
    }

    /**
     * Get the song number stored in the map at a certain number from 1-5
     * @param mapNumber takes values 1-5
     * @return song number stored in that map as an integer.
     */
    private int getMapsSongNumber(int mapNumber){
        String songNumber = "";
        String targetMapNumber;
        //TODO check if a switch works too - says variable not initialised
        /*
        switch(mapNumber){
            case 1:
                targetMapNumber = MAP_1_NUM;
            case 2:
                targetMapNumber = MAP_2_NUM;
            case 3:
                targetMapNumber = MAP_3_NUM;
            case 4:
                targetMapNumber = MAP_4_NUM;
            case 5:
                targetMapNumber = MAP_5_NUM;
        }*/
        if (mapNumber == 1){
            targetMapNumber = MAP_1_NUM;
        } else if (mapNumber == 2){
            targetMapNumber = MAP_2_NUM;
        } else if (mapNumber == 3){
            targetMapNumber = MAP_3_NUM;
        } else if (mapNumber == 4){
            targetMapNumber = MAP_4_NUM;
        } else if (mapNumber == 5){
            targetMapNumber = MAP_5_NUM;
        } else {
            Log.e(TAG, "Unexpected number requested: " + mapNumber);
            return 0;
        }
        if (settings.contains(targetMapNumber)){
            //return 0 as default to indicate no song is there
            songNumber = settings.getString(targetMapNumber, "0");
            return Integer.parseInt(songNumber);
        } else {
            return 0;
        }

    }

    /**
     * Checks that all the maps for a song are correct
     * @param songNumber - target song number
     * @return true if all the maps for that song are stored; false otherwise
     */
    public boolean checkMaps(String songNumber){
        Log.v(TAG, "checkMaps called");
        //convert to integer for easy comparison
        int songNum = Integer.parseInt(songNumber);
        boolean result = true;
        for (int i = 1; i < 6; i++){
            //result will only remain true if all the maps had the song number
            result = result && (getMapsSongNumber(i) == songNum);
        }
        return result;
    }

    /**
     * Check if the lyrics for a certain song are stored.
     * @param songNumber - number of song being checked
     * @return true if the song is stored in the lyrics, false otherwise
     */
    public boolean checkLyricsStored(String songNumber){
        Log.v(TAG, "checkLyricsStored called");
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        int target = Integer.parseInt(songNumber);
        //check if the song number is already in the statuses
        return currentStatuses.contains(target);
    }

    /**
     * Find which lyrics file a song number is stored in.
     * @param songNumber - number of song being looked for
     * @return - index where that song is stored.
     */
    public int getSongLocation(String songNumber){
        Log.v(TAG, "getSongLocation called");
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        Log.v(TAG, "current Statuses: " + currentStatuses);
        int target = Integer.parseInt(songNumber);
        return currentStatuses.indexOf(target);
    }

    /**
     * Check if a lyrics file will be overwritten when saved
     * @return true if the lyrics are full, false if there's space
     */
    public boolean willOverwrite(){
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        //true if there are no zeros in the statuses
        return !currentStatuses.contains(0);
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
        // if there is no lyric status object already stored, make one
        if (!settings.contains(LYRIC_STATUS)){
            ArrayList<Integer> statuses = new ArrayList<>();
            for (int i = 0; i < 5; i++){
                statuses.add(0);
            }
            Gson gson = new Gson();
            String jsonStatuses = gson.toJson(statuses);
            editor.putString(LYRIC_STATUS, jsonStatuses);
            //return empty list since the statuses must have been empty
            return statuses;
        }
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

    private int getNextLyricLocation(){
        //if there was no previously stored location, return 0 (and save)
        if (!settings.contains(NEXT_LYRIC_LOCATION)) {
            editor.putInt(NEXT_LYRIC_LOCATION, 0);
            editor.apply();
            return 0;
        }
        else {
            int prevLocation = settings.getInt(NEXT_LYRIC_LOCATION, 0);
            ArrayList<Integer> currentStatuses = getLyricStatuses();
            Log.d(TAG, "Song statuses" + currentStatuses.toString());
            // Check if there are any empty spaces in the lyrics (marked by 0)
            int possibleEmptyLocation = currentStatuses.indexOf(0);
            //no empty spots, return the default next location
            if (possibleEmptyLocation == -1) return prevLocation;
            //empty spot, return this
            else return possibleEmptyLocation;
        }
    }

    /**
     * Get the song stored at a certain location
     * @param location takes on values from 0-4 inclusive
     * @return the integer of the song number there.
     */
    private int getSongAtLyricLocation(int location){
        Log.v(TAG, "getSongAtLyricLocation called");
        ArrayList<Integer> statuses = getLyricStatuses();
        return statuses.get(location);
    }

    private void incrementNextLyricLocation(){
        int prevLocation = settings.getInt(NEXT_LYRIC_LOCATION, 0);
        if (prevLocation < 4){
            int newLocation = prevLocation+1;
            editor.putInt(NEXT_LYRIC_LOCATION, newLocation);
            editor.apply();
        } else if (prevLocation == 4){
            //reset to 1
            editor.putInt(NEXT_LYRIC_LOCATION, 0);
            editor.apply();
        } else {
            Log.e(TAG, "Unexpected Location found: " + prevLocation);
        }
    }

    /**
     * Stores the lyrics file.
     * If there is already a lyrics file where it needs to store, overrides it.
     * @param lyrics the lyrics to store
     * @param songNumber the song number for those lyrics
     */
    public void saveNewLyrics(HashMap<String, ArrayList<String>> lyrics, String songNumber){
        Log.d(TAG, "saveNewLyrics called");
        // Check that the lyrics are not already stored.
        if(!checkLyricsStored(songNumber)){
            Gson gson = new Gson();
            String jsonLyrics = gson.toJson(lyrics);
            int nextLyricLocation = getNextLyricLocation();
            Log.v(TAG, "Will store in " + nextLyricLocation);

            int prevSongNumber = getSongAtLyricLocation(nextLyricLocation);
            Log.v(TAG, "old song: " + prevSongNumber);
            //if there was a song already there, erase it
            if(prevSongNumber != 0) {
                eraseLyrics(prevSongNumber);
            }

            //save in the next location indicated
            switch(nextLyricLocation) {
                case 0:
                    editor.putString(LYRICS_0, jsonLyrics);
                case 1:
                    editor.putString(LYRICS_1, jsonLyrics);
                case 2:
                    editor.putString(LYRICS_2, jsonLyrics);
                case 3:
                    editor.putString(LYRICS_3, jsonLyrics);
                case 4:
                    editor.putString(LYRICS_4, jsonLyrics);
            }
            editor.apply();
            //update the statuses to reflect the new song added
            ArrayList<Integer> statuses = getLyricStatuses();
            statuses.set(nextLyricLocation, Integer.parseInt(songNumber));
            saveLyricStatuses(statuses);
            resetNumberWordsFound();
            //move to the next default location
            incrementNextLyricLocation();
        }

    }

    /**
     * Updates an already-stored lyrics file.
     * If there is not a lyrics file for that number, does nothing.
     * @param lyrics the lyrics to store
     * @param songNumber the song number for those lyrics
     */
    public void updateLyrics(HashMap<String, ArrayList<String>> lyrics, String songNumber){
        Log.d(TAG, "updateLyrics called");
        Gson gson = new Gson();
        String jsonLyrics = gson.toJson(lyrics);
        int lyricLocation = getSongLocation(songNumber);
        // If the lyrics are stored there
        if (lyricLocation != -1){
            //save in the next location indicated
            switch(lyricLocation) {
                case 0:
                    editor.putString(LYRICS_0, jsonLyrics);
                case 1:
                    editor.putString(LYRICS_1, jsonLyrics);
                case 2:
                    editor.putString(LYRICS_2, jsonLyrics);
                case 3:
                    editor.putString(LYRICS_3, jsonLyrics);
                case 4:
                    editor.putString(LYRICS_4, jsonLyrics);
            }
            editor.apply();
        }
    }

    /**
     * Return the lyrics for a song number
     * @param songNumber - number of song to get lyrics for
     * @return - the lyrics, null if they aren't stored.
     */
    public HashMap<String, ArrayList<String>> getLyrics(String songNumber){
        Log.d(TAG, "getLyrics called");
        HashMap<String, ArrayList<String>> lyrics;
        int lyricLocation = getSongLocation(songNumber);
        Log.v(TAG, "lyric location is: " + lyricLocation);
        //return null if the song is not there.
        if (lyricLocation == -1) {
            Log.e(TAG, "Song lyrics not found for song #" + songNumber);
            return null;
        }
        Gson gson = new Gson();
        String jsonLyrics;
        Type type = new TypeToken<HashMap<String, ArrayList<String>>>(){}.getType();
        //TODO fix switch
        switch(lyricLocation){
            case 0:
                jsonLyrics = settings.getString(LYRICS_0, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 1:
                jsonLyrics = settings.getString(LYRICS_1, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 2:
                jsonLyrics = settings.getString(LYRICS_2, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 3:
                jsonLyrics = settings.getString(LYRICS_3, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            case 4:
                jsonLyrics = settings.getString(LYRICS_4, null);
                lyrics = gson.fromJson(jsonLyrics, type);
                return lyrics;
            default:
                Log.e(TAG, "Unexpected lyric location: " + lyricLocation);
                return null;
        }
    }

    public void eraseLyrics(int songNumber){
        Log.v(TAG, "eraseLyrics called");
        //indicate the song as not started (it's erased)
        String songNumString = String.valueOf(songNumber);
        if (songNumber < 10){
            //put a 0 in front of the string so it matches the other song numbers below 10
            songNumString = "0" + songNumString;
        }

        saveSongStatus(songNumString, "N");

        resetNumberWordsFound();
        resetNumberWordsAvailable();
        resetIncorrectGuess();
        artistHidden();

        //update statuses
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        int lyricLocation = currentStatuses.indexOf(songNumber);
        //return if the song is already not there.
        if (lyricLocation == -1) return;

        //song was in statuses, update to 0.
        currentStatuses.set(lyricLocation, 0);
        saveLyricStatuses(currentStatuses);

        switch(lyricLocation){
            case 0:
                editor.putString(LYRICS_0, null);
            case 1:
                editor.putString(LYRICS_1, null);
            case 2:
                editor.putString(LYRICS_2, null);
            case 3:
                editor.putString(LYRICS_3, null);
            case 4:
                editor.putString(LYRICS_4, null);
            default:
                Log.e(TAG, "Unexpected location erased: " + lyricLocation);

        }
        editor.apply();
    }

    public void completeSong(String songNumber){
        Log.v(TAG, "completeSong called");
        //indicate the song as completed
        int songNumInteger = Integer.parseInt(songNumber);

        saveSongStatus(songNumber, "C");

        //update statuses
        ArrayList<Integer> currentStatuses = getLyricStatuses();
        int lyricLocation = currentStatuses.indexOf(Integer.parseInt(songNumber));
        //return if the song is already not there.
        if (lyricLocation == -1) {
            Log.e(TAG, "Song not saved in lyrics when it was completed");
            return;
        }
        resetNumberWordsFound();
        resetNumberWordsAvailable();
        resetIncorrectGuess();
        artistHidden();

        //song was in statuses, update to 0.
        currentStatuses.set(lyricLocation, 0);
        saveLyricStatuses(currentStatuses);

        switch(lyricLocation){
            case 0:
                editor.putString(LYRICS_0, null);
            case 1:
                editor.putString(LYRICS_1, null);
            case 2:
                editor.putString(LYRICS_2, null);
            case 3:
                editor.putString(LYRICS_3, null);
            case 4:
                editor.putString(LYRICS_4, null);
            default:
                Log.e(TAG, "Unexpected location completed: " + lyricLocation);

        }
        editor.apply();
    }

    public void resetNumberWordsFound(){
        Log.d(TAG, "resetNumberWordsFound called");
        String songNumber = getCurrentSongNumber();
        ArrayList<Integer> wordsList = getWordsFoundList();
        int loc = getSongLocation(songNumber);
        if (loc != -1){
            wordsList.set(loc, 0);
            saveWordsFoundList(wordsList);
        } else {
            Log.e(TAG, "Song not found in storage");
        }
    }

    public void incrementNumberWordsFound(){
        Log.d(TAG, "incrementNumberWordsFound called");
        String songNumber = getCurrentSongNumber();
        ArrayList<Integer> wordsList = getWordsFoundList();
        int loc = getSongLocation(songNumber);
        if (loc != -1){
            int prev = wordsList.get(loc);
            prev++;
            wordsList.set(loc, prev);
            saveWordsFoundList(wordsList);
            incrementNumAvailableWords(loc);
        } else {
            Log.e(TAG, "Song not found in storage");
        }

    }

    public int getNumberWordsFound(){
        Log.d(TAG, "getNumberWordsFound called");
        String songNumber = getCurrentSongNumber();
        ArrayList<Integer> wordsFoundList = getWordsFoundList();
        int loc = getSongLocation(songNumber);
        if (loc != -1)
        return wordsFoundList.get(loc);
        else {
            Log.e(TAG, "Song not found in storage");
            return 0;
        }
    }

    private ArrayList<Integer> getWordsFoundList(){
        // if there is no words list object stored make one.
        if(!settings.contains(NUM_WORDS_FOUND)){
            ArrayList<Integer> numWordsList = new ArrayList<>();
            for (int i = 0; i < 5; i++){
                numWordsList.add(0);
            }
            Gson gson = new Gson();
            String jsonWordsList = gson.toJson(numWordsList);
            editor.putString(NUM_WORDS_FOUND, jsonWordsList);
            return numWordsList;
        } else {
            // find and return the array
            Gson gson = new Gson();
            String jsonWordsList = settings.getString(NUM_WORDS_FOUND, null);
            Type type = new TypeToken<ArrayList<Integer>>(){}.getType();
            return gson.fromJson(jsonWordsList, type);
        }
    }

    private void saveWordsFoundList(ArrayList<Integer> wordsList){
        Gson gson = new Gson();
        String jsonWordsList = gson.toJson(wordsList);
        editor.putString(NUM_WORDS_FOUND, jsonWordsList);
        editor.apply();
    }

    public void resetNumberWordsAvailable(){
        Log.d(TAG, "resetNumberWordsAvailable called");
        String songNumber = getCurrentSongNumber();
        ArrayList<Integer> wordsList = getWordsAvailableList();
        int loc = getSongLocation(songNumber);
        if (loc != -1){
            wordsList.set(loc, 0);
            saveWordsAvailableList(wordsList);
        } else {
            Log.e(TAG, "Song not found in storage");
        }
    }

    public int getNumAvailableWords(){
        Log.d(TAG, "getNumAvailableWords called");
        String songNumber = getCurrentSongNumber();
        Log.v(TAG, "songNumber is: " + songNumber);
        ArrayList<Integer> wordsAvailableList = getWordsAvailableList();
        int location = getSongLocation(songNumber);
        if (location == -1){
            Log.e(TAG, "Song not found in storage");
            return 0;
        }
        return wordsAvailableList.get(location);
    }


    // Called whenever incrementNumWordsFound is called.
    // Already checked that it's not -1 in numWordsFound method.
    private void incrementNumAvailableWords(int loc){
        Log.d(TAG, "incrementNumAvailableWords called");
        ArrayList<Integer> wordsAvailableList = getWordsAvailableList();
        int prev = wordsAvailableList.get(loc);
        prev++;
        wordsAvailableList.set(loc, prev);
        saveWordsAvailableList(wordsAvailableList);
    }

    /**
     * Removes a number of words from the words available to spend on hints.
     * Used to update the shared preferences with this change.
     * @param numToRemove - number of words spent on hint
     */
    public void removeNumAvailableWords(int numToRemove){
        Log.d(TAG, "removeNumAvailableWords called");
        String songNumber = getCurrentSongNumber();
        int loc = getSongLocation(songNumber);
        if (loc != -1){
            ArrayList<Integer> wordsAvailableList = getWordsAvailableList();
            int prev = wordsAvailableList.get(loc);
            int newVal = prev - numToRemove;
            wordsAvailableList.set(loc, newVal);
            saveWordsAvailableList(wordsAvailableList);
        } else {
            Log.e(TAG, "Song not found in storage");
        }


    }

    private ArrayList<Integer> getWordsAvailableList(){
        // if there is no words list object stored make one.
        if(!settings.contains(NUM_WORDS_AVAILABLE)){
            ArrayList<Integer> numWordsList = new ArrayList<>();
            for (int i = 0; i < 5; i++){
                numWordsList.add(0);
            }
            Gson gson = new Gson();
            String jsonWordsList = gson.toJson(numWordsList);
            editor.putString(NUM_WORDS_AVAILABLE, jsonWordsList);
            return numWordsList;
        } else {
            // find and return the array
            Gson gson = new Gson();
            String jsonWordsList = settings.getString(NUM_WORDS_AVAILABLE, null);
            Type type = new TypeToken<ArrayList<Integer>>(){}.getType();
            return gson.fromJson(jsonWordsList, type);
        }
    }

    private void saveWordsAvailableList(ArrayList<Integer> wordsList){
        Gson gson = new Gson();
        String jsonWordsList = gson.toJson(wordsList);
        editor.putString(NUM_WORDS_AVAILABLE, jsonWordsList);
        editor.apply();
    }

    public void artistRevealed(){
        String songNumber = getCurrentSongNumber();
        int loc = getSongLocation(songNumber);
        if (loc != -1) {
            ArrayList<Boolean> currentArtistStatuses = getArtistList();
            currentArtistStatuses.set(loc, true);
            saveArtistList(currentArtistStatuses);
        }
    }

    public void artistHidden(){
        String songNumber = getCurrentSongNumber();
        int loc = getSongLocation(songNumber);
        if (loc != -1){
        ArrayList<Boolean> currentArtistStatuses = getArtistList();
        currentArtistStatuses.set(loc, false);
        saveArtistList(currentArtistStatuses);
        }
    }

    public boolean isArtistRevealed(){
        String songNumber = getCurrentSongNumber();
        int loc = getSongLocation(songNumber);
        if (loc == - 1) return false;
        return getArtistList().get(loc);
    }

    private ArrayList<Boolean> getArtistList(){
        // if there is no artist list object stored make one.
        if(!settings.contains(ARTIST_REVEALED)){
            ArrayList<Boolean> artistList = new ArrayList<>();
            for (int i = 0; i < 5; i++){
                artistList.add(false);
            }
            Gson gson = new Gson();
            String jsonArtistList = gson.toJson(artistList);
            editor.putString(ARTIST_REVEALED, jsonArtistList);
            return artistList;
        } else {
            // find and return the array
            Gson gson = new Gson();
            String jsonArtistList = settings.getString(ARTIST_REVEALED, null);
            Type type = new TypeToken<ArrayList<Boolean>>(){}.getType();
            return gson.fromJson(jsonArtistList, type);
        }
    }

    private void saveArtistList(ArrayList<Boolean> artistList){
        Gson gson = new Gson();
        String jsonArtistList = gson.toJson(artistList);
        editor.putString(ARTIST_REVEALED, jsonArtistList);
        editor.apply();
    }

    public void saveIncorrectGuess(){
        editor.putBoolean(INCORRECT_GUESS, true);
        editor.apply();
    }

    public void resetIncorrectGuess(){
        editor.putBoolean(INCORRECT_GUESS, false);
    }

    public boolean getIncorrectGuess(){
        return settings.getBoolean(INCORRECT_GUESS, false);
    }



}
