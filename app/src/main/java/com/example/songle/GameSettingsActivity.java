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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class GameSettingsActivity extends FragmentActivity implements DownloadCallback,
        FragmentManager.OnBackStackChangedListener,
        View.OnClickListener{
    private static final String TAG = "GameSettingsActivity";
    //String is NewGame or OldGame, depending on what was selected.
    private String gameType;
    private Fragment contentFragment;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private SharedPreference sharedPreference;
    private TextView txtViewSong, txtViewDiff;
    private NetworkFragment mNetworkFragment;
    private String songNumber;
    private String diffLevel;
    private final Object syncObject = new Object();
    private MediaPlayer buttonClickSound;
    private MediaPlayer radioButtonSound;


    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            String songNumber2 = sharedPreference.getCurrentSongNumber();
            //if achievements are now different (implying the user has selected one)
            if(songNumber2 == null) {
                txtViewSong.setText(R.string.msg_no_song_selected);
            } else {
                //update songNumber in this activity.
                songNumber = songNumber2;
                //update textView object with selected song
                String songText = getString(R.string.msg_song_number_general) + songNumber;
                txtViewSong.setText(songText);

            }

        }
    };
    private ArrayList<String> mChosenDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_game_layout);
        sharedPreference = new SharedPreference(getApplicationContext());
        //check if there was an error when downloading previously so the activity had to be reset
        boolean error = getIntent().getBooleanExtra("DOWNLOAD_ERROR", false);
        if (error){
            sendDownloadErrorDialog();
        }

        txtViewSong = findViewById(R.id.txt_selected_song);
        txtViewDiff = findViewById(R.id.txt_current_difficulty);


        //get the game type to pass on to the songList fragment, so it loads the correct games.
        gameType = getIntent().getStringExtra("GAME_TYPE");
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        mNetworkFragment = NetworkFragment.getInstance(fragmentManager,
                getString(R.string.url_general));

        //shows the last chosen song (if there is one) - avoids bug where clicking an old song
        //doesn't update the text (since the shared preferences don't change).
        songNumber = sharedPreference.getCurrentSongNumber();
        if (songNumber != null){
            String songText = "Previously chose: Song #" + songNumber;
            txtViewSong.setText(songText);
        }

        /*
          This is called when the orientation is changed.
          If the song fragment is on the screen, it is retained.
         */
        if (savedInstanceState != null){
            if (savedInstanceState.containsKey("content")){
                String content = savedInstanceState.getString("content");
                if (content != null && content.equals(SongListFragment.ARG_ITEM_ID)){
                    if (fragmentManager.findFragmentByTag(SongListFragment.ARG_ITEM_ID) != null){
                        setFragmentTitle(R.string.txt_select_song);
                        contentFragment = fragmentManager.findFragmentByTag(SongListFragment.ARG_ITEM_ID);
                    }
                }
            }
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);


    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_select_song){
            //buttonClickSound.start();
            //show the song selection fragment
            setFragmentTitle(R.id.btn_select_song);
            SongListFragment songListFragment = new SongListFragment();
            Bundle bundle = new Bundle();
            bundle.putString("GAME_TYPE", gameType);
            Log.d(TAG, "Select song button clicked");
            songListFragment.setArguments(bundle);
            switchContent(songListFragment, SongListFragment.ARG_ITEM_ID);
        } else if (view.getId() == R.id.btn_select_difficulty){
            //buttonClickSound.start();
            sendSelectDifficultyDialog();
        } else if (view.getId() == R.id.btn_start_game){
            //buttonClickSound.start();
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
    }


    @Override
    protected void onResume(){
        super.onResume();
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        setupSounds();

    }

    @Override
    protected void onPause(){
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        releaseSounds();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if (contentFragment instanceof SongListFragment){
            outState.putString("content", SongListFragment.ARG_ITEM_ID);
        }
        super.onSaveInstanceState(outState);
    }

    // Starts the game by downloading needed files.
    private void startGame(){
        Song currentSong = sharedPreference.getCurrentSong();
        String number = currentSong.getNumber();
        // Save this song status to shared preferences.
        sharedPreference.saveSongStatus(number, "I");

        // Create new info if a new game has been chosen and it wasn't started before.
        if (gameType.equals(getString(R.string.txt_new_game)) && currentSong.isSongNotStarted()){
            String title = currentSong.getTitle();
            String artist = currentSong.getArtist();
            String link = currentSong.getLink();
            SongInfo newInfo = new SongInfo(title, artist, link);
            sharedPreference.saveSongInfo(number, newInfo);
        } else if ((gameType.equals(getString(R.string.txt_new_game)) && currentSong.isSongIncomplete()) ||
                currentSong.isSongComplete()) {// Reset the song if it's a new game and the song was incomplete, or if the song was completed before.
            sharedPreference.resetSong(number, true);
        }

        // Check lyrics are stored - if not then download them
        if (!sharedPreference.checkLyricsStored(songNumber)){
            Log.d(TAG, "Started downloading lyrics");
            mNetworkFragment.startLyricsDownload();
            //do nothing further until download is finished
            synchronized (syncObject){
                try {
                    //Wait up to 15 seconds, will trigger reset otherwise
                    syncObject.wait(15000);
                } catch (InterruptedException e){
                    Log.e(TAG, "Interrupted: " + e);
                    // Reset the activity and notify the user that there was an error.
                    resetSettingsActivity.run();
                }
            }
            if (!sharedPreference.checkLyricsStored(songNumber)) resetSettingsActivity.run();

            Log.d(TAG, "Lyrics downloaded");
        } else {
            Log.d(TAG, "Lyrics already stored");
        }

        // Check maps are stored - if not then download them.
        if (!sharedPreference.checkMaps(songNumber)){
            Log.d(TAG, "Started downloading Maps");
            mNetworkFragment.startKmlDownload();
            // Do nothing further until download is finished
            synchronized (syncObject){
                try {
                    //Wait up to 15 seconds, will trigger reset otherwise
                    syncObject.wait(15000);
                } catch (InterruptedException e){
                    Log.e(TAG, "Interrupted: " + e);
                    // Reset the activity and notify the user that there was an error
                    resetSettingsActivity.run();
                }
            }
            if (!sharedPreference.checkMaps(songNumber)) resetSettingsActivity.run();


            Log.d(TAG, "Maps downloaded");
        } else {
            Log.d(TAG, "Maps already stored");
        }


        // Now ready to start game
        Intent intent = new Intent(GameSettingsActivity.this, MainGameActivity.class);
        startActivity(intent);
    }

    // Resets the settings activity and notifies it that there was an error while downloading.
    private final Runnable resetSettingsActivity = new Runnable() {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if (count == 0) {
                    // Go back to parent activity
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                } else {
                    // Remove the fragment (the songs list)
                    getSupportFragmentManager().popBackStack();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackStackChanged(){

    }

    private void setFragmentTitle(int resourceID){
        setTitle(resourceID);
        getActionBar().setTitle(resourceID);
    }

    private void switchContent(Fragment fragment, String tag){
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
    public void onBackPressed(){
        int count = fragmentManager.getBackStackEntryCount();
        if (count == 0){
            finishDownloading();
            super.onBackPressed();
        } else {
            fragmentManager.popBackStack();
        }
    }


    // checks if a network connection is available
    // Code from https://stackoverflow.com/questions/19240627/how-to-check-internet-connection-available-or-not-when-application-start/19240810#19240810
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
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
        //Notify the main thread that the download is complete
        synchronized (syncObject){
            syncObject.notify();
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
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
                buttonClickSound.start();
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        adb.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClickSound.start();
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
                buttonClickSound.start();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private void sendSelectDifficultyDialog(){
        Log.d(TAG, "sendSelectDifficultyDialog called");
        mChosenDifficulty = new ArrayList<>(2);
        final CharSequence[] diffList = getResources().getStringArray(R.array.difficulty_levels);
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.txt_select_difficulty);
        adb.setSingleChoiceItems(diffList, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        radioButtonSound.start();
                        String chosenDiff = diffList[i].toString();
                        mChosenDifficulty.add(0, chosenDiff);

                    }
                }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClickSound.start();
                if (mChosenDifficulty.size() > 0){
                    String chosenDiff = mChosenDifficulty.get(0);
                    //save value in private string
                    diffLevel = chosenDiff;
                    //save value in shared preferences
                    sharedPreference.saveCurrentDifficultyLevel(chosenDiff);
                    //update text displayed
                    txtViewDiff.setText(chosenDiff);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.msg_diff_error, Toast.LENGTH_SHORT).show();
                }

            }
        });
        AlertDialog ad = adb.create();
        ad.show();
    }

    private void sendConfirmGameDialog(final String chosenSongNumber, final String chosenDiff){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirm_game_settings);
        String beginMessage = getString(R.string.msg_begin_question);
        Song chosenSong = sharedPreference.getCurrentSong();
        // If the song was completed before, or was incomplete but a new game is being started,
        // notify the user that this will reset any progress.
        if (chosenSong.isSongComplete() ||
                (chosenSong.isSongIncomplete() && gameType.equals(getString(R.string.txt_new_game)))){
            beginMessage = "Song number: " + chosenSongNumber + "\nDifficulty level: " +
                    chosenDiff + "\nPrevious progress will be reset\n" + beginMessage;
        } else {
            // Song is incomplete or not started, can just confirm the choice with no warning.
            beginMessage = "Song number: " + chosenSongNumber + "\nDifficulty level: " +
                    chosenDiff + "\n" + beginMessage;
        }
        adb.setMessage(beginMessage);
        // If user is happy with settings, start the game
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClickSound.start();
                // Check for internet connection again
                boolean networkOn = isNetworkAvailable(getApplicationContext());
                if (!networkOn){
                    sendNetworkErrorDialog();
                    return;
                }
                // Going to start the game, so switch to the loading layout
                setContentView(R.layout.loading_layout);
                startGame();
            }
        }).setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClickSound.start();
            }
        });
        AlertDialog ad = adb.create();
        ad.show();
    }

    // Sets up and releases the sounds to avoid memory leaks
    private void setupSounds(){
        buttonClickSound = MediaPlayer.create(this, R.raw.button_click);
        radioButtonSound = MediaPlayer.create(this, R.raw.radio_button);
    }

    private void releaseSounds(){
        buttonClickSound.release();
        radioButtonSound.release();
    }
}
