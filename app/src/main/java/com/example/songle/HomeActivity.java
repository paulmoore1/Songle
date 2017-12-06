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
import java.util.HashMap;
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null){
            setTitle(R.string.txt_select_song);
            getActionBar().setTitle(R.string.txt_select_song);
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content_frame, fragment, tag);
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



    private void signInSilently() {
        Log.d(TAG, "Signing in");
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        signInClient.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            // The signed in account is stored in the task's result.
                            GoogleSignInAccount signedInAccount = task.getResult();
                        } else {
                            startSignInIntent();
                            // Player will need to sign-in explicitly using via UI
                        }
                    }
                });
    }

    private void startSignInIntent() {
        Log.d(TAG, "startSignInIntent called");
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        Intent intent = signInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // The signed in account is stored in the result.
                GoogleSignInAccount signedInAccount = result.getSignInAccount();
            } else {
                String message = result.getStatus().getStatusMessage();
                if (message == null || message.isEmpty()) {
                    message = getString(R.string.signin_other_error);
                }
                new AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
    }

    private void signOut() {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        signInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // at this point, the user is signed out.
                    }
                });
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
                    R.drawable.achievement_first_word_color,
                    R.drawable.achievement_first_word_grey,
                    false);
            List<Achievement> achievements = new ArrayList<>();
            achievements.add(firstWord);
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
