package com.example.songle;
import android.support.test.espresso.Espresso;
import com.example.songle.HomeActivity;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by Paul on 04/12/2017.
 */

public class HomeActivityTest {
    @Test
    public void testClickNewGame(){
        onView(withId(R.id.btn_new_game))
                .perform(click());
        intended(hasComponent(GameSettingsActivity.class.getName()));
    }

    @Test
    public void testClickContinueGame(){
        onView(withId(R.id.btn_continue_game))
                .perform(click());
        intended(hasComponent(MainGameActivity.class.getName()));
    }

}
