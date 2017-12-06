package com.example.songle;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Paul on 06/12/2017.
 */

public class SongInfo {
    private String name;
    private String artist;
    private String link;
    private int numWordsFound;
    private int numNumWordsAvailable;
    private boolean incorrectlyGuessed;
    private boolean artistRevealed;
    private boolean lineRevealed;
    private long timeStarted;

    public SongInfo(String name, String artist, String link){
        this.name = name;
        this.artist = artist;
        this.link = link;
        numWordsFound= 0;
        numNumWordsAvailable = 0;
        incorrectlyGuessed = false;
        artistRevealed = false;
        lineRevealed = false;
        timeStarted = System.currentTimeMillis();
    }

    public String getName(){
        return name;
    }

    public String getArtist(){
        return artist;
    }

    public String getLink(){
        return link;
    }

    public int getNumWordsFound(){
        return numWordsFound;
    }

    public int getNumWordsAvailable(){
        return  numNumWordsAvailable;
    }

    public void incrementNumWordsFound(){
        numWordsFound++;
        numNumWordsAvailable++;
    }

    public void removeNumWordsAvailable(int num){
        numNumWordsAvailable -= num;
    }


    public boolean isIncorrectlyGuessed(){
        return incorrectlyGuessed;
    }

    public void setIncorrectlyGuessed(){
        incorrectlyGuessed = true;
    }

    public boolean isArtistRevealed(){
        return artistRevealed;
    }

    public void setArtistRevealed(){
        artistRevealed = true;
    }

    public boolean isLineRevealed(){
        return lineRevealed;
    }

    public void setLineRevealed(){
        lineRevealed = true;
    }

    public void resetSongInfo(){
        numWordsFound = 0;
        numNumWordsAvailable = 0;
        incorrectlyGuessed = false;
        artistRevealed = false;
        lineRevealed = false;
    }

    public String getTimeTaken(){
        return String.format(Locale.getDefault(),"%d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(timeStarted),
                TimeUnit.MILLISECONDS.toMinutes(timeStarted),
                TimeUnit.MILLISECONDS.toSeconds(timeStarted));
    }



}
