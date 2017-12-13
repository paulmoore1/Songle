package com.example.songle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

//Based on https://stackoverflow.com/questions/32944798/switch-between-fragments-with-onnavigationitem-selected-in-new-navigation-drawer
//And https://github.com/ChrisRisner/AndroidFragmentNavigationDrawer
public class HomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        DownloadCallback,
        FragmentListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private boolean stopPermissionRequests, checkSongs;
    private int currentViewID;
    private int oldNumSongs;
    private SharedPreference sharedPreference;
    private static MediaPlayer buttonSound;
    private static MediaPlayer achievementComplete;
    private static MediaPlayer radioButton;
    private Achievement achHelp;
    private Vector<AlertDialog> dialogs = new Vector<>();
    private FragmentManager fragmentManager;
    private ArrayList<Integer> selectedGender;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Toolbar toolbar;

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;

    //Listens for if new songs are downloaded and stored. Sends a dialog if they are.
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (sharedPreference.getAllSongs() != null){
                int newNumSongs = sharedPreference.getAllSongs().size();
                if (newNumSongs > oldNumSongs) sendSongsDownloadedDialog();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "super.onCreate() called");
        setContentView(R.layout.home_layout);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Used to quit the app from other fragments (otherwise can result in loading_layout errors)
        if (getIntent().getBooleanExtra("LOGOUT", false)) finish();

        // Only check for songs once when the app is started
        if (getIntent().getBooleanExtra("JUST_STARTED", false)) {
            checkSongs = true;
            //Have to avoid case where screen is rotated (which means JUST_STARTED would still be true)
            getIntent().putExtra("JUST_STARTED", false);
        }

        fragmentManager = getSupportFragmentManager();
        mNetworkFragment  = NetworkFragment.getInstance(fragmentManager,
                getString(R.string.url_songs_xml));

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open,
                R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        buttonSound = MediaPlayer.create(getApplicationContext(),
                R.raw.button_click);
        achievementComplete = MediaPlayer.create(getApplicationContext(), R.raw.happy_jingle);
        radioButton = MediaPlayer.create(getApplicationContext(), R.raw.radio_button);

        sharedPreference = new SharedPreference(this);
        //Only bother listening if there is a chance the songs will be downloaded
        if (checkSongs) sharedPreference.registerOnSharedPreferenceChangedListener(listener);

        //Load achievements if it is the first time the game has been started, or permission wasn't granted.
        if (sharedPreference.isFirstTimeAppUsed() || !checkPermissions()){
            Log.e(TAG, "First time " + sharedPreference.isFirstTimeAppUsed()
                    + " check permissions: " + checkPermissions());
            AsyncLoadAchievementsTask task = new AsyncLoadAchievementsTask(this);
            task.execute();
            sendEnterHeightDialog(0);
            sharedPreference.saveFirstTimeAppUsed();
        }
        achHelp = sharedPreference.getIncompleteAchievement(getString(R.string.ach_read_help_title));

        //For checking if more songs were downloaded
        oldNumSongs = 0;
        if (sharedPreference.getAllSongs() != null){
            oldNumSongs = sharedPreference.getAllSongs().size();
        }
/*
          This is called when the orientation is changed.
          It retains whatever is on the screen.
         */
        if (savedInstanceState != null){
            currentViewID = savedInstanceState.getInt("PREVIOUS_VIEW_ID", R.id.nav_home);
            displayView(currentViewID);
        } else {
            displayView(R.id.nav_home);
        }


    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        outState.putInt("PREVIOUS_VIEW_ID", currentViewID);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        //Sync the toggle stat after onRestoreInstanceState has occured
        mDrawerToggle.syncState();
        if (checkSongs && isNetworkAvailable(this)){
            Log.e(TAG, "Downloading songs");
            mNetworkFragment.startXmlDownload();
        } else {
            //If songs still need to be checked, send a warning
            if (checkSongs)sendNetworkWarningDialog();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume(){
        Log.d(TAG, "onResume called");
        if (!checkPermissions() && !stopPermissionRequests){
            requestPermissions();
        }
        //Only bother listening if there's a chance that the songs may be downloaded.
        if (checkSongs) sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        super.onResume();
    }

    @Override
    protected void onPause(){
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        super.onPause();
    }

    @Override
    protected void onStop(){
        //Close dialogs to prevent window leaks.
        closeDialogs();
        super.onStop();
    }

    @Override
    public void onBackPressed(){
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
        if (currentViewID != R.id.nav_home){// If the current fragment is not the Home fragment
            displayView(R.id.nav_home); // Display the Home fragment
        } else {
            moveTaskToBack(true); //If view is in Home fragment, exit application
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item){
        displayView(item.getItemId());
        return true;
    }

    private void displayView(int viewId){
        Fragment fragment = null;
        String title = getString(R.string.app_name);
        switch(viewId){
            case R.id.nav_home:
                fragment = new HomeFragment();
                break;
            case R.id.nav_achievements:
                fragment = new AchievementsFragment();//new AchievementListFragment();
                title = getString(R.string.txt_achievements);
                break;
            case R.id.nav_scores:
                fragment = new ScoreListFragment();
                title = getString(R.string.txt_scores);
                break;
            case R.id.nav_help:
                if (updateAchievement(achHelp)) achHelp = null;
                fragment = new HelpFragment();
                title = getString(R.string.txt_help);
                break;
            case R.id.nav_credits:
                fragment = new CreditsFragment();
                title = getString(R.string.txt_credits);
                break;
        }
        if (fragment != null){
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
            ft.replace(R.id.fragment_content, fragment);
            ft.commit();
        }
        //Set the toolbar title
        if (toolbar != null){
            getSupportActionBar().setTitle(title);
        }
        currentViewID = viewId;
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onFragmentInteraction(String msg){
        Log.e(TAG, msg);
        if (msg.equals(getString(R.string.download_required))){
            if (!isNetworkAvailable(this)){
                sendNetworkErrorDialog();
            } else {
                mNetworkFragment.startXmlDownload();
            }
        }
    }

    @Override
    public void updateFromDownload(Object result) {
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
    }


    @Override
    public void finishDownloading() {
        Log.d(TAG, "finishDownloading called");
        checkSongs = false;
        Log.w(TAG, "Check songs set to false after finished");
    }


    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    // checks if a network connection is available
    // Code from https://stackoverflow.com/questions/19240627/how-to-check-internet-connection-available-or-not-when-application-start/19240810#19240810
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }


    private boolean checkPermissions(){
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showLocationRationaleDialog();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Log.d(TAG, "Permission was denied");
                // Permission denied.
                // Notify the user that they have denied a core permission
                showLocationDeniedDialog(false);
                stopPermissionRequests = true;
            }
        }
    }

    private void showLocationRationaleDialog(){
        Log.d(TAG, "show location rationale dialog called");
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_location_error);
        adb.setMessage(R.string.msg_location_rationale);
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(HomeActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
                dialog.dismiss();
            }
        });
        AlertDialog ad = adb.create();
        dialogs.add(ad);
        ad.setCancelable(false);
        ad.show();
    }

    private void showLocationDeniedDialog(Boolean override){
        Log.d(TAG, "Show location denied dialog called");
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_location_error);
        adb.setMessage(R.string.msg_location_permission_denied);
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(HomeActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
        });
        AlertDialog ad = adb.create();
        dialogs.add(ad);
        if (!stopPermissionRequests || override)ad.show();

    }


    //use if a network connection is required for the selected option to work
    private void sendNetworkErrorDialog(){
        //with no internet, send an alert that will take user to settings or close the app.
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.network_error);
        adb.setMessage(R.string.msg_data_required);
        adb.setPositiveButton(R.string.txt_internet_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        adb.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog ad = adb.create();
        dialogs.add(ad);
        ad.show();
    }

    //use if a network connection will probably be required (but not for certain)
    private void sendNetworkWarningDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.network_warning);
        adb.setMessage(R.string.msg_data_warning);
        //dismiss dialog if 'Continue' selected
        adb.setPositiveButton(R.string.txt_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //stop app if 'Exit' selected
        adb.setNegativeButton(R.string.txt_exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        AlertDialog ad = adb.create();
        dialogs.add(ad);
        ad.show();
    }

    //Enter the user height.
    private void sendEnterHeightDialog(int numTimesShown){

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.txt_enter_height);

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_calculate_step_size);
        adb.setView(input);
        if (numTimesShown == 0) adb.setMessage(R.string.msg_enter_height);
        else adb.setMessage(R.string.msg_enter_height_try_again);
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonSound.start();
                try {
                    int height = Integer.parseInt(input.getText().toString());
                    //Pick sensible height cutoffs
                    if (height > 40 && height < 272){
                        sendEnterGenderDialog(height);
                    } else {
                        sendEnterHeightDialog(1);
                    }
                } catch(NumberFormatException e){
                    sendEnterHeightDialog(1);
                }
            }
        }).setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dismiss dialog
                //step size will be default, or whatever it was before.
            }
        });

        AlertDialog ad = adb.create();
        dialogs.add(ad);
        ad.show();
    }

    //
    private void sendEnterGenderDialog(final int height){
        final CharSequence[] list = getResources().getStringArray(R.array.select_gender);
        selectedGender = new ArrayList<>();
        //have default value
        selectedGender.add(0, 0);

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_calculate_step_size);
        adb.setSingleChoiceItems(list, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                radioButton.start();
                selectedGender.add(0, which);
            }
        }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sharedPreference.saveStepSize(height, selectedGender.get(0));
            }
        }).setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing but save step size based on estimate of gender.
                sharedPreference.saveStepSize(height, 2);
            }
        });
        AlertDialog ad = adb.create();
        dialogs.add(ad);
        ad.show();
    }

    private void sendSongsDownloadedDialog(){
        Log.e(TAG, "Dialog builder started");
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_new_songs);
        adb.setMessage(R.string.msg_new_songs);
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

            }
        });
        AlertDialog ad = adb.create();
        dialogs.add(ad);
        ad.show();
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        return false;
    }

    private void closeDialogs(){
        for (AlertDialog dialog : dialogs){
            if (dialog.isShowing()) dialog.dismiss();
        }
    }


    /**
     * For setting up the achievements list if they aren't already, and getting the help text set up.
     * Done as an Async Task so that it doesn't hinder user experience even though it goes quite fast
     * If it's the first time they're loading_layout the game, they shouldn't be able to
     * unlock any achievements by then
     */
    private static class AsyncLoadAchievementsTask extends AsyncTask<Void, Void, String>{

        private WeakReference<HomeActivity> activityReference;
        private SharedPreference sharedPreferenceTask;
        AsyncLoadAchievementsTask(HomeActivity context){
            activityReference = new WeakReference<>(context);
            sharedPreferenceTask = new SharedPreference(activityReference.get().getApplicationContext());
        }

        private String getString(int resID){
            return activityReference.get().getString(resID);
        }

        private int fetchInteger(int resID){
            return activityReference.get().getResources().getInteger(resID);
        }

        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "Did first time task");
            Achievement firstWord = new Achievement(getString(R.string.ach_words_1_title),
                    getString(R.string.ach_words_1_msg),
                    fetchInteger(R.integer.ach_words_1_goal),
                    R.drawable.triangle_grey,
                    R.drawable.triangle_colour,
                    false);
            List<Achievement> achievements = new ArrayList<>();

            achievements.add(firstWord);

            Achievement wordWizard = new Achievement(getString(R.string.ach_words_100_title),
                    getString(R.string.ach_words_100_msg),
                    fetchInteger(R.integer.ach_words_100_goal),
                    R.drawable.piano_grey,
                    R.drawable.piano_colour,
                    false);

            achievements.add(wordWizard);

            Achievement theCollector = new Achievement(getString(R.string.ach_words_500_title),
                    getString(R.string.ach_words_500_msg),
                    fetchInteger(R.integer.ach_words_500_goal),
                    R.drawable.xylophone_grey,
                    R.drawable.xylophone_colour,
                    false);
            achievements.add(theCollector);

            Achievement gottaCatch = new Achievement(getString(R.string.ach_words_all_title),
                    getString(R.string.ach_words_all_msg),
                    fetchInteger(R.integer.ach_words_all_goal),
                    R.drawable.electric_guitar_grey,
                    R.drawable.electric_guitar_colour,
                    false);
            achievements.add(gottaCatch);

            Achievement firstOfMany = new Achievement(getString(R.string.ach_song_1_title),
                    getString(R.string.ach_song_1_msg),
                    fetchInteger(R.integer.ach_songs_1_goal),
                    R.drawable.clarinet_grey,
                    R.drawable.clarinet_colour,
                    false);
            achievements.add(firstOfMany);

            Achievement babyTriple = new Achievement(getString(R.string.ach_song_3_title),
                    getString(R.string.ach_song_3_msg),
                    fetchInteger(R.integer.ach_songs_3_goal),
                    R.drawable.drums_grey,
                    R.drawable.drums_colour,
                    false);
            achievements.add(babyTriple);

            Achievement maestro = new Achievement(getString(R.string.ach_song_10_title),
                    getString(R.string.ach_song_10_msg),
                    fetchInteger(R.integer.ach_songs_10_goal),
                    R.drawable.accordion_grey,
                    R.drawable.accordion_colour,
                    false);
            achievements.add(maestro);

            Achievement theSongfather = new Achievement(getString(R.string.ach_song_20_title),
                    getString(R.string.ach_song_20_msg),
                    fetchInteger(R.integer.ach_songs_20_goal),
                    R.drawable.saxophone_grey,
                    R.drawable.saxophone_colour,
                    false);
            achievements.add(theSongfather);

            Achievement procrastinate = new Achievement(getString(R.string.ach_watch_video_title),
                    getString(R.string.ach_watch_video_msg),
                    fetchInteger(R.integer.ach_watch_video_goal),
                    R.drawable.video_player_grey,
                    R.drawable.video_player_colour,
                    false);
            achievements.add(procrastinate);

            Achievement walk500 = new Achievement(getString(R.string.ach_walk_500_title),
                    getString(R.string.ach_walk_500_msg),
                    fetchInteger(R.integer.ach_walk_500_goal),
                    R.drawable.bass_guitar_grey,
                    R.drawable.bass_guitar_colour,
                    false);
            achievements.add(walk500);

            Achievement goingOut = new Achievement(getString(R.string.ach_walk_5k_title),
                    getString(R.string.ach_walk_5k_msg),
                    fetchInteger(R.integer.ach_walk_5k_goal),
                    R.drawable.maracas_grey,
                    R.drawable.maracas_colour,
                    false);
            achievements.add(goingOut);

            Achievement bearGrylls = new Achievement(getString(R.string.ach_walk_10k_title),
                    getString(R.string.ach_walk_10k_msg),
                    fetchInteger(R.integer.ach_walk_10k_goal),
                    R.drawable.trumpet_grey,
                    R.drawable.trumpet_colour,
                    false);
            achievements.add(bearGrylls);

            Achievement forgotLine = new Achievement(getString(R.string.ach_line_help_1_title),
                    getString(R.string.ach_line_help_1_msg),
                    fetchInteger(R.integer.ach_line_help_1_goal),
                    R.drawable.harmonica_grey,
                    R.drawable.harmonica_colour,
                    false);
            achievements.add(forgotLine);

            Achievement lineHelp = new Achievement(getString(R.string.ach_line_help_10_title),
                    getString(R.string.ach_line_help_10_msg),
                    fetchInteger(R.integer.ach_line_help_10_goal),
                    R.drawable.violin_grey,
                    R.drawable.violin_colour,
                    false);
            achievements.add(lineHelp);

            Achievement artistHelp = new Achievement(getString(R.string.ach_artist_help_title),
                    getString(R.string.ach_artist_help_msg),
                    fetchInteger(R.integer.ach_artist_help_goal),
                    R.drawable.microphone_grey,
                    R.drawable.microphone_colour,
                    false);
            achievements.add(artistHelp);

            Achievement fasterBullet = new Achievement(getString(R.string.ach_time_title),
                    getString(R.string.ach_time_msg),
                    fetchInteger(R.integer.ach_time_goal),
                    R.drawable.stopwatch_grey,
                    R.drawable.stopwatch_colour,
                    false);
            achievements.add(fasterBullet);

            Achievement cannaeDaeIt = new Achievement(getString(R.string.ach_give_up_title),
                    getString(R.string.ach_give_up_msg),
                    fetchInteger(R.integer.ach_give_up_goal),
                    R.drawable.ukelele_grey,
                    R.drawable.ukelele_colour,
                    true);
            achievements.add(cannaeDaeIt);

            Achievement readInstructions = new Achievement(getString(R.string.ach_read_help_title),
                    getString(R.string.ach_read_help_msg),
                    fetchInteger(R.integer.ach_read_help_goal),
                    R.drawable.headphones_grey,
                    R.drawable.headphones_colour,
                    true);
            achievements.add(readInstructions);

            Achievement rickrolled = new Achievement(getString(R.string.ach_rickrolled_title),
                    getString(R.string.ach_rickrolled_msg),
                    fetchInteger(R.integer.ach_rickrolled_goal),
                    R.drawable.jukebox_grey,
                    R.drawable.jukebox_colour,
                    true);
            achievements.add(rickrolled);

            sharedPreferenceTask.saveAchievements(achievements);
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            // get a reference to the activity if it is still there
            HomeActivity activity = activityReference.get();
            if (activity == null) return;
        }
    }

    /**
     * Shows the achievement if it is achieved, saves it regardless of progress
     * @param achievement - achievement to update.
     * @return true if the achievement is achieved, false otherwise;
     */
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
}
