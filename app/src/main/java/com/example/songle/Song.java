package com.example.songle;

import android.util.Log;

/**
 * Created by Paul Moore on 25-Oct-17.
 */

public class Song {
    private String number;
    private String artist;
    private String title;
    private String link;
    private String status; //"N" = not started, "I" = INCOMPLETE, "C" = COMPLETE
    private final String NOT_STARTED = "N";
    private final String INCOMPLETE = "I";
    private final String COMPLETE = "C";

    public Song(String number, String artist, String title, String link){
            this.number = number;
            this.artist = artist;
            this.title = title;
            this.link = link;
            this.status = NOT_STARTED; // when a song is added it should be viewed as INCOMPLETE

    }
    /**
     * Don't need setters apart from status as the only time the number etc. should be changed is
     * when a new song is made.
     * The status can change though, so it should be settable.
     */

    public String getNumber() {
        return number;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the song. Valid arguments are "N" - not started, "I" - INCOMPLETE,
     * "C" - COMPLETE
     * @param status - what the new status String should be
     */
    public void setStatus(String status) {
        if (status.equals(NOT_STARTED) || status.equals(INCOMPLETE) || status.equals(COMPLETE)) {
            this.status = status;
        } else {
            System.err.println("Invalid status chosen");
            Log.e("Song", "invalid status set");
        }
    }

    //may use to display list of songs to choose from
    public String showSong(){
        return "Song #" + number + " Status: " + showStatus();
    }

    public String showArtist(){
        return "Artist: " + artist;
    }

    public String showStatus(){
        if (status.equals(NOT_STARTED)){
            return "Not Started";
        } else if (status.equals(INCOMPLETE)){
            return "Incomplete";
        } else if (status.equals(COMPLETE)){
            return "Complete";
        } else {
            Log.e("Song", "Invalid status shown");
            return "Error: invalid status";
        }
    }

    public boolean isSongComplete(){
        if (status.equals(COMPLETE)) return true;
        else return false;
    }

    public boolean isSongIncomplete(){
        if (status.equals(INCOMPLETE)) return true;
        else return false;
    }

    public boolean isSongNotStarted(){
        if (status.equals(NOT_STARTED)) return true;
        else return false;
    }

    public String toString(){
        return "Song #" + number + " Status: " + status;
    }
}
