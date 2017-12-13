package com.example.songle;

import android.support.v4.widget.TextViewCompat;
import android.widget.Toast;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.example.songle.HomeActivity;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class HomeActivityUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testUpdateAchievement(){
        Achievement test = new Achievement("Title", "Description",
                100, 1234, 1234, false);
        boolean actual = false;


    }


    /*
    private boolean updateAchievement(Achievement achievement){
        if (achievement != null){
            achievement.incrementSteps();
            if (achievement.isAchieved()){
                achievementComplete.start();
                Toast.makeText(this, "Achievement unlocked: " + achievement.getTitle(), Toast.LENGTH_SHORT).show();
                sharedPreference.saveAchievement(achievement);
                return true;
            } else{
                sharedPreference.saveAchievement(achievement);
            }
        }
        return false;
    }
     */
}
