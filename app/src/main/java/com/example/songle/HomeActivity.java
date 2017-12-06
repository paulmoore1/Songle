package com.example.songle;

import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends FragmentActivity implements DownloadCallback, View.OnClickListener,
FragmentManager.OnBackStackChangedListener{
    private static final String TAG = "HomeActivity";
    private static final int RC_SIGN_IN = 66;
    private SharedPreference sharedPreference;
    private MediaPlayer buttonSound;
    AchievementListFragment achievementListFragment;
    private Fragment contentFragment;
    //Broadcast receiver that tracks network connectivity changes
    //private NetworkReceiver receiver = new NetworkReceiver();

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;


    // boolean telling us whether a download is in progress so we don't trigger overlapping
    // downloads with consecutive button clicks
    private boolean mDownloading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "super.onCreate() called");

        //check for internet access first
        boolean networkOn = isNetworkAvailable(this);
        //if there is internet, load as normal
        if (!networkOn) {
            sendNetworkWarningDialog();
        }

        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!defaultPrefs.getBoolean("firstTime", false)){
            mNetworkFragment  = NetworkFragment.getInstance(getSupportFragmentManager(),
                    getString(R.string.url_songs_xml));
        }


        // Used to quit the app from other fragments (otherwise can result in loading errors)
        if (getIntent().getBooleanExtra("LOGOUT", false))
        {
            finish();
        }
        setContentView(R.layout.home_screen);
        Log.d(TAG, "Layout loaded");

        sharedPreference = new SharedPreference(getApplicationContext());

        findViewById(R.id.btn_new_game).setOnClickListener(this);
        findViewById(R.id.btn_continue_game).setOnClickListener(this);
        findViewById(R.id.btn_load_game).setOnClickListener(this);
        findViewById(R.id.btn_achievements).setOnClickListener(this);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        Drawable helpIcon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_help);
        //toolbar.setOverflowIcon(helpIcon);
        //signInSilently();

        buttonSound = MediaPlayer.create(getApplicationContext(),
                R.raw.button_click);
/*
        // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
*/
        AsyncCreateAchievementsTask task = new AsyncCreateAchievementsTask();
        task.execute();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_achievements){
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
        }
    }

    protected void setFragmentTitle(int resourceID){
        setTitle(resourceID);
        getActionBar().setTitle(resourceID);
    }

    public void switchContent(Fragment fragment, String tag){
        Log.v(TAG, "Switching to Achievements fragment view");
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null){
            setTitle(R.string.txt_achievements);
            //getActionBar().setTitle(R.string.txt_achievements);
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



    //called when the New Game button is clicked

    public void newGame(){
        //check the network is available.
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        //start downloading the achievements xml, which will store the achievements in XML in Shared Prefs
        startXmlDownload();
        setContentView(R.layout.loading);
        //do nothing until downloading is false.
        while (mDownloading){
            //Log.d(TAG, "Waiting for download");
        }

        //now ready to start game settings
        Intent intent = new Intent(this, GameSettingsActivity.class);
        //send the game type with the intent so the settings activity loads correctly.
        intent.putExtra("GAME_TYPE", getString(R.string.txt_new_game));
        startActivity(intent);

    }

    public void continueGame(){
        Log.d(TAG, "Continue Game button clicked");
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

    public void loadGame(){
        Log.d(TAG, "loadGame called");
        //check the network is available - we will probably need to download the maps.
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        Log.v(TAG,"Network connection found");

        //now ready to start new activity
        Intent intent = new Intent(this, GameSettingsActivity.class);
        //make sure game type is old game for the game settings activity.
        intent.putExtra("GAME_TYPE", getString(R.string.txt_load_old_game));
        startActivity(intent);

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
        getActionBar().setTitle(R.string.app_name);
 /*       // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);*/
        super.onResume();

    }
    public void startXmlDownload(){
        mNetworkFragment.startXmlDownload();
        mDownloading = true;
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
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }

    }

    // checks if a network connection is available
    // Code from https://stackoverflow.com/questions/19240627/how-to-check-internet-connection-available-or-not-when-application-start/19240810#19240810
    public boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            showHelp();
            return true;
        }

        if (id == R.id.action_credits){
            showCredits();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showHelp(){
        sendGameNotFoundDialog();
    }

    private void showCredits(){
        sendGameCompletedAlreadyDialog();
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
    public void onBackStackChanged() {

    }

    /**
     * For setting up the achievements list if they aren't already
     * Done as an Async Task so that it doesn't hinder user experience
     * If it's the first time they're loading the game, they shouldn't be able to
     * unlock any achievements by then
     */
    private class AsyncCreateAchievementsTask extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... voids) {
            Achievement firstWord = new Achievement(getString(R.string.achievement_first_word_title),
                    getString(R.string.achievement_first_word_msg),
                    fetchInteger(R.integer.achievement_first_word_goal),
                    R.drawable.triangle_grey,
                    R.drawable.triangle_colour,
                    false);
            List<Achievement> achievements = new ArrayList<>();

            achievements.add(firstWord);

            Achievement wordWizard = new Achievement(getString(R.string.achievement_words_wizard_title),
                    getString(R.string.achievement_words_wizard_msg),
                    fetchInteger(R.integer.achievement_words_wizard_goal),
                    R.drawable.piano_grey,
                    R.drawable.piano_colour,
                     false);

            achievements.add(wordWizard);

            Achievement theCollector = new Achievement(getString(R.string.achievement_the_collector_title),
                    getString(R.string.achievement_the_collector_msg),
                    fetchInteger(R.integer.achievement_the_collector_goal),
                    R.drawable.xylophone_grey,
                    R.drawable.xylophone_colour,
                    false);
            achievements.add(theCollector);

            Achievement gottaCatch = new Achievement(getString(R.string.achievement_gotta_catch_title),
                    getString(R.string.achievement_gotta_catch_msg),
                    fetchInteger(R.integer.achievement_gotta_catch_goal),
                    R.drawable.electric_guitar_grey,
                    R.drawable.electric_guitar_colour,
                    false);
            achievements.add(gottaCatch);

            Achievement firstOfMany = new Achievement(getString(R.string.achievement_first_of_many_title),
                    getString(R.string.achievement_first_of_many_msg),
                    fetchInteger(R.integer.achievement_first_of_many_goal),
                    R.drawable.clarinet_grey,
                    R.drawable.clarinet_colour,
                    false);
            achievements.add(firstOfMany);

            Achievement babyTriple = new Achievement(getString(R.string.achievement_baby_triple_title),
                    getString(R.string.achievement_baby_triple_msg),
                    fetchInteger(R.integer.achievement_baby_triple_goal),
                    R.drawable.drums_grey,
                    R.drawable.drums_colour,
                    false);
            achievements.add(babyTriple);

            Achievement maestro = new Achievement(getString(R.string.achievement_maestro_title),
                    getString(R.string.achievement_maestro_msg),
                    fetchInteger(R.integer.achievement_maestro_goal),
                    R.drawable.accordion_grey,
                    R.drawable.accordion_colour,
                    false);
            achievements.add(maestro);

            Achievement theSongfather = new Achievement(getString(R.string.achievement_the_songfather_title),
                    getString(R.string.achievement_the_songfather_msg),
                    fetchInteger(R.integer.achievement_the_songfather_goal),
                    R.drawable.saxophone_grey,
                    R.drawable.saxophone_colour,
                    false);
            achievements.add(theSongfather);

            Achievement walk500 = new Achievement(getString(R.string.achievement_walk_500_title),
                    getString(R.string.achievement_walk_500_msg),
                    fetchInteger(R.integer.achievement_walk_500_goal),
                    R.drawable.bass_guitar_grey,
                    R.drawable.bass_guitar_colour,
                    false);
            achievements.add(walk500);

            Achievement goingOut = new Achievement(getString(R.string.achievement_going_out_title),
                    getString(R.string.achievement_going_out_msg),
                    fetchInteger(R.integer.achievement_going_out_goal),
                    R.drawable.maracas_grey,
                    R.drawable.maracas_colour,
                    false);
            achievements.add(goingOut);

            Achievement bearGrylls = new Achievement(getString(R.string.achievement_bear_grylls_title),
                    getString(R.string.achievement_bear_grylls_msg),
                    fetchInteger(R.integer.achievement_bear_grylls_goal),
                    R.drawable.trumpet_grey,
                    R.drawable.trumpet_colour,
                    false);
            achievements.add(bearGrylls);

            Achievement forgotLine = new Achievement(getString(R.string.achievement_forgot_line_title),
                    getString(R.string.achievement_forgot_line_msg),
                    fetchInteger(R.integer.achievement_forgot_line_goal),
                    R.drawable.harmonica_grey,
                    R.drawable.harmonica_colour,
                    false);
            achievements.add(forgotLine);

            Achievement lineHelp = new Achievement(getString(R.string.achievement_line_help_title),
                    getString(R.string.achievement_line_help_msg),
                    fetchInteger(R.integer.achievement_line_help_goal),
                    R.drawable.violin_grey,
                    R.drawable.violin_colour,
                    false);
            achievements.add(lineHelp);

            Achievement artistHelp = new Achievement(getString(R.string.achievement_artist_help_title),
                    getString(R.string.achievement_artist_help_msg),
                    fetchInteger(R.integer.achievement_artist_help_goal),
                    R.drawable.microphone_grey,
                    R.drawable.microphone_colour,
                    false);
            achievements.add(artistHelp);

            Achievement procrastinate = new Achievement(getString(R.string.achievement_procrastinate_title),
                    getString(R.string.achievement_procrastinate_msg),
                    fetchInteger(R.integer.achievement_procrastinate_goal),
                    R.drawable.video_player_grey,
                    R.drawable.video_player_colour,
                    false);
            achievements.add(procrastinate);

            Achievement fasterBullet = new Achievement(getString(R.string.achievement_faster_bullet_title),
                    getString(R.string.achievement_faster_bullet_msg),
                    fetchInteger(R.integer.achievement_faster_bullet_goal),
                    R.drawable.stopwatch_grey,
                    R.drawable.stopwatch_colour,
                    false);
            achievements.add(fasterBullet);

            Achievement cannaeDaeIt = new Achievement(getString(R.string.achievement_cannae_dae_it_title),
                    getString(R.string.achievement_cannae_dae_it_msg),
                    fetchInteger(R.integer.achievement_cannae_dae_it_goal),
                    R.drawable.ukelele_grey,
                    R.drawable.ukelele_colour,
                    true);
            achievements.add(cannaeDaeIt);

            Achievement readInstructions = new Achievement(getString(R.string.achievement_read_instructions_title),
                    getString(R.string.achievement_read_instructions_msg),
                    fetchInteger(R.integer.achievement_read_instructions_goal),
                    R.drawable.headphones_grey,
                    R.drawable.headphones_colour,
                    true);
            achievements.add(readInstructions);

            Achievement rickrolled = new Achievement(getString(R.string.achievement_rickrolled_title),
                    getString(R.string.achievement_rickrolled_msg),
                    fetchInteger(R.integer.achievement_rickrolled_goal),
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
        }
    }

    private int fetchInteger(int id){
        return getResources().getInteger(id);
    }
}
