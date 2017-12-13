package com.example.songle;

import android.util.Log;

/**
 * Created by Paul Moore on 25-Oct-17.
 * Class for storing and using songs from the XML file
 */

public class Song {
    private final String number;
    private final String artist;
    private final String title;
    private final String link;
    private String status; //"N" = not started, "I" = INCOMPLETE, "C" = COMPLETE
    private final String NOT_STARTED = "N";
    private final String INCOMPLETE = "I";
    private final String COMPLETE = "C";

    Song(String number, String artist, String title, String link){
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

    String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    String getLink() {
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

    String showStatus(){
        switch (status) {
            case NOT_STARTED:
                return "Not Started";
            case INCOMPLETE:
                return "Incomplete";
            case COMPLETE:
                return "Complete";
            default:
                Log.e("Song", "Invalid status shown");
                return "Error: invalid status";
        }
    }

    boolean isSongComplete(){
        return status.equals(COMPLETE);
    }

    boolean isSongIncomplete(){
        return status.equals(INCOMPLETE);
    }

    boolean isSongNotStarted(){
        return status.equals(NOT_STARTED);
    }

    public String toString(){
        return "Song #" + number + " Status: " + status;
    }
}
