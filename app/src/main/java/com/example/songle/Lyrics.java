package com.example.songle;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by s1531206 on 03/11/17.
 */

public class Lyrics {
    private static final String TAG = "Lyrics";
    private Map<String, List<String>> lyrics;

    public Lyrics(){
        super();
    }

    public Lyrics(Context context, File file){
        LyricsTextParser ltp = new LyricsTextParser(context, file);
        ltp.parse();
        ltp.saveLyrics(context);
    }

    public Map<String, List<String>> getLyrics() {
        return lyrics;
    }

    /**
     * Returns the lyrics associated with a particular song number.
     * @param context
     * @param songNumber - the song number for the lyrics
     * @return - a Map containing the lyrics, null if the file doesn't exist
     */
    public void loadLyrics(Context context, String songNumber) {

        String filename = songNumber + "lyrics.tmp";
        try {
            FileInputStream fis = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, List<String>> lyrics= (Map<String, List<String>>) ois.readObject();
            ois.close();
            fis.close();
            this.lyrics = lyrics;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not find file for song: " + songNumber);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Could not set up ObjectInputStream");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Error in file - does not cast to Map<String, List<String>>");
            e.printStackTrace();
        }
        return;

    }

}
