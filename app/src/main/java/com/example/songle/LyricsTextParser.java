package com.example.songle;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by Paul Moore on 28-Oct-17.
 * Parses txt files
 */

//Some help found here: https://stackoverflow.com/questions/5819772/java-parsing-text-file
class LyricsTextParser {
    private static final String TAG = "LyricsTextParser";
    private final SharedPreference sharedPreference;
    private final String songNumber;
    private int currentLineNum = 1;
    private final HashMap<String, ArrayList<String>> lyrics;
    private final ArrayList<String> sizes;


    public LyricsTextParser(Context context, String songNumber) {
        Log.d(TAG, "Lyrics Text Parser created");
        this.lyrics = new HashMap<>();
        this.sharedPreference = new SharedPreference(context);
        this.songNumber = songNumber;
        this.sizes = new ArrayList<>(20);
    }

    public void parse(InputStream stream) {
        Scanner scanner;
        scanner = new Scanner(stream);
        Log.d(TAG, "parsing lyrics started");
        while (scanner.hasNext()) {
            //read line and split on spaces
            String[] fullLine = scanner.nextLine().split(" ");
            int n = fullLine.length;
            // length of index for making keys of the line.
            int m;
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
                ArrayList<String> lyric = new ArrayList<>(2);
                lyric.add(word);
                lyric.add(bool);
                lyrics.put(key, lyric);
            }
            //put the length of the line in the array so we can show blank lines correctly.
            sizes.add(currentLineNum - 1, Integer.toString(m - 1));

            currentLineNum++;
        }
        scanner.close();
        try {
            stream.close();
        } catch (IOException e){
            Log.e(TAG, "Tried to close stream, gave exception: " + e);
        }
        lyrics.put("SIZE", sizes);
        sharedPreference.saveLyrics(songNumber, lyrics);

        Log.d(TAG, "Lyrics saved");
    }




}
