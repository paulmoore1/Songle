package com.example.songle;
//adapted from https://github.com/Suleiman19/Bottom-Navigation-Demo/blob/master/app/src/main/java/com/grafixartist/bottomnav/MainActivity.java


import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.notification.AHNotification;
import com.google.android.gms.maps.MapFragment;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainGameActivity extends AppCompatActivity {
    private static final String TAG = "MainGameActivity";
    private final int[] colors = {R.color.maps_tab, R.color.words_tab, R.color.guess_tab};
    private boolean permission;
    private Toolbar toolbar;
    private NoSwipePager viewPager;
    private AHBottomNavigation bottomNavigation;
    private BottomBarAdapter pagerAdapter;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    private boolean notificationVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_AUTO);
        //if location permissions not available request them.
        if(!checkPermission()){
            requestPermission();
        }
        super.onCreate(savedInstanceState);
        Log.d(TAG, "super onCreate called");
        setContentView(R.layout.main_game_screen);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle("Let's play!");


        setupViewPager();


//        final DummyFragment fragment = new DummyFragment();
//        Bundle bundle = new Bundle();
//        bundle.putInt("color", ContextCompat.getColor(this, colors[0]));
//        fragment.setArguments(bundle);

//        getSupportFragmentManager()
//                .beginTransaction()
//                .add(R.id.frame, fragment, DummyFragment.TAG)
//                .commit();

        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);
        setupBottomNavBehaviors();
        setupBottomNavStyle();

        createFakeNotification();

        addBottomNavigationItems();
        bottomNavigation.setCurrentItem(1);


        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
//                fragment.updateColor(ContextCompat.getColor(MainActivity.this, colors[position]));

                if (!wasSelected)
                    viewPager.setCurrentItem(position);
                //allow translucent scrolling for middle position only.
                    if(position == 1) bottomNavigation.setTranslucentNavigationEnabled(true);
                    else bottomNavigation.setTranslucentNavigationEnabled(false);

                // remove notification badge
                int lastItemPos = bottomNavigation.getItemsCount() - 1;
                if (notificationVisible && position == lastItemPos)
                    bottomNavigation.setNotification(new AHNotification(), lastItemPos);
                return true;
            }
        });

    }

    private void setupViewPager() {
        viewPager = (NoSwipePager) findViewById(R.id.viewpager);
        viewPager.setPagingEnabled(false);
        pagerAdapter = new BottomBarAdapter(getSupportFragmentManager());

        pagerAdapter.addFragments(createMapFragment(R.color.maps_tab));
        pagerAdapter.addFragments(createWordsFragment(R.color.words_tab));
        pagerAdapter.addFragments(createGuessFragment(R.color.guess_tab));

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

    private void createFakeNotification() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AHNotification notification = new AHNotification.Builder()
                        .setText("1")
                        .setBackgroundColor(Color.YELLOW)
                        .setTextColor(Color.BLACK)
                        .build();
                // Adding notification to last item.

                bottomNavigation.setNotification(notification, bottomNavigation.getItemsCount() - 1);

                notificationVisible = true;
            }
        }, 1000);
    }


    public void setupBottomNavBehaviors() {
//       bottomNavigation.setBehaviorTranslationEnabled(false);

        /*
        Before enabling this. Change MainActivity theme to MyTheme.TranslucentNavigation in
        AndroidManifest.
        Warning: Toolbar Clipping might occur. Solve this by wrapping it in a LinearLayout with a top
        View of 24dp (status bar size) height.
         */
        bottomNavigation.setTranslucentNavigationEnabled(false);
    }

    /**
     * Adds styling properties to {@link AHBottomNavigation}
     */
    private void setupBottomNavStyle() {
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

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(){
        //should we show an explanation?
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)){
            showLocationDialog();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                if (grantResults.length > 0){
                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted){
                        break;
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)){
                                showLocationDialog();
                            }
                        }
                    }
                }
        }
    }

    private void showLocationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.msg_location_rationale)
                .setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermission();
                    }
                })
                .setNegativeButton(R.string.txt_cancel, null)
                .create()
                .show();
    }

}