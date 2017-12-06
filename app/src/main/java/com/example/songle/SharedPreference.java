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
    private static final String LYRICS = "Lyrics";
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
    private static final String HEIGHT = "Height";
    private static final String ACHIEVEMENTS = "Achievements";
    private static final String SONG_INFO = "SongInfo";

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
        return getLyrics(songNumber) != null;
    }


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


    public void completeSong(String songNumber){
        Log.v(TAG, "completeSong called");
        //indicate the song as completed
        saveSongStatus(songNumber, "C");
        SongInfo thisSong = getSongInfo(songNumber);
        thisSong.resetSongInfo();
        saveSongInfo(songNumber, thisSong);
    }

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
