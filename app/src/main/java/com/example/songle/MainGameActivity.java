package com.example.songle;
//adapted from https://github.com/Suleiman19/Bottom-Navigation-Demo/blob/master/app/src/main/java/com/grafixartist/bottomnav/MainActivity.java


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;

public class MainGameActivity extends AppCompatActivity {
    private static final String TAG = "MainGameActivity";
    private final int[] colors = {R.color.maps_tab, R.color.words_tab, R.color.guess_tab};
    private boolean permission;
    private Toolbar toolbar;
    private NoSwipePager viewPager;
    private AHBottomNavigation bottomNavigation;
    private BottomBarAdapter pagerAdapter;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private SharedPreference sharedPreference;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private String songNumber;
    private SongInfo songInfo;
    private boolean wordsNotificationVisible = false;
    private boolean hintNotificationVisible = false;
    private int lastNumWordsFound = 0;
    private int lastNumWordsAvailable = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_AUTO);

        super.onCreate(savedInstanceState);
        Log.d(TAG, "super onCreate called");
        setContentView(R.layout.main_game_screen);
/*
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle("Let's play!");
*/
        setupViewPager();

        sharedPreference = new SharedPreference(getApplicationContext());
        if (sharedPreference.getHeight() == -1){
            Log.e(TAG, "Height not saved");
            sendRequestHeightDialog(0);
        }

        songNumber = sharedPreference.getCurrentSongNumber();
        songInfo = sharedPreference.getSongInfo(songNumber);
        lastNumWordsFound = songInfo.getNumWordsFound();
        lastNumWordsAvailable = songInfo.getNumWordsAvailable();

        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                //update the song info
                songInfo = sharedPreference.getSongInfo(songNumber);
                int newNumWordsFound = songInfo.getNumWordsFound();
                int newNumWordsAvailable = songInfo.getNumWordsAvailable();
                if (newNumWordsFound > lastNumWordsFound){
                    lastNumWordsFound = newNumWordsFound;
                    createWordsFoundNotification();
                }
                if (newNumWordsAvailable > lastNumWordsAvailable){
                    lastNumWordsAvailable = newNumWordsAvailable;
                }
                int requiredNumForHint = requiredWordsForLine();
                if (lastNumWordsAvailable > requiredNumForHint){
                    createHintNotification();
                }
            }
        };


        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);
        setupBottomNavBehaviors();
        setupBottomNavStyle();

        addBottomNavigationItems();
        bottomNavigation.setCurrentItem(0);


        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
//                fragment.updateColor(ContextCompat.getColor(MainActivity.this, colors[position]));

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
        viewPager = (NoSwipePager) findViewById(R.id.viewpager);
        viewPager.setPagingEnabled(false);
        pagerAdapter = new BottomBarAdapter(getSupportFragmentManager());
        pagerAdapter.addFragments(createMapFragment(R.color.maps_tab));
        pagerAdapter.addFragments(createWordsFragment(R.color.words_tab));
        pagerAdapter.addFragments(createGuessFragment(R.color.guess_tab));
        /*
        pagerAdapter.addFragments(createMapFragment(R.color.maps_tab));
        pagerAdapter.addFragments(createWordsFragment(R.color.words_tab));
        pagerAdapter.addFragments(createGuessFragment(R.color.guess_tab));
*/
        viewPager.setAdapter(pagerAdapter);
    }

    @NonNull
    private MapsFragment createMapFragment(int color) {
        MapsFragment fragment = new MapsFragment();
        fragment.setArguments(passFragmentArguments(fetchColor(color)));
        return fragment;
    }

    @NonNull
    private WordsFragment createWordsFragment(int color) {
        WordsFragment fragment = new WordsFragment();
        fragment.setArguments(passFragmentArguments(fetchColor(color)));
        return fragment;
    }

    @NonNull
    private GuessFragment createGuessFragment(int color) {
        GuessFragment fragment = new GuessFragment();
        fragment.setArguments(passFragmentArguments(fetchColor(color)));
        return fragment;
    }

    @NonNull
    private Bundle passFragmentArguments(int color) {
        Bundle bundle = new Bundle();
        bundle.putInt("color", color);
        return bundle;
    }

    private void createWordsFoundNotification(){
        Log.d(TAG, "createWordsFoundNotification called");
        AHNotification notification = new AHNotification.Builder()
                .setText("!")
                .setBackgroundColor(getColor(R.color.colorBottomNavigationPrimaryDark))
                .setTextColor(Color.WHITE)
                .build();
        bottomNavigation.setNotification(notification, bottomNavigation.getItemsCount() - 2);
        wordsNotificationVisible = true;
    }

    private void createHintNotification(){
        Log.d(TAG, "createHintNotification called");
        AHNotification notification = new AHNotification.Builder()
                .setText("!")
                .setBackgroundColor(getColor(R.color.colorBottomNavigationPrimaryDark))
                .setTextColor(Color.WHITE)
                .build();
        bottomNavigation.setNotification(notification, bottomNavigation.getItemsCount() - 1);
        hintNotificationVisible = true;
    }


    public void setupBottomNavBehaviors() {
        Log.v(TAG, "setupBottomNavBehaviours called");
//       bottomNavigation.setBehaviorTranslationEnabled(false);

        /*
        Warning: Toolbar Clipping might occur. Solve this by wrapping it in a LinearLayout with a top
        View of 24dp (status bar size) height.
         */
        bottomNavigation.setTranslucentNavigationEnabled(true);
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

    private int requiredWordsForLine(){
        String difficulty = sharedPreference.getCurrentDifficultyLevel();
        switch(difficulty){
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

    private int fetchInteger(int id){
        return getResources().getInteger(id);
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPause(){
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        super.onPause();
    }

    @Override
    public void onResume(){
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        super.onResume();
    }

    public void sendRequestHeightDialog(int numTimesShown){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_enter_height);
        if (numTimesShown == 0) adb.setMessage(R.string.msg_enter_height);
        else adb.setMessage(R.string.msg_enter_height_try_again);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        adb.setView(input);

        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int height = Integer.parseInt(input.getText().toString());
                    if (height > 70 && height < 272){
                        sharedPreference.saveHeight(height);
                    } else {
                        sendRequestHeightDialog(1);
                    }

                } catch(NumberFormatException e){
                    sendRequestHeightDialog(1);
                }
            }
        });
        adb.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


    }

}