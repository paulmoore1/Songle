package com.example.songle;

import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by Paul on 13/12/2017.
 */
public class SongInfoTest {
    private final SongInfo songInfoTest = new SongInfo("name", "artist", "link");

    @Test
    public void getName() throws Exception {
        assertEquals("name", songInfoTest.getName());
    }

    @Test
    public void getArtist() throws Exception {
        assertEquals("artist", songInfoTest.getArtist());
    }

    @Test
    public void getLink() throws Exception {
        assertEquals("link", songInfoTest.getLink());
    }

    @Test
    public void getNumWordsFound() throws Exception {
        assertEquals(0, songInfoTest.getNumWordsFound());
    }

    @Test
    public void getNumWordsAvailable() throws Exception {
        assertEquals(0, songInfoTest.getNumWordsAvailable());
    }

    @Test
    public void incrementNumWordsFound() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(0, changeableSongInfo.getNumWordsFound());
        assertEquals(0, changeableSongInfo.getNumWordsAvailable());

        changeableSongInfo.incrementNumWordsFound();

        assertEquals(1, changeableSongInfo.getNumWordsFound());
        assertEquals(1, changeableSongInfo.getNumWordsAvailable());
    }

    @Test
    public void removeNumWordsAvailable() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(0, changeableSongInfo.getNumWordsFound());
        assertEquals(0, changeableSongInfo.getNumWordsAvailable());

        changeableSongInfo.incrementNumWordsFound();
        changeableSongInfo.incrementNumWordsFound();

        assertEquals(2, changeableSongInfo.getNumWordsFound());
        assertEquals(2, changeableSongInfo.getNumWordsAvailable());

        changeableSongInfo.removeNumWordsAvailable(2);
        assertEquals(2, changeableSongInfo.getNumWordsFound());
        assertEquals(0, changeableSongInfo.getNumWordsAvailable());

        changeableSongInfo.removeNumWordsAvailable(2);
        assertEquals(0, changeableSongInfo.getNumWordsAvailable());
    }

    @Test
    public void addDistance() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(0f, changeableSongInfo.getDistanceWalked(), 0.01);

        changeableSongInfo.addDistance(1.96f);
        assertEquals(1.96f, changeableSongInfo.getDistanceWalked(), 0.01);
    }

    @Test
    public void getDistanceWalked() throws Exception {
        assertEquals(0f, songInfoTest.getDistanceWalked(), 0.01);
    }

    @Test
    public void isIncorrectlyGuessed() throws Exception {
        assertEquals(false, songInfoTest.isIncorrectlyGuessed());
    }

    @Test
    public void setIncorrectlyGuessed() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(false, changeableSongInfo.isIncorrectlyGuessed());

        changeableSongInfo.setIncorrectlyGuessed();
        assertEquals(true, changeableSongInfo.isIncorrectlyGuessed());
    }

    @Test
    public void isArtistRevealed() throws Exception {
        assertEquals(false, songInfoTest.isArtistRevealed());
    }

    @Test
    public void setArtistRevealed() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(false, changeableSongInfo.isArtistRevealed());

        changeableSongInfo.setArtistRevealed();
        assertEquals(true, changeableSongInfo.isArtistRevealed());
    }

    @Test
    public void isLineRevealed() throws Exception {
        assertEquals(false, songInfoTest.isLineRevealed());
    }

    @Test
    public void setLineRevealed() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(false, changeableSongInfo.isLineRevealed());

        changeableSongInfo.setLineRevealed();
        assertEquals(true, changeableSongInfo.isLineRevealed());
    }

    @Test
    public void resetSongInfo() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");

        //Change values
        changeableSongInfo.incrementNumWordsFound();
        changeableSongInfo.addDistance(3.14f);
        changeableSongInfo.setIncorrectlyGuessed();
        changeableSongInfo.setArtistRevealed();
        changeableSongInfo.setLineRevealed();

        //Reset the song
        changeableSongInfo.resetSongInfo();

        //Check the values were all reset
        assertEquals("name", changeableSongInfo.getName());
        assertEquals("artist", changeableSongInfo.getArtist());
        assertEquals("link", changeableSongInfo.getLink());
        assertEquals(0, changeableSongInfo.getNumWordsFound());
        assertEquals(0, changeableSongInfo.getNumWordsAvailable());
        assertEquals(0f, changeableSongInfo.getDistanceWalked(), 0.01);
        assertEquals(false, changeableSongInfo.isIncorrectlyGuessed());
        assertEquals(false, changeableSongInfo.isArtistRevealed());
        assertEquals(false, changeableSongInfo.isLineRevealed());
    }

    @Test
    public void getTimeTaken() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        long currentTime = System.currentTimeMillis();
        String target = String.format(Locale.getDefault(),"%d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(currentTime),
                TimeUnit.MILLISECONDS.toMinutes(currentTime),
                TimeUnit.MILLISECONDS.toSeconds(currentTime));

        assertEquals(target, changeableSongInfo.getTimeTaken());

    }

    @Test
    public void minutesTaken() throws Exception {
        SongInfo changeableSongInfo = new SongInfo("name", "artist", "link");
        assertEquals(0, changeableSongInfo.minutesTaken());
    }

}