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
import java.io.InputStream;
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
    private Context context;
    private SharedPreference sharedPreference = new SharedPreference();
    private InputStream stream;
    private File file;
    private int currentLineNum = 1;
    private HashMap<String, ArrayList<String>> lyrics;
    private ArrayList<Integer> sizes;


    public LyricsTextParser(Context context) {
        Log.d(TAG, "Lyrics Text Parser created");
        this.lyrics = new HashMap<String, ArrayList<String>>();
        this.context = context;
        this.sizes = new ArrayList<>(20);
    }

    public void parse(InputStream stream) {
        Scanner scanner = null;
        scanner = new Scanner(stream);
        Log.d(TAG, "parsing lyrics started");
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
                ArrayList<String> lyric = new ArrayList<String>(2);
                lyric.add(word);
                lyric.add(bool);
                lyrics.put(key, lyric);
            }
            //put the length of the line in the array so we can show blank lines correctly.
            sizes.add(currentLineNum - 1, m - 1);

            currentLineNum++;
            Log.v(TAG, "Line number: " + currentLineNum);
        }
        scanner.close();
        Log.d(TAG, "Lyrics saved");
        sharedPreference.saveLyrics(context, lyrics);
        sharedPreference.saveLyricDimensions(context, sizes);


    }




}
