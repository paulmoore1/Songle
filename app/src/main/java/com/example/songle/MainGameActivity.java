package com.example.songle;
//adapted from https://github.com/Suleiman19/Bottom-Navigation-Demo/blob/master/app/src/main/java/com/grafixartist/bottomnav/MainActivity.java


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;

public class MainGameActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainGameActivity";
    private final int[] colors = {R.color.maps_tab, R.color.words_tab, R.color.guess_tab};
    private NoSwipePager viewPager;
    private AHBottomNavigation bottomNavigation;

    //Keep track of shared preferences and any changes
    private SharedPreference sharedPreference;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    //Identifies the song
    private String songNumber;
    //Holds useful information about the song with the corresponding song number
    private SongInfo songInfo;

    //Tracks steps - used to calculate distance walked
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int prevSteps = 0;
    private float stepSize;
    private boolean waitingFirstStep;

    //Used to detect if a notification should be shown in the words or guessing tab
    private boolean wordsNotificationVisible = false;
    private boolean hintNotificationVisible = false;
    private int lastNumWordsFound = 0;

    //Give the user achievements for walking distances while playing
    private Achievement achWalk500;
    private Achievement achWalk5k;
    private Achievement achWalk10k;

    //Play sounds when appropriate.
    private static MediaPlayer achievementComplete;
    private static MediaPlayer tabSwitchSound;
    private static MediaPlayer hintSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_AUTO);

        super.onCreate(savedInstanceState);
        Log.d(TAG, "super onCreate called");
        setContentView(R.layout.main_game_layout);

        setupViewPager();

        sharedPreference = new SharedPreference(getApplicationContext());


        songNumber = sharedPreference.getCurrentSongNumber();
        songInfo = sharedPreference.getSongInfo(songNumber);
        lastNumWordsFound = songInfo.getNumWordsFound();
        stepSize = sharedPreference.getStepSize();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // Used so that the number of steps to expect is updated to the value it was when the activity is created.
        // This means that if the step count was updated elsewhere (e.g. on another app )
        // while the activity wasn't running, this won't affect the counted steps.
        waitingFirstStep = true;

        achievementComplete = MediaPlayer.create(this, R.raw.happy_jingle);
        tabSwitchSound = MediaPlayer.create(this, R.raw.tab_click);
        hintSound = MediaPlayer.create(this, R.raw.hint_notification);

        achWalk500 = sharedPreference.getIncompleteAchievement(getString(R.string.ach_walk_500_title));
        achWalk5k = sharedPreference.getIncompleteAchievement(getString(R.string.ach_walk_5k_title));
        achWalk10k = sharedPreference.getIncompleteAchievement(getString(R.string.ach_walk_10k_title));

        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                //update the song info
                songInfo = sharedPreference.getSongInfo(songNumber);
                int newNumWordsFound = songInfo.getNumWordsFound();
                int numWordsAvailable = songInfo.getNumWordsAvailable();
                //Only show notification if more words were found
                if (newNumWordsFound > lastNumWordsFound) {
                    lastNumWordsFound = newNumWordsFound;
                    createWordsFoundNotification();
                }
                //Show notification if there are enough words for a hint (for the line)
                int requiredNumForHint = requiredWordsForLine();
                if (numWordsAvailable >= requiredNumForHint) {
                    createHintNotification();
                }
            }
        };


        bottomNavigation = findViewById(R.id.bottom_navigation);
        setupBottomNavBehaviors();
        setupBottomNavStyle();

        addBottomNavigationItems();
        bottomNavigation.setCurrentItem(0);

        UiChangeListener();
/*
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
*/


        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
//                fragment.updateColor(ContextCompat.getColor(MainActivity.this, colors[position]));
                tabSwitchSound.start();
                if (!wasSelected)
                    viewPager.setCurrentItem(position);

                //allow translucent scrolling for middle position only.
                //    if(position == 1) bottomNavigation.setTranslucentNavigationEnabled(true);
                //    else bottomNavigation.setTranslucentNavigationEnabled(false);

                // remove notification badge if it was there
                int middleItemPos = 1;
                int lastItemPos = 2;
                if (wordsNotificationVisible && position == middleItemPos)
                    bottomNavigation.setNotification(new AHNotification(), middleItemPos);
                if (hintNotificationVisible && position == lastItemPos)
                    bottomNavigation.setNotification(new AHNotification(), lastItemPos);
                return true;
            }
        });

    }

    private void setupViewPager() {
        Log.v(TAG, "setupViewPager called");
        viewPager = findViewById(R.id.viewpager);
        viewPager.setPagingEnabled(false);
        BottomBarAdapter pagerAdapter = new BottomBarAdapter(getSupportFragmentManager());
        pagerAdapter.addFragments(new MapsFragment());
        pagerAdapter.addFragments(new WordsFragment());
        pagerAdapter.addFragments(new GuessFragment());
        /*
        pagerAdapter.addFragments(createMapFragment(R.color.maps_tab));
        pagerAdapter.addFragments(createWordsFragment(R.color.words_tab));
        pagerAdapter.addFragments(createGuessFragment(R.color.guess_tab));
*/
        viewPager.setAdapter(pagerAdapter);
    }

    private void createWordsFoundNotification() {
        Log.v(TAG, "createWordsFoundNotification called");
        hintSound.start();
        AHNotification notification = new AHNotification.Builder()
                .setText("!")
                .setBackgroundColor(getColor(R.color.colorBottomNavigationPrimaryDark))
                .setTextColor(Color.WHITE)
                .build();
        bottomNavigation.setNotification(notification, bottomNavigation.getItemsCount() - 2);
        wordsNotificationVisible = true;
    }

    private void createHintNotification() {
        Log.v(TAG, "createHintNotification called");
        hintSound.start();
        AHNotification notification = new AHNotification.Builder()
                .setText("!")
                .setBackgroundColor(getColor(R.color.colorBottomNavigationPrimaryDark))
                .setTextColor(Color.WHITE)
                .build();
        bottomNavigation.setNotification(notification, bottomNavigation.getItemsCount() - 1);
        hintNotificationVisible = true;
    }


    private void setupBottomNavBehaviors() {
        Log.v(TAG, "setupBottomNavBehaviours called");
//       bottomNavigation.setBehaviorTranslationEnabled(false);

        /*
        Warning: Toolbar Clipping might occur. Solve this by wrapping it in a LinearLayout with a top
        View of 24dp (status bar size) height.
         */
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            bottomNavigation.setTranslucentNavigationEnabled(false);
        } else {
            bottomNavigation.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    //System bars are visible
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0){
                        bottomNavigation.setTranslucentNavigationEnabled(true);
                    } else {
                        bottomNavigation.setTranslucentNavigationEnabled(false);
                    }
                }
            });
            //bottomNavigation.setTranslucentNavigationEnabled(true);
        }

    }

    private void UiChangeListener(){
        final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0){
                        decorView.setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                }
            });
    }

    /**
     * Adds styling properties to {@link AHBottomNavigation}
     */
    private void setupBottomNavStyle() {
        Log.v(TAG, "setupBottomNavStyle called");
        /*
        Set Bottom Navigation colors. Accent color for active item,
        Inactive color when its view is disabled.
        Will not be visible if setColored(true) and default current item is set.
         */
        bottomNavigation.setDefaultBackgroundColor(Color.WHITE);
        bottomNavigation.setAccentColor(fetchColor(R.color.maps_tab));
        bottomNavigation.setInactiveColor(fetchColor(R.color.bottom_tab_item_resting));

        // Colors for selected (active) and non-selected items.
        bottomNavigation.setColoredModeColors(Color.WHITE,
                fetchColor(R.color.bottom_tab_item_resting));

        //  Enables Reveal effect
        bottomNavigation.setColored(true);

        //  Displays item Title always (for selected and non-selected items)
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);
    }


    /**
     * Adds (items) {@link AHBottomNavigationItem} to {@link AHBottomNavigation}
     * Also assigns a distinct color to each Bottom Navigation item, used for the color ripple.
     */
    private void addBottomNavigationItems() {
        Log.v(TAG, "addBottomNavigationItems called");
        AHBottomNavigationItem item1 = new AHBottomNavigationItem(R.string.maps_tab, R.drawable.ic_search_tab, colors[0]);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem(R.string.words_tab, R.drawable.ic_words_tab, colors[1]);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem(R.string.guess_tab, R.drawable.ic_guess_tab, colors[2]);

        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);
    }


    /**
     * Simple facade to fetch color resource, so I avoid writing a huge line every time.
     *
     * @param color to fetch
     * @return int color value.
     */
    private int fetchColor(@ColorRes int color) {
        return ContextCompat.getColor(this, color);
    }

    private int requiredWordsForLine() {
        String difficulty = sharedPreference.getCurrentDifficultyLevel();
        switch (difficulty) {
            case "Insane":
                return fetchInteger(R.integer.hint_line_insane);
            case "Hard":
                return fetchInteger(R.integer.hint_line_hard);
            case "Moderate":
                return fetchInteger(R.integer.hint_line_moderate);
            case "Easy":
                return fetchInteger(R.integer.hint_line_easy);
            case "Very Easy":
                return fetchInteger(R.integer.hint_line_very_easy);
            default:
                Log.e(TAG, "Should not be in default case. Difficulty: " + difficulty);
                return 0;
        }
    }

    private int fetchInteger(int id) {
        return getResources().getInteger(id);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPause() {
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        super.onPause();
    }

    @Override
    public void onResume() {
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int steps = 0;
            float[] values = event.values;
            if (values.length > 0){
                steps = (int) values[0];
                Log.e(TAG, "steps = " + steps);
            }
            //Update the prevSteps count if the app has just been created.
            if (waitingFirstStep){
                Log.e(TAG, "Just made activity");
                //Subtract 1 for the step before the one that just happened
                prevSteps = steps-1;
                waitingFirstStep = false;
            }
            Log.e(TAG, "Current steps: " + steps + " Previous steps: " + prevSteps);
            int stepsAdded = steps - prevSteps;
            float addedDistance = (stepsAdded*stepSize)/100;
            //Update the previous steps counter.
            prevSteps = steps;
            //Add to the distance for this specific song
            songInfo.addDistance(addedDistance);
            sharedPreference.saveSongInfo(songNumber, songInfo);
            //Add to the total distance walked.
            sharedPreference.addDistanceWalked(addedDistance);
            checkWalkingAchievements(sharedPreference.getTotalDistance());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void checkWalkingAchievements(float totalDistance){
        if (updateWalkingAchievement(achWalk500, totalDistance)) achWalk500 = null;
        if (updateWalkingAchievement(achWalk5k, totalDistance)) achWalk5k = null;
        if (updateWalkingAchievement(achWalk10k, totalDistance)) achWalk10k = null;
    }

    private void showAchievement(String title){
        Toast.makeText(getApplicationContext(), "Achievement unlocked: " + title, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows the achievement if it is achieved, saves it regardless of progress
     * @param achievement - achievement to update.
     * @return true if the achievement is achieved, false otherwise;
     */
    private boolean updateWalkingAchievement(Achievement achievement, float totalDistance){
        if (achievement != null){
            achievement.setSteps((int) totalDistance);
            if (achievement.isAchieved()){
                achievementComplete.start();
                showAchievement(achievement.getTitle());
                sharedPreference.saveAchievement(achievement);
                return true;
            } else{
                Log.v(TAG, "New steps: " + achievement.getSteps());
                sharedPreference.saveAchievement(achievement);
            }
        }
        return false;
    }

}