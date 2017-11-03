package com.example.songle;

/**
 * Created by Paul Moore on 28-Oct-17.
 * Parses txt files
 */


import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


//Some help found here: https://stackoverflow.com/questions/5819772/java-parsing-text-file
public class LyricsTextParser {
    public static final String TAG = "LyricsTextParser";
    private File file;
    private int currentLineNum = 1;
    private Map<String, List<String>> lyrics;

// check for the file extension found here: https://stackoverflow.com/questions/25298691/how-to-check-the-file-type-in-java
    public LyricsTextParser(File file) {
        //check the file ends in '.txt'
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
        if (extension.equals("txt")) {
            this.lyrics = new HashMap<String, List<String>>();
            this.file = file;
        } else {

            this.file = null;
        }
    }

    public void parse() {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file");
            e.printStackTrace();
            return;
        }
        while (scanner.hasNext()) {
            //read line and split on spaces
            String[] fullLine = scanner.nextLine().split(" ");
            int n = fullLine.length;
            // length of index for making keys of the line.
            int m = 0;
            //removing the line number is slightly different depending on number of digits
            if (currentLineNum < 10) {
                fullLine = Arrays.copyOfRange(fullLine, 5, n);
                //removes number from the first word.
                fullLine[0] = fullLine[0].substring(2, fullLine[0].length());
                m = n - 4;
            } else if (currentLineNum < 100) {
                fullLine = Arrays.copyOfRange(fullLine, 4, n);
                //removes number from the first word.
                fullLine[0] = fullLine[0].substring(3, fullLine[0].length());
                m = n - 3;
            } else {
                //cover rare case when there are more than 100 lines - don't expect but possible
                fullLine = Arrays.copyOfRange(fullLine, 3, n);
                //removes number from the first word.
                fullLine[0] = fullLine[0].substring(4, fullLine[0].length());
                m = n - 2;
            }

            //make each word into a HashMap entry. Add one since first word is 1
            for (int i = 1; i < m; i++) {

                String key = String.valueOf(currentLineNum) + ":" + String.valueOf(i);
                String word = fullLine[i-1];
                String bool = "False";
                List<String> lyric = new ArrayList<String>(2);
                lyric.add(word);
                lyric.add(bool);
                lyrics.put(key, lyric);
            }

            currentLineNum++;
        }
        scanner.close();
    }

    public void saveLyrics(Context context) {

        SharedPreference sharedPreference = new SharedPreference();
        String songNumber = sharedPreference.getCurrentSongNumber(context);
        String filename = songNumber + "lyrics.tmp";

        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(lyrics);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not create file");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Could not set up ObjectOutputStream");
            e.printStackTrace();
        }

    }

    /**
     * Returns the lyrics associated with a particular song number.
     * @param context
     * @param songNumber - the song number for the lyrics
     * @return - a Map containing the lyrics, null if the file doesn't exist
     */
    public Map<String, List<String>> loadLyrics(Context context, String songNumber) {

        String filename = songNumber + "lyrics.tmp";
        try {
            FileInputStream fis = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, List<String>> lyrics= (Map<String, List<String>>) ois.readObject();
            ois.close();
            fis.close();
            return lyrics;
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
        return null;

    }

}
