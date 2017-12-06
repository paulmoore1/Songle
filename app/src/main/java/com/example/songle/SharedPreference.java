package com.example.songle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentSender;
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
    private final Context context;
    private final SharedPreferences settings;
    private final SharedPreferences.Editor editor;
    private static final String TAG = "SharedPreference";
    private static final String PREFS_NAME = "SONGLE_APP";
    private static final String SONGS = "Songs";
    private static final String LYRICS = "Lyrics";
    private static final String SONG_INFO = "SongInfo";
    private static final String MAPS = "Maps";
    private static final String MAPS_INFO = "MapsInfo";
    private static final String TIMESTAMP = "Timestamp";
    private static final String CURRENT_SONG = "CurrentSong";
    private static final String CURRENT_SONG_NUMBER = "CurrentSongNumber";
    private static final String CURRENT_DIFFICULTY_LEVEL = "CurrentDifficultyLevel";
    private static final String HEIGHT = "Height";
    private static final String ACHIEVEMENTS = "Achievements";


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
        //Check there is a list of achievements
        if (songs == null){
            Log.e(TAG, "No achievements found when trying to update status");
            return;
        }
        Song song = getSong(songNumber, songs);
        //Check the song is actually there
        if (song == null){
            Log.e(TAG, "Song number not found in achievements list");
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
        //update the achievements list
        songs.set(targetNum, song);

        //save the updated achievements
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
            Log.d(TAG, "getAllSongs returned achievements");
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
                Log.e(TAG, "No achievements found");
                return null;
            }
            Log.d(TAG, "Full list of achievements: " + songs.toString());
            //remove all achievements which have not been started, so only complete or incomplete ones are returned.
            Iterator<Song> iter = songs.iterator();
            while (iter.hasNext()){
                Song song = iter.next();
                if (song.isSongNotStarted()) iter.remove();
            }

            Log.d(TAG, "New list of achievements: " + songs.toString());
            Log.d(TAG, "getOldSongs returned achievements");
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

    /**
     * Save the current difficulty level. Input difficulty must be valid or nothing happens.
     * @param diffLevel - difficulty level to save as.
     */
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

    /**
     * Get the current difficulty level
     * @return current difficulty level
     */
    public String getCurrentDifficultyLevel(){
        Log.d(TAG, "getCurrentDifficultyLevel called");
        String diffLevel = null;
        if (settings.contains(CURRENT_DIFFICULTY_LEVEL)){
            diffLevel = settings.getString(CURRENT_DIFFICULTY_LEVEL, null);

        }
        return diffLevel;
    }

    /**
     * Get the map number based on the current difficulty setting
     * @return number of the map for that difficulty, 1 by default.
     */
    public String getCurrentMapNumber(){
        Log.d(TAG, "getCurrentMapNumber called");
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
        HashMap<String, List<Placemark>> allMaps = new HashMap<>();
        Gson gson = new Gson();
        //If there are no previously saved maps then create them
        if (!settings.contains(MAPS)){
            allMaps.put(mapNumber, placemarks);
            String jsonAllMaps = gson.toJson(allMaps);
            editor.putString(MAPS, jsonAllMaps);
            editor.apply();
        } else {
            //Found old maps, put the new value in
            String jsonAllMaps = settings.getString(MAPS, null);
            if (jsonAllMaps != null) {
                Type type = new TypeToken<HashMap<String, List<Placemark>>>() {
                }.getType();
                allMaps = gson.fromJson(jsonAllMaps, type);
                allMaps.put(mapNumber, placemarks);
                jsonAllMaps = gson.toJson(allMaps);
                editor.putString(MAPS, jsonAllMaps);
                editor.apply();
                //Save the map information as well
                try {
                    int songNum = Integer.parseInt(songNumber);
                    saveMapsInfo(mapNumber, songNum, placemarks.size());
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Tried to pass invalid song number: " + e);
                }
            } else {
                Log.e(TAG, "Found null jsonAllMaps");
            }
        }
    }

    /**
     * Get the placemarks for a certain map
     * @param mapNum - number of the map
     * @return - placemarks if they exist, null otherwise
     */
    public ArrayList<Placemark> getMap(String mapNum){
        Log.v(TAG, "getMap called for map #" + mapNum);
        Gson gson = new Gson();
        String jsonAllMaps = settings.getString(MAPS, null);
        if (jsonAllMaps != null) {
            Type type = new TypeToken<HashMap<String, List<Placemark>>>() {}.getType();
            HashMap<String, List<Placemark>> allMaps = gson.fromJson(jsonAllMaps, type);
            return (ArrayList<Placemark>) allMaps.get(mapNum);
        } else {
            return null;
        }
    }

    /**
     * Saves the information associated with a map - number of words and song.
     * @param mapNumber  - Key to identify which map is being referred to
     * @param songNumber - Number of song the map is of
     * @param numWords - Number of words in the map
     */
    private void saveMapsInfo(String mapNumber, int songNumber, int numWords){
        ArrayList<Integer> ints = new ArrayList<>(Arrays.asList(songNumber, numWords));
        HashMap<String, ArrayList<Integer>> mapInfo = new HashMap<>();

        Gson gson = new Gson();
        if (!settings.contains(MAPS_INFO)){
            mapInfo.put(mapNumber, ints);
            String jsonMapInfo = gson.toJson(mapInfo);
            editor.putString(MAPS_INFO, jsonMapInfo);
            editor.apply();

        } else {
            String jsonMapInfo = settings.getString(MAPS_INFO, null);
            if (jsonMapInfo != null){
                Type type = new TypeToken<HashMap<String, ArrayList<Integer>>>(){}.getType();
                mapInfo = gson.fromJson(jsonMapInfo, type);
                mapInfo.put(mapNumber, ints);
                jsonMapInfo = gson.toJson(mapInfo);
                editor.putString(MAPS_INFO, jsonMapInfo);
                editor.apply();
            } else {
                Log.e(TAG, "No map found");
            }
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
        Gson gson = new Gson();
        String jsonMapInfo = settings.getString(MAPS_INFO, null);
        if (jsonMapInfo != null){
            Type type = new TypeToken<HashMap<String, ArrayList<Integer>>>(){}.getType();
            HashMap<String, ArrayList<Integer>> mapInfo = gson.fromJson(jsonMapInfo, type);
            //Return false if there are not five entries
            if (mapInfo.size() != 5){
                Log.e(TAG, "Found this many entries in the Map Info HashMap: " + mapInfo.size());
                return false;
            }
            for (String mapNumber : mapInfo.keySet()){
                //Song number is stored at index 0 of the list (in the map)
                result = result && (songNum == mapInfo.get(mapNumber).get(0));
            }
            return result;
        } else {
            Log.e(TAG, "No map found");
            return false;
        }
    }

    /**
     * Get the number of words for a map
     * @param mapNumber - number of the map being queried
     * @return the number of words if that map is valid, 1 otherwise
     */
    public int getMapNumWords(String mapNumber){
        Log.v(TAG, "getMapNumWords called");
        Gson gson = new Gson();
        String jsonMapInfo = settings.getString(MAPS_INFO, null);
        if (jsonMapInfo != null){
            Type type = new TypeToken<HashMap<String, ArrayList<Integer>>>(){}.getType();
            HashMap<String, ArrayList<Integer>> mapInfo = gson.fromJson(jsonMapInfo, type);
            //Number of words is stored at index 1 of the list (in the map)
            return mapInfo.get(mapNumber).get(1);
        } else {
            Log.e(TAG, "No map found");
            //Return 1 as we divide by the number of words, so dividing by 0 would not be good!
            return 1;
        }

    }

    /**
     * Check if the lyrics for a certain song are stored.
     * @param songNumber - number of song being checked
     * @return true if the song is stored in the lyrics, false otherwise
     */
    public boolean checkLyricsStored(String songNumber){
        Log.v(TAG, "checkLyricsStored called");
        return getLyrics(songNumber) != null;
    }

    /**
     * Saves the lyrics for a song in the shared preferences
     * @param songNumber - number associated with the song
     * @param lyrics - the lyrics for that song
     */
    public void saveLyrics(String songNumber, HashMap<String, ArrayList<String>> lyrics){
        HashMap<String, HashMap<String, ArrayList<String>>> allLyrics = new HashMap<>();
        Gson gson = new Gson();
        //If there are no lyrics previously saved make a new object and save it
        if (!settings.contains(LYRICS)) {
            allLyrics.put(songNumber, lyrics);
            String jsonAllLyrics = gson.toJson(allLyrics);
            editor.putString(LYRICS, jsonAllLyrics);
            editor.apply();
        } else {
            //There are previous lyrics
            String jsonAllLyrics = settings.getString(LYRICS, null);
            if (jsonAllLyrics != null){
                Type type = new TypeToken<HashMap<String, HashMap<String, ArrayList<String>>>>(){}.getType();
                allLyrics = gson.fromJson(jsonAllLyrics, type);
                allLyrics.put(songNumber, lyrics);
                jsonAllLyrics = gson.toJson(allLyrics);
                editor.putString(LYRICS, jsonAllLyrics);
                editor.apply();
            }
        }
    }

    /**
     * Get the lyrics associated with a certain song
     * @param songNumber - number of the song the lyrics are being looked for
     * @return the lyrics if they are there, null otherwise
     */
    public HashMap<String, ArrayList<String>> getLyrics(String songNumber){
        String jsonAllLyrics = settings.getString(LYRICS, null);
        if (jsonAllLyrics != null){
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, HashMap<String, ArrayList<String>>>>(){}.getType();
            HashMap<String, HashMap<String, ArrayList<String>>> allLyrics = gson.fromJson(jsonAllLyrics, type);
            return allLyrics.get(songNumber);
        } else {
            return null;
        }
    }

    /**
     * Mark a song as completed. This updates its status and resets its associated information
     * @param songNumber - song to mark as complete
     */
    public void completeSong(String songNumber){
        Log.v(TAG, "completeSong called");
        //indicate the song as completed
        saveSongStatus(songNumber, "C");
        SongInfo thisSong = getSongInfo(songNumber);
        thisSong.resetSongInfo();
        saveSongInfo(songNumber, thisSong);
    }

    /**
     * Save the information associated with a song
     * @param songNumber - number of that song
     * @param info - all the necessary information about it
     */
    public void saveSongInfo(String songNumber, SongInfo info){
        //create a place to save the info if it isn't there already
        HashMap<String, SongInfo> songInfos = new HashMap<>();
        Gson gson = new Gson();
        if (!settings.contains(SONG_INFO)){
            songInfos.put(songNumber, info);
            String jsonInfo = gson.toJson(songInfos);
            editor.putString(SONG_INFO, jsonInfo);
            editor.apply();
        } else {
            String jsonInfo = settings.getString(SONG_INFO, null);
            if (jsonInfo != null){
                Type type = new TypeToken<HashMap<String, SongInfo>>(){}.getType();
                songInfos = gson.fromJson(jsonInfo, type);
                songInfos.put(songNumber, info);
                jsonInfo = gson.toJson(songInfos);
                editor.putString(SONG_INFO, jsonInfo);
                editor.apply();
            } else {
                Log.e(TAG, "Found null string");
            }
        }
    }

    /**
     * Get the information associated with a song
     * @param songNumber - number of that song
     * @return - the information if it exists, null otherwise
     */
    public SongInfo getSongInfo(String songNumber){
        String jsonInfo = settings.getString(SONG_INFO, null);
        if (jsonInfo != null){
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, SongInfo>>(){}.getType();
            HashMap<String, SongInfo> songInfos = gson.fromJson(jsonInfo, type);
            return songInfos.get(songNumber);
        } else {
            Log.e(TAG, "Found null string");
            return null;
        }
    }

    public void saveHeight(int height){
        editor.putInt(HEIGHT, height);
        editor.apply();
    }

    public int getHeight(){
        if (settings.contains(HEIGHT))return settings.getInt(HEIGHT, 180);
        else return -1;
    }

    public void saveAchievements(List<Achievement> achievements){
        Gson gson = new Gson();
        String jsonAchievements = gson.toJson(achievements);
        editor.putString(ACHIEVEMENTS, jsonAchievements);
        editor.apply();
    }
/*
    public HashMap<String, Achievement> getAchievements(){
        if (settings.contains(ACHIEVEMENTS)){
            Gson gson = new Gson();
            String jsonAchievements = settings.getString(ACHIEVEMENTS, null);
            Type type = new TypeToken<HashMap<String, Achievement>>(){}.getType();
            return gson.fromJson(jsonAchievements, type);
        } else {
            return null;
        }
    }
  */
    public List<Achievement> getAchievements(){
        if (settings.contains(ACHIEVEMENTS)){
            Gson gson = new Gson();
            String jsonAchievements = settings.getString(ACHIEVEMENTS, null);
            Type type = new TypeToken<List<Achievement>>(){}.getType();
            List<Achievement> achievements = gson.fromJson(jsonAchievements, type);
            //only return achievements which aren't hidden
            if (achievements != null){
                Iterator<Achievement> i = achievements.iterator();
                while (i.hasNext()){
                    Achievement a = i.next();
                    if (a.isHidden() && !a.isAchieved()) i.remove();
                }
            }
            return achievements;
        } else {
            return null;
        }
    }
}
