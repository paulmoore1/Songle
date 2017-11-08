package com.example.songle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends FragmentActivity implements DownloadCallback {
    private static final String TAG = "HomeActivity";
    //Broadcast receiver that tracks network connectivity changes
    //private NetworkReceiver receiver = new NetworkReceiver();

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;


    private SharedPreference sharedPreference = new SharedPreference();

    // boolean telling us whether a download is in progress so we don't trigger overlapping
    // downloads with consecutive button clicks
    private boolean mDownloading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "super.onCreate() called");
        setContentView(R.layout.home_screen);
        Log.d(TAG, "Layout loaded");

        //check for internet access first
        boolean networkOn = isNetworkAvailable(this);
        Log.d(TAG,"Checked network on");
        //if there is internet, load as normal
        if (!networkOn) {
            sendNetworkWarningDialog();
        }

        mNetworkFragment  = NetworkFragment.getInstance(getSupportFragmentManager(),
                getString(R.string.url_songs_xml));

/*
        // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
*/


    }

    //called when the New Game button is clicked

    public void newGame(View view){
        //check the network is available.
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        //start downloading the songs xml, which will store the songs in XML in Shared Prefs
        startXmlDownload();
        setContentView(R.layout.loading);
        //do nothing until downloading is false.
        while (mDownloading){
            //Log.d(TAG, "Waiting for download");
        }

        //now ready to start new activity
        Intent intent = new Intent(this, GameSettingsActivity.class);
        //send the game type with the intent so the settings activity loads correctly.
        intent.putExtra("GAME_TYPE", getString(R.string.txt_new_game));
        startActivity(intent);

    }

    public void continueGame(View view){
        Log.d(TAG, "Continue Game button clicked");
        Song song = sharedPreference.getCurrentSong(this);
        String diffLevel = sharedPreference.getCurrentDifficultyLevel(this);
        //check there is actually a song in the current song list and a difficulty chosen
        if (song != null && diffLevel != null){
            //song is incomplete as expected, check that necessary files are present
            if (song.isSongIncomplete()){

                //check lyrics and map are already downloaded and stored.
                HashMap<String, ArrayList<String>> lyrics = sharedPreference.getLyrics(this);
                List<Placemark> placemarks = sharedPreference.getMap(this, diffLevel);
                if (lyrics != null && placemarks != null){
                    //have lyrics file and song in place, can load MainGameActivity with this
                    //check in that activity for internet before allowing maps
                    Intent intent = new Intent(this, MainGameActivity.class);

                    //convert to JSON format for sending and extracting
                    Gson gson = new Gson();
                    String jsonLyrics = gson.toJson(lyrics);
                    String jsonSong = gson.toJson(song);
                    String jsonPlacemarks = gson.toJson(placemarks);

                    Bundle info = new Bundle();
                    info.putString("JSON_LYRICS", jsonLyrics);
                    info.putString("JSON_SONG", jsonSong);
                    info.putString("JSON_PLACEMARKS", jsonPlacemarks);
                    intent.putExtras(info);
                    startActivity(intent);

                } else {
                    //no lyrics/map found
                    //check if network is available so we can download
                    boolean networkAvailable = isNetworkAvailable(this);
                    //network is active, so download files and start the game.
                    if (networkAvailable){
                        //TODO download and send the files as before above
                    } else {
                        //give up as a network is needed
                        sendNetworkErrorDialog();
                        
                    }


                }

            } else if (song.isSongComplete()) {
                //send alert dialog that the song has already been done.
                sendGameCompletedAlreadyDialog();

            } else if (song.isSongNotStarted()){
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

    public void loadGame(View view){
        Log.d(TAG, "Load Game button pressed");
        //check the network is available - we will probably need to download the maps.
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        Log.d(TAG,"Network connection found");

        //now ready to start new activity
        Intent intent = new Intent(this, GameSettingsActivity.class);
        //make sure game type is old game for the game settings activity.
        intent.putExtra("GAME_TYPE", getString(R.string.txt_load_old_game));
        startActivity(intent);
        Log.d(TAG, "Started Game Settings Activity");

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
        if(result.equals("Updated")){
            finishDownloading();
        } else {
            mNetworkFragment.retryDownload();
        }
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
}
