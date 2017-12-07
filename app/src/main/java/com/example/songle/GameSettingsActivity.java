package com.example.songle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class GameSettingsActivity extends FragmentActivity implements DownloadCallback,
        FragmentManager.OnBackStackChangedListener{
    public static final String TAG = "GameSettingsActivity";
    //String is NewGame or OldGame, depending on what was selected.
    private String gameType;
    private Fragment contentFragment;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    SongListFragment songListFragment;
    SharedPreference sharedPreference;
    Button buttonSelectSong, buttonSelectDifficulty, buttonStartGame;
    TextView selectedSongNumber, selectedDifficultyLevel;
    private NetworkFragment mNetworkFragmentLyrics;
    private NetworkFragment mNetworkFragmentMaps;
    private boolean mDownloading = false;
    private String songNumber;
    private String diffLevel;
    private final Object syncObject = new Object();
    private MediaPlayer buttonClick;
    private MediaPlayer buttonClick2;


    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            String songNumber2 = sharedPreference.getCurrentSongNumber();
            //if achievements are now different (implying the user has selected one)
            if(songNumber2 == null) {
                selectedSongNumber.setText(R.string.msg_no_song_selected);
            } else {
                //update songNumber in this activity.
                songNumber = songNumber2;
                //update textview object with selected song
                String songText = getString(R.string.msg_song_number_general) + songNumber;
                selectedSongNumber.setText(songText);

            }

        }
    };
    private ArrayList<String> mChosenDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_game_settings);
        sharedPreference = new SharedPreference(getApplicationContext());
        //check if there was an error when downloading previously so the activity had to be reset
        boolean error = getIntent().getBooleanExtra("DOWNLOAD_ERROR", false);
        if (error){
            sendDownloadErrorDialog();
        }

        buttonSelectSong = findViewById(R.id.btn_select_song);
        buttonSelectDifficulty = findViewById(R.id.btn_select_difficulty);
        buttonStartGame = findViewById(R.id.btn_start_game);
        selectedSongNumber = findViewById(R.id.txt_selected_song);
        selectedDifficultyLevel = findViewById(R.id.txt_current_difficulty);

        buttonClick = MediaPlayer.create(this, R.raw.button_click);
        buttonClick2 = MediaPlayer.create(this, R.raw.button_click_2);
        //get the game type to pass on to the songlist fragment, so it loads the correct games.
        gameType = getIntent().getStringExtra("GAME_TYPE");
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        shouldDisplayHomeUp();
        mNetworkFragmentLyrics = NetworkFragment.getInstance(fragmentManager,
                getString(R.string.url_general));
        mNetworkFragmentMaps = NetworkFragment.getInstance(fragmentManager,
                getString(R.string.url_general));

        //shows the last chosen song (if there is one) - avoids bug where clicking an old song
        //doesn't update the text (since the shared preferences don't change).
        songNumber = sharedPreference.getCurrentSongNumber();
        if (songNumber != null){
            String songText = "Previously chose: Song #" + songNumber;
            selectedSongNumber.setText(songText);
        }

        //if select song is selected, load a fragment to display it.
        buttonSelectSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonClick.start();
                //show the song selection fragment
                setFragmentTitle(R.id.btn_select_song);
                songListFragment = new SongListFragment();
                Bundle bundle = new Bundle();
                bundle.putString("GAME_TYPE", gameType);
                Log.d(TAG, "Select song button clicked");
                songListFragment.setArguments(bundle);
                switchContent(songListFragment, SongListFragment.ARG_ITEM_ID);
            }
        });
        //set a dialog if select difficulty is clicked.
        buttonSelectDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonClick.start();
                sendSelectDifficultyDialog();
            }
        });
        //send a dialog if start game is clicked
        buttonStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonClick.start();
                if (songNumber == null){
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.msg_no_song_selected), Toast.LENGTH_SHORT).show();
                } else if (diffLevel == null){
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.msg_no_difficulty_selected), Toast.LENGTH_SHORT).show();
                } else {
                    sendConfirmGameDialog(songNumber, diffLevel);
                }
            }
        });

        /**
         * This is called when the orientation is changed.
         * If the song fragment is on the screen, it is retained.
         */
        if (savedInstanceState != null){
            if (savedInstanceState.containsKey("content")){
                String content = savedInstanceState.getString("content");
                if (content.equals(SongListFragment.ARG_ITEM_ID)){
                    if (fragmentManager.findFragmentByTag(SongListFragment.ARG_ITEM_ID) != null){
                        setFragmentTitle(R.string.txt_select_song);
                        contentFragment = fragmentManager.findFragmentByTag(SongListFragment.ARG_ITEM_ID);
                    }
                }
            }
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);


    }

    protected void setFragmentTitle(int resourceID){
        setTitle(resourceID);
        getActionBar().setTitle(resourceID);
    }

    public void switchContent(Fragment fragment, String tag){
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
    protected void onResume(){
        super.onResume();
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);

    }

    @Override
    protected void onPause(){
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if (contentFragment instanceof SongListFragment){
            outState.putString("content", SongListFragment.ARG_ITEM_ID);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

    }

    public void sendSelectDifficultyDialog(){
        Log.d(TAG, "sendSelectDifficultyDialog called");
        mChosenDifficulty = new ArrayList<>(2);
        final CharSequence[] diffList = getResources().getStringArray(R.array.difficulty_levels);
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_select_difficulty);
        adb.setSingleChoiceItems(diffList, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        buttonClick2.start();
                        String chosenDiff = diffList[i].toString();
                        mChosenDifficulty.add(0, chosenDiff);

                    }
                }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
                String chosenDiff = mChosenDifficulty.get(0);
                //save value in private string
                diffLevel = chosenDiff;
                //save value in shared preferences
                sharedPreference.saveCurrentDifficultyLevel(chosenDiff);
                //update text displayed
                selectedDifficultyLevel.setText(chosenDiff);
            }
        });
        AlertDialog ad = adb.create();
        ad.show();

    }

    public void sendConfirmGameDialog(final String chosenSongNumber, final String chosenDiff){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirm_game_settings);
        String beginQuestion = getString(R.string.msg_begin_question);
        String beginMessage = "Chosen song number: " + chosenSongNumber + "\nChosen difficulty level: " +
                    chosenDiff + "\n" + beginQuestion;
        adb.setMessage(beginMessage);
        //if user is happy with settings, start the game
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
                startGame();

            }
        }).setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
            }
        });
        AlertDialog ad = adb.create();
        ad.show();


    }

    public void startGame(){
        //check for internet connection again
        boolean networkOn = isNetworkAvailable(this);
        if (!networkOn){
            sendNetworkErrorDialog();
            return;
        }
        setContentView(R.layout.loading);
        //save this song status to shared preferences as it has been definitely chosen.
        Song currentSong = sharedPreference.getCurrentSong();

        String number = currentSong.getNumber();
        sharedPreference.saveSongStatus(number, "I");
        //Override old info only if a new game has been chosen.
        if (gameType.equals(getString(R.string.txt_new_game))){
            String title = currentSong.getTitle();
            String artist = currentSong.getArtist();
            String link = currentSong.getLink();
            SongInfo newInfo = new SongInfo(title, artist, link);
            sharedPreference.saveSongInfo(number, newInfo);
        }


        //use this just in case it gets stuck downloading for more than 10s - resets loading screen
        Handler handler = new Handler();
        handler.postDelayed(resetSettingsActivity, 10000);

        //check lyrics are stored - if not then download them
        if (!sharedPreference.checkLyricsStored(songNumber)){
   /*     // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        NetworkReceiver receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
*/
            int i = 0;
            startLyricsDownload();
            //do nothing further until download is finished
            synchronized (syncObject){
                try {
                    syncObject.wait();
                } catch (InterruptedException e){
                    Log.e(TAG, "Interrupted: " + e);
                    //reset the activity and notify the user that there was an error.
                    resetSettingsActivity.run();
                }
            }

            Log.d(TAG, "Lyrics downloaded");
        } else {
            Log.d(TAG, "Lyrics already stored");
        }

        //check maps are stored - if not then download them.
        if (!sharedPreference.checkMaps(songNumber)){
            Log.d(TAG, "Started downloading Kml");
            Log.v(TAG, "mDownloading before startMapsDownload" + mDownloading);
            startMapsDownload();
            Log.v(TAG, "mDownloading after startMapsDownload" + mDownloading);
            //do nothing further until download is finished
            synchronized (syncObject){
                try {
                    syncObject.wait();
                } catch (InterruptedException e){
                    Log.e(TAG, "Interrupted: " + e);
                    //reset the activity and notify the user that there was an error
                    resetSettingsActivity.run();
                }
            }
            Log.d(TAG, "Maps downloaded");
        } else {
            Log.d(TAG, "Maps already stored");
        }
        Log.d(TAG, "Downloaded maps and lyrics, ready to start");
        //now ready to start game
        Intent intent = new Intent(GameSettingsActivity.this, MainGameActivity.class);
        //all data loaded, cancel handler
        handler.removeCallbacks(resetSettingsActivity);

        //finished downloading, unregister.
       // this.unregisterReceiver(receiver);
        startActivity(intent);
    }

    private Runnable resetSettingsActivity = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "Download error - reset activity");
            finishDownloading();
            Intent intent = new Intent(getApplicationContext(), GameSettingsActivity.class);
            intent.putExtra("DOWNLOAD_ERROR", true);
            intent.putExtra("GAME_TYPE", gameType);
            startActivity(intent);
        }
    };

    //TODO fix bug where pressing back button in the Action Bar for the SongListFragment returns to HomeActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if (count == 0) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                } else {
                    Log.e(TAG, "Pressed back");
                    return onSupportNavigateUp();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackStackChanged(){
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only if there are entries in the back stack
        boolean canback = getSupportFragmentManager().getBackStackEntryCount() > 0;
        getActionBar().setDisplayHomeAsUpEnabled(canback);
    }

    public boolean onSupportNavigateUp(){
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    public void onBackPressed(){
        int count = getFragmentManager().getBackStackEntryCount();
        if (count == 0){
            finishDownloading();
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
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

    //use if a network connection is required for the selected option to work
    private void sendNetworkErrorDialog(){
        //with no internet, send an alert that will take user to settings or close the app.
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.network_error);
        adb.setMessage(R.string.msg_data_required);
        adb.setPositiveButton(R.string.txt_internet_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        adb.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private void sendDownloadErrorDialog(){
        //if there was an error, alert the user and ask them to try again.
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.download_error);
        adb.setMessage(R.string.msg_download_error);
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    public void startLyricsDownload(){
        Log.d(TAG, "startLyricsDownload called");
        mNetworkFragmentLyrics.startLyricsDownload();
        mDownloading = true;
    }

    public void startMapsDownload(){
        mNetworkFragmentMaps.startKmlDownload();
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
        if (mNetworkFragmentLyrics != null) {
            mNetworkFragmentLyrics.cancelDownload();
        } else if (mNetworkFragmentMaps != null){
            mNetworkFragmentMaps.cancelDownload();
        }
        //Notify the main thread that the download is complete
        synchronized (syncObject){
            syncObject.notify();
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }





}
