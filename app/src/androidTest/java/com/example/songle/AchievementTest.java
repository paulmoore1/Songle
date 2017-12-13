package com.example.songle;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Paul on 13/12/2017.
 * Tests the achievement class works as expected
 */
public class AchievementTest {
    private final Achievement testAchievement = new Achievement("title", "description",
            100, 1234, 5678, false);

    @Test
    public void testGetTitle() throws Exception {
        assertEquals("title", testAchievement.getTitle());
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("description", testAchievement.getDescription());
    }

    @Test
    public void testGetSteps() throws Exception {
        assertEquals(0, testAchievement.getSteps());
    }

    @Test
    public void testGetGreyPictureID() throws Exception {
        assertEquals(1234, testAchievement.getGreyPictureID());
    }

    @Test
    public void testGetColorPictureID() throws Exception {
        assertEquals(5678, testAchievement.getColorPictureID());
    }

    @Test
    public void testIsAchieved() throws Exception {
        assertEquals(false, testAchievement.isAchieved());
    }

    @Test
    public void testIsHidden() throws Exception {
        assertEquals(false, testAchievement.isHidden());
    }

    @Test
    public void testAddingSteps() throws Exception {
        Achievement ach10steps = new Achievement("title", "description",
                10, 1234, 5678, false);
        assertEquals(0, ach10steps.getSteps());
        ach10steps.incrementSteps();
        assertEquals(1, ach10steps.getSteps());
        ach10steps.setSteps(8);
        assertEquals(8, ach10steps.getSteps());
    }

    @Test
    public void testPercentProgress() throws Exception {
        Achievement ach10steps = new Achievement("title", "description",
                10, 1234, 5678, false);
        String noProgress = "0% complete";
        String halfway = "50% complete";
        String complete = "Completed!";
        assertEquals(noProgress, ach10steps.getPercentProgress());

        ach10steps.setSteps(5);
        assertEquals(halfway, ach10steps.getPercentProgress());

        ach10steps.setSteps(10);
        assertEquals(complete, ach10steps.getPercentProgress());

        ach10steps.setSteps(20);
        assertEquals(complete, ach10steps.getPercentProgress());
    }

    @Test
    public void testToString() throws Exception {
        String target = "title" +
                "\ndescription" +
                "\nSteps so far: 0" +
                "\nSteps required: 100" +
                "\nHidden: false" +
                "\nAchieved: false";
        assertEquals(target, testAchievement.toString());
    }

}