package com.example.songle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends FragmentActivity implements DownloadCallback, View.OnClickListener,
FragmentManager.OnBackStackChangedListener{
    private static final String TAG = "HomeActivity";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private int numSongs = 0;
    private boolean locationGranted;
    private boolean stopPermissionRequests;
    private boolean checkSongs;
    private boolean asyncTaskFinished;
    private SharedPreference sharedPreference;
    private static MediaPlayer buttonSound;
    private static MediaPlayer achievementComplete;
    private Achievement achHelp;
    private AchievementListFragment achievementListFragment;
    private Fragment contentFragment;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private boolean helpVisible;
    private boolean creditsVisible;
    private boolean achievementsVisible;
    private boolean hideMenu;
    private Menu menu;
    //Broadcast receiver that tracks network connectivity changes
    //private NetworkReceiver receiver = new NetworkReceiver();

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;

    private final Object syncObject = new Object();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "super.onCreate() called");
        setContentView(R.layout.home_layout);

        //check for internet access first
        boolean networkOn = isNetworkAvailable(this);
        //if there is internet, load as normal
        if (!networkOn) {
            sendNetworkWarningDialog();
        }
        // Used to quit the app from other fragments (otherwise can result in loading_layout errors)
        if (getIntent().getBooleanExtra("LOGOUT", false)) finish();

        // Used to find if it needs to check for new songs or not.
        if (getIntent().getBooleanExtra("JUST_STARTED", false)) checkSongs = true;

        locationGranted = checkPermissions();
        sharedPreference = new SharedPreference(getApplicationContext());
        setupClickListeners();

        helpVisible = false;
        creditsVisible = false;
        achievementsVisible = false;
        hideMenu = false;

        //Toolbar toolbar = findViewById(R.id.toolbar);
        //toolbar.setOverflowIcon(helpIcon);

        buttonSound = MediaPlayer.create(getApplicationContext(),
                R.raw.button_click);
        achievementComplete = MediaPlayer.create(getApplicationContext(), R.raw.happy_jingle);
        achHelp = sharedPreference.getIncompleteAchievement(getString(R.string.ach_read_help_title));
        /*
        // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
*/
        //Load achievements if it is the first time the game has been started, or permission wasn't granted.
        if (sharedPreference.isFirstTimeAppUsed() || !checkPermissions()){
            asyncTaskFinished = false;
            Log.e(TAG, "First time " + sharedPreference.isFirstTimeAppUsed()
            + " check permissions: " + checkPermissions());
            AsyncFirstTimeTask task = new AsyncFirstTimeTask();
            task.execute();
            sharedPreference.saveFirstTimeAppUsed();
        } else {
            asyncTaskFinished = true;
        }

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        mNetworkFragment  = NetworkFragment.getInstance(getSupportFragmentManager(),
                getString(R.string.url_songs_xml));

    }


    @Override
    public void onClick(View view) {
        //Only let the user continue if they grant permission to the location
        if (locationGranted){
            if (view.getId() == R.id.btn_achievements){
                achievementsVisible = true;
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.image_view_click));
                buttonSound.start();
                if (sharedPreference.getAchievements() != null){
                    setFragmentTitle(R.id.btn_achievements);
                    achievementListFragment = new AchievementListFragment();
                    switchContent(achievementListFragment, AchievementListFragment.ARG_ITEM_ID);
                } else {
                    Toast.makeText(this, "Achievements not loaded yet", Toast.LENGTH_SHORT).show();
                }
            } else if (view.getId() == R.id.btn_new_game){
                Log.e(TAG, "New game button clicked");
                buttonSound.start();
                newGame();
            } else if (view.getId() == R.id.btn_continue_game){
                buttonSound.start();
                continueGame();
            } else if (view.getId() == R.id.btn_load_game){
                buttonSound.start();
                loadGame();
            } else if (view.getId() == R.id.btn_scores){
                view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.image_view_click));
                buttonSound.start();
            } else if (view.getId() == R.id.text_view_help){
                if (helpVisible){
                    animateHelpDown();
                }
            } else if (view.getId() == R.id.text_view_credit){
                if (creditsVisible){
                    Log.e(TAG, "visible");
                    animateCreditsDown();
                } else {
                    Log.e(TAG, "not visible");
                }
            }
        } else {
            showLocationDeniedDialog(true);
        }

    }

    private void setupClickListeners(){
        findViewById(R.id.btn_new_game).setOnClickListener(this);
        findViewById(R.id.btn_continue_game).setOnClickListener(this);
        findViewById(R.id.btn_load_game).setOnClickListener(this);
        findViewById(R.id.btn_achievements).setOnClickListener(this);
        findViewById(R.id.btn_scores).setOnClickListener(this);
        findViewById(R.id.text_view_help).setOnClickListener(this);
    }

    private void setFragmentTitle(int resourceID){
        setTitle(resourceID);
    }

    private void switchContent(Fragment fragment, String tag){
        while (fragmentManager.popBackStackImmediate());
        if (fragment != null){
            getActionBar().setIcon(R.color.transparent);
            getActionBar().setDisplayShowHomeEnabled(false);
            getActionBar().setTitle(R.string.txt_achievements);
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content_frame_home, fragment, tag);
            transaction.addToBackStack(tag);
            transaction.commit();
            contentFragment = fragment;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if (contentFragment instanceof AchievementListFragment){
            outState.putString("content", AchievementListFragment.ARG_ITEM_ID);
        }
        super.onSaveInstanceState(outState);
    }




    @Override
    protected void onPause(){
        Log.d(TAG, "onPause called");
        super.onPause();
        //this.unregisterReceiver(receiver);
    }


    @Override
    protected void onResume(){
        Log.d(TAG, "onResume called");
        setContentView(R.layout.home_layout);
        setupClickListeners();
 /*       // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);*/
        if (!checkPermissions() && !stopPermissionRequests){
            requestPermissions();
        }
        super.onResume();
    }


    @Override
    public void onBackPressed(){
        if (helpVisible){
            animateHelpDown();
            return;
        } else if (creditsVisible){
            animateCreditsDown();
            return;
        }

        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 0){
            super.onBackPressed();
        } else {
            fragmentManager.popBackStack();
            //Reset the title if it
            if (count == 1){
                achievementsVisible = false;
                getActionBar().setTitle(R.string.app_name);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (!hideMenu){
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else return false;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.action_help:
                if (!achievementsVisible){
                    try{
                        Log.e(TAG, achHelp.toString());
                    } catch(NullPointerException e){
                        Log.e(TAG, "null");
                    }
                    if (updateAchievement(achHelp)) achHelp = null;
                    showHelp();
                }

                else makeToast(R.string.txt_close_achievements);
                return true;
            case R.id.action_credits:
                if (!achievementsVisible)
                showCredits();
                else makeToast(R.string.txt_close_achievements);
                return true;
            case android.R.id.home:
                if (helpVisible){
                    animateHelpDown();
                    return true;
                }
                if (creditsVisible){
                    animateCreditsDown();
                    return true;
                }
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if (count == 0) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                } else {
                    Log.e(TAG, "Pressed back");
                    achievementsVisible = false;
                    return onSupportNavigateUp();
                }

        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelp(){
        if (!helpVisible){
            if (creditsVisible){
                animateCreditsDown();
            }
            getActionBar().setTitle(R.string.txt_help);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setDisplayShowHomeEnabled(true);
            animateHelpUp();

        } else {
            animateHelpDown();

        }
    }

    private void animateHelpUp(){
        Animation bottomUp = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.hidden_help_panel);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);
        helpVisible = true;
    }

    private void animateHelpDown(){
        Animation bottomDown = AnimationUtils.loadAnimation(this, R.anim.bottom_down);
        ViewGroup hiddenPanel = (ViewGroup)findViewById(R.id.hidden_help_panel);
        hiddenPanel.startAnimation(bottomDown);
        hiddenPanel.setVisibility(View.GONE);
        helpVisible = false;
        getActionBar().setTitle(R.string.app_name);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);
    }

    private void showCredits(){
        if (!creditsVisible){
            if (helpVisible){
                animateHelpDown();
            }
            animateCreditsUp();
            getActionBar().setTitle(R.string.txt_credits);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setDisplayShowHomeEnabled(true);
        } else {
            animateCreditsDown();
        }
    }

    private void animateCreditsUp(){
        Animation bottomUp = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
        ViewGroup creditsPanel = (ViewGroup)findViewById(R.id.hidden_credit_panel);
        creditsPanel.startAnimation(bottomUp);
        creditsPanel.setVisibility(View.VISIBLE);
        creditsVisible = true;
    }

    private void animateCreditsDown(){
        Animation bottomDown = AnimationUtils.loadAnimation(this, R.anim.bottom_down);
        ViewGroup creditsPanel = (ViewGroup)findViewById(R.id.hidden_credit_panel);
        creditsPanel.startAnimation(bottomDown);
        creditsPanel.setVisibility(View.GONE);
        creditsVisible = false;
        getActionBar().setTitle(R.string.app_name);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);
    }




    //called when the New Game button is clicked

    private void newGame(){
        //check the network is available.
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        //For checking if more songs were downloaded
        int oldNumSongs = 0;
        if (sharedPreference.getAllSongs() != null){
            oldNumSongs = sharedPreference.getAllSongs().size();
        }
        //Download songs if they need to be checked
        //Will not parse if the timestamp matches
        if (checkSongs){
            Log.e(TAG, "Downloading songs");
            startDownloadingSongs();
            synchronized (syncObject){
                try{
                    syncObject.wait();
                } catch (InterruptedException e){
                    Log.e(TAG, "Interrupted while downloading");
                    Intent intent = new Intent(this, HomeActivity.class);
                    startActivity(intent);
                }
            }
            checkSongs = false;
        }

        if (sharedPreference.getAllSongs() != null){
            //now ready to start game settings
            Intent intent = new Intent(this, GameSettingsActivity.class);
            int newNumSongs = sharedPreference.getAllSongs().size();
            if (newNumSongs > oldNumSongs){
                intent.putExtra("NEW_SONGS", true);
            }
            //send the game type with the intent so the settings activity loads correctly.
            intent.putExtra("GAME_TYPE", getString(R.string.txt_new_game));
            startActivity(intent);
        }
    }

    private void continueGame(){
        Log.v(TAG, "Continue Game button clicked");
        Song song = sharedPreference.getCurrentSong();
        String diffLevel = sharedPreference.getCurrentDifficultyLevel();
        //check there is actually a song in the current song list and a difficulty chosen
        if (song != null && diffLevel != null){
            //song is incomplete as expected, check that necessary files are present
            if (song.isSongIncomplete()){
                String songNumber = song.getNumber();
                //If the lyrics are not stored
                if(!sharedPreference.checkLyricsStored(songNumber)){
                    Log.e(TAG, "Lyrics not stored correctly");
                    sendGameNotFoundDialog();
                    return;
                }
                if (!sharedPreference.checkMaps(songNumber)){
                    Log.e(TAG, "Maps not stored correctly");
                    sendGameNotFoundDialog();
                    return;
                }
                //Lyrics and map stored correctly, can load game.
                Intent intent = new Intent(this, MainGameActivity.class);
                startActivity(intent);

            } else if (song.isSongComplete()) {
                //send alert dialog that the song has already been done.
                sendGameCompletedAlreadyDialog();

            } else if (song.isSongNotStarted()){
                sendGameNotFoundDialog();
                //error, should have been marked as incomplete if it is in currentSong
                Log.e(TAG, "Song marked as not started when tried to continue");

            } else {
                //error, should be at least one of these!
                Log.e(TAG, "Unexpected song status tag when tried to load");

            }
        } else {
            //send alert that no game was found
            sendGameNotFoundDialog();

        }
    }

    private void loadGame(){
        Log.d(TAG, "loadGame called");
        //check the network is available - we will probably need to download the maps.
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        Log.v(TAG,"Network connection found");
        //Check that there is at least one old game.
        if (sharedPreference.getCurrentSong() != null && sharedPreference.getCurrentDifficultyLevel() != null){
            //now ready to start new activity
            Intent intent = new Intent(this, GameSettingsActivity.class);
            //make sure game type is old game for the game settings activity.
            intent.putExtra("GAME_TYPE", getString(R.string.txt_load_old_game));
            startActivity(intent);
        } else {
            sendGameNotFoundDialog();
        }
    }


    private void startDownloadingSongs(){
        mNetworkFragment.startXmlDownload();
    }

    @Override
    public void updateFromDownload(Object result) {

    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                break;
            case Progress.CONNECT_SUCCESS:
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                //could add something like this
                //mDataText.setText("" + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
        }
    }


    @Override
    public void finishDownloading() {
        Log.d(TAG, "finishDownloading called");
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
        synchronized (syncObject){
            syncObject.notify();
        }
    }

    // checks if a network connection is available
    // Code from https://stackoverflow.com/questions/19240627/how-to-check-internet-connection-available-or-not-when-application-start/19240810#19240810
    private boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
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
                locationGranted = true;
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
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        adb.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                AlertDialog alertDialog = adb.create();
        alertDialog.show();
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
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private void sendGameNotFoundDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.loading_error);
        adb.setMessage(R.string.msg_game_not_found);
        adb.setNegativeButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private void sendGameCompletedAlreadyDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.loading_error);
        adb.setMessage(R.string.msg_game_already_completed);
        adb.setNegativeButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    @Override
    public void onBackStackChanged(){
        shouldDisplayHomeUp();
    }

    private void shouldDisplayHomeUp(){
        //Enable Up button only if there are entries in the back stack
        boolean fragmentPresent = getSupportFragmentManager().getBackStackEntryCount() > 0;
        getActionBar().setDisplayHomeAsUpEnabled(fragmentPresent);
    }

    private boolean onSupportNavigateUp(){
        fragmentManager.popBackStack();
        achievementsVisible = false;
        getActionBar().setTitle(R.string.app_name);
        getActionBar().setDisplayShowHomeEnabled(true);
        return true;
    }

    private static String readFromAssets(Context context, String filename) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(filename)));
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine!=null){
            sb.append(mLine);
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }

    /**
     * For setting up the achievements list if they aren't already, and getting the help text set up.
     * Done as an Async Task so that it doesn't hinder user experience
     * If it's the first time they're loading_layout the game, they shouldn't be able to
     * unlock any achievements by then
     */
    private class AsyncFirstTimeTask extends AsyncTask<Void, Void, String>{

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

            Achievement procrastinate = new Achievement(getString(R.string.ach_watch_video_title),
                    getString(R.string.ach_watch_video_msg),
                    fetchInteger(R.integer.ach_watch_video_goal),
                    R.drawable.video_player_grey,
                    R.drawable.video_player_colour,
                    false);
            achievements.add(procrastinate);

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
            achHelp = readInstructions;

            Achievement rickrolled = new Achievement(getString(R.string.ach_rickrolled_title),
                    getString(R.string.ach_rickrolled_msg),
                    fetchInteger(R.integer.ach_rickrolled_goal),
                    R.drawable.jukebox_grey,
                    R.drawable.jukebox_colour,
                    true);
            achievements.add(rickrolled);

            sharedPreference.saveAchievements(achievements);
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            Toast.makeText(getApplicationContext(), "Got achievements!", Toast.LENGTH_SHORT).show();
            asyncTaskFinished = true;
        }
    }

    private int fetchInteger(int id){
        return getResources().getInteger(id);
    }

    private void makeToast(int toastTextID){
        Toast.makeText(getApplicationContext(), toastTextID, Toast.LENGTH_SHORT).show();
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
