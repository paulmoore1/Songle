package com.example.songle;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Paul on 13/12/2017.
 * Tests that the song class works as expected
 */
public class SongTest {
    private final Song testSong = new Song("01", "artist", "title", "link");

    @Test
    public void getNumber() throws Exception {
        assertEquals("01", testSong.getNumber());
    }

    @Test
    public void getArtist() throws Exception {
        assertEquals("artist", testSong.getArtist());
    }

    @Test
    public void getTitle() throws Exception {
        assertEquals("title", testSong.getTitle());
    }

    @Test
    public void getLink() throws Exception {
        assertEquals("link", testSong.getLink());
    }

    @Test
    public void getStatus() throws Exception {
        assertEquals("N", testSong.getStatus());
    }

    @Test
    public void setStatus() throws Exception {
        Song changeableSong = new Song("01", "artist", "title", "link");
        assertEquals("N", changeableSong.getStatus());

        changeableSong.setStatus("I");
        assertEquals("I", changeableSong.getStatus());

        changeableSong.setStatus("C");
        assertEquals("C", changeableSong.getStatus());

        changeableSong.setStatus("Not valid status");
        assertEquals("C", changeableSong.getStatus());
    }

    @Test
    public void showStatus() throws Exception {
        Song changeableSong = new Song("01", "artist", "title", "link");
        assertEquals("Not Started", changeableSong.showStatus());

        changeableSong.setStatus("I");
        assertEquals("Incomplete", changeableSong.showStatus());

        changeableSong.setStatus("C");
        assertEquals("Complete", changeableSong.showStatus());

        changeableSong.setStatus("Not valid status");
        assertEquals("Complete", changeableSong.showStatus());

        changeableSong.setStatus("N");
        assertEquals("Not Started", changeableSong.showStatus());
    }

    @Test
    public void isSongComplete() throws Exception {
        assertEquals(false, testSong.isSongComplete());
    }

    @Test
    public void isSongIncomplete() throws Exception {
        assertEquals(false, testSong.isSongIncomplete());
    }

    @Test
    public void isSongNotStarted() throws Exception {
        assertEquals(true, testSong.isSongNotStarted());
    }


    @Test
    public void testToString() throws Exception {
        String target = "Song #01 Status: N";
        assertEquals(target, testSong.toString());
    }

}