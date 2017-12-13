package com.example.songle;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Paul on 06/12/2017.
 * A class for storing information about the songs
 */

public class SongInfo {
    private final String name;
    private final String artist;
    private final String link;
    private int numWordsFound;
    private int numWordsAvailable;
    private float distanceWalked;
    private boolean incorrectlyGuessed;
    private boolean artistRevealed;
    private boolean lineRevealed;
    private long timeStarted;

    SongInfo(String name, String artist, String link){
        this.name = name;
        this.artist = artist;
        this.link = link;
        numWordsFound= 0;
        numWordsAvailable = 0;
        distanceWalked = 0;
        incorrectlyGuessed = false;
        artistRevealed = false;
        lineRevealed = false;
        timeStarted = System.currentTimeMillis();
    }

    public String getName(){
        return name;
    }

    String getArtist(){
        return artist;
    }

    String getLink(){
        return link;
    }

    int getNumWordsFound(){
        return numWordsFound;
    }

    int getNumWordsAvailable(){
        return numWordsAvailable;
    }

    void incrementNumWordsFound(){
        numWordsFound++;
        numWordsAvailable++;
    }

    void removeNumWordsAvailable(int num){
        if (numWordsAvailable >= num) numWordsAvailable -= num;
        else numWordsAvailable = 0;
    }

    void addDistance(float addedDistance){
        distanceWalked += addedDistance;
    }

    float getDistanceWalked(){
        return distanceWalked;
    }

    boolean isIncorrectlyGuessed(){
        return incorrectlyGuessed;
    }

    void setIncorrectlyGuessed(){
        incorrectlyGuessed = true;
    }

    boolean isArtistRevealed(){
        return artistRevealed;
    }

    void setArtistRevealed(){
        artistRevealed = true;
    }

    boolean isLineRevealed(){
        return lineRevealed;
    }

    void setLineRevealed(){
        lineRevealed = true;
    }

    void resetSongInfo(){
        numWordsFound = 0;
        numWordsAvailable = 0;
        distanceWalked = 0;
        incorrectlyGuessed = false;
        artistRevealed = false;
        lineRevealed = false;
        timeStarted = System.currentTimeMillis();
    }

    public String getTimeTaken(){
        long finishTime = System.currentTimeMillis();
        long timeTaken = finishTime - timeStarted;

        return String.format(Locale.getDefault(),"%d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(timeTaken),
                TimeUnit.MILLISECONDS.toMinutes(timeTaken),
                TimeUnit.MILLISECONDS.toSeconds(timeTaken));
    }

    int minutesTaken(){
        long timeFinished = System.currentTimeMillis();
        long timeTaken = timeFinished - timeStarted;
        //safe to cast to int - don't expect it to take 65,000 minutes!
        return (int) TimeUnit.MILLISECONDS.toMinutes(timeTaken);
    }



}
