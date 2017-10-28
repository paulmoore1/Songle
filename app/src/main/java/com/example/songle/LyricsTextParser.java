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
import java.util.Collections;
import java.util.Scanner;


//Some help found here: https://stackoverflow.com/questions/5819772/java-parsing-text-file
public class LyricsTextParser {
    public static final String TAG = "LyricsTextParser";

    private ArrayList<ArrayList<String>> lyrics;
    private ArrayList<ArrayList<Boolean>> lyricsFound;
    private ArrayList<String> line;
    private ArrayList<Boolean> bools;
    private File file;
    private int currentLineNum = 1;

// check for the file extension found here: https://stackoverflow.com/questions/25298691/how-to-check-the-file-type-in-java
    public LyricsTextParser(File file) {
        //check the file ends in '.txt'
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
        if (extension.equals("txt")) {
            lyrics = new ArrayList<ArrayList<String>>();
            lyricsFound = new ArrayList<ArrayList<Boolean>>();
            this.file = file;
        } else {
            Log.e(TAG, "File not of the form .txt");
            this.file = null;
        }
    }

    public static String getFileExtension(String fullName) {

        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
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
            //removing the line number is slightly different depending on number of digits
            if (currentLineNum < 10) {
                fullLine = Arrays.copyOfRange(fullLine, 5, fullLine.length);
                //removes number from the first word.
                fullLine[0] = fullLine[0].substring(2, fullLine[0].length());
            } else if (currentLineNum < 100) {
                fullLine = Arrays.copyOfRange(fullLine, 4, fullLine.length);
                //removes number from the first word.
                fullLine[0] = fullLine[0].substring(3, fullLine[0].length());
            } else {
                //cover rare case when there are more than 100 lines - don't expect but possible
                fullLine = Arrays.copyOfRange(fullLine, 3, fullLine.length);
                //removes number from the first word.
                fullLine[0] = fullLine[0].substring(4, fullLine[0].length());
            }
            //put line into lyrics array
            line = new ArrayList<>(Arrays.asList(fullLine));
            lyrics.add(currentLineNum - 1, line);

            //set all values for the lyrics to false and store
            bools = new ArrayList<>(Arrays.asList(new Boolean[line.size()]));
            Collections.fill(bools, Boolean.FALSE);
            lyricsFound.add(currentLineNum - 1, bools);

            //increment index
            currentLineNum++;
        }
        scanner.close();
    }

    public void saveLyrics(Context context) {

        try {
            FileOutputStream fos = context.openFileOutput("words.tmp", Context.MODE_PRIVATE);
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

    public ArrayList<ArrayList<String>> loadLyrics(Context context) {
        try {
            FileInputStream fis = context.openFileInput("words.tmp");
            ObjectInputStream ois = new ObjectInputStream(fis);
            @SuppressWarnings("unchecked")
            ArrayList<ArrayList<String>> lyrics = (ArrayList<ArrayList<String>>) ois.readObject();
            ois.close();
            fis.close();
            return lyrics;
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Could not set up ObjectInputStream");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Error in file - does not cast to ArrayList<ArrayList<String>>");
            e.printStackTrace();
        }
        return null;

    }

    public void saveBools(Context context) {

        try {
            FileOutputStream fos = context.openFileOutput("words.tmp", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(lyricsFound);
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

    public ArrayList<ArrayList<Boolean>> loadLyricBools(Context context) {
        try {
            FileInputStream fis = context.openFileInput("words.tmp");
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<ArrayList<Boolean>> bools = (ArrayList<ArrayList<Boolean>>) ois.readObject();
            ois.close();
            fis.close();
            return bools;
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Could not set up ObjectInputStream");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Error in file - does not cast to ArrayList<ArrayList<String>>");
            e.printStackTrace();
        }
        return null;

    }
}
