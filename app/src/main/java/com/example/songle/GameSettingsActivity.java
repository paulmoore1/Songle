package com.example.songle;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class GameSettingsActivity extends FragmentActivity {
    public static final String TAG = "GameSettingsActivity";
    //String is NewGame or OldGame, depending on what was selected.
    private String gameType;
    private Fragment contentFragment;
    SongListFragment songListFragment;
    SharedPreference sharedPreference = new SharedPreference();
    Button buttonSelectSong, buttonSelectDifficulty;
    TextView selectedSongNumber, selectedDifficultyLevel;

    private String songNumber;
    private String diffLevel;
    private Song currentSong;

    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            String songNumber2 = sharedPreference.getCurrentSongNumber(getApplicationContext());
            //if songs are now different (implying the user has selected one)
            if(songNumber2 == null) {
                TextView songTextView = (TextView)findViewById(R.id.txt_selected_song);
                songTextView.setText(R.string.msg_no_song_selected);
            } else if (songNumber == null){
                //no previous song number - update.
                songNumber = songNumber2;
                //update textview object with selected song
                String songText = getString(R.string.msg_song_number_general) + songNumber;
                TextView songTextView = (TextView)findViewById(R.id.txt_selected_song);
                songTextView.setText(songText);

            } else if (!songNumber.equals(songNumber2)){
                //update songNumber in this activity.
                songNumber = songNumber2;
                //update textview object with selected song
                String songText = getString(R.string.msg_song_number_general) + songNumber;
                TextView songTextView = (TextView)findViewById(R.id.txt_selected_song);
                songTextView.setText(songText);

            }

        }
    };
    private ArrayList<String> mChosenDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_game_settings);
        songNumber = sharedPreference.getCurrentSongNumber(this);
        diffLevel = sharedPreference.getCurrentDifficultyLevel(this);

        buttonSelectSong = findViewById(R.id.btn_select_song);
        buttonSelectDifficulty = findViewById(R.id.btn_select_difficulty);
        selectedSongNumber = findViewById(R.id.txt_selected_song);
        selectedDifficultyLevel = findViewById(R.id.txt_current_difficulty);
        //get the game type to pass on to the songlist fragment, so it loads the correct games.
        gameType = getIntent().getStringExtra("GAME_TYPE");
        Log.d(TAG, "gameType: " + gameType);

        sharedPreference.registerOnSharedPreferenceChangedListener(getApplicationContext(), listener);

        FragmentManager fragmentManager = getSupportFragmentManager();
        //if select song is selected, load a fragment to display it.

        buttonSelectSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show the song selection fragment
                setFragmentTitle(R.id.btn_select_song);
                songListFragment = new SongListFragment();
                Bundle bundle = new Bundle();
                bundle.putString("GAME_TYPE", gameType);
                Log.d(TAG, "Game tag in bundle:" + bundle.getString("GAME_TYPE"));
                songListFragment.setArguments(bundle);
                switchContent(songListFragment, SongListFragment.ARG_ITEM_ID);
            }
        });


        buttonSelectDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectDifficultyDialog();
            }
        });

        final Button bStartGame = findViewById(R.id.btn_start_game);
        bStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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


    }

    protected void setFragmentTitle(int resourceID){
        setTitle(resourceID);
        getActionBar().setTitle(resourceID);
    }

    public void switchContent(Fragment fragment, String tag){
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null){
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
        sharedPreference.registerOnSharedPreferenceChangedListener(getApplicationContext(), listener);

    }

    @Override
    protected void onPause(){
        sharedPreference.unregisterOnSharedPreferenceChangedListener(getApplicationContext(), listener);
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
                        String chosenDiff = diffList[i].toString();
                        mChosenDifficulty.add(0, chosenDiff);

                    }
                }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String chosenDiff = mChosenDifficulty.get(0);
                //save value in private string
                diffLevel = chosenDiff;
                //save value in shared preferences
                sharedPreference.saveCurrentDifficultyLevel(getApplicationContext(),
                        chosenDiff);
                //update text displayed
                selectedDifficultyLevel.setText(chosenDiff);
            }
        });
        AlertDialog ad = adb.create();
        ad.show();

    }

    public void sendConfirmGameDialog(final String chosenSongNumber, final String chosenDiff){
        String beginQuestion = getString(R.string.msg_begin_question);
        String beginMessage = "Chosen song number: " + chosenSongNumber + "\nChosen difficulty level: " +
                chosenDiff + "\n" + beginQuestion;
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirm_game_settings);
        adb.setMessage(beginMessage);
        //if user is happy with settings, start the game
        adb.setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //save this song to shared preferences as it has been definitely chosen.
                Song currentSong = sharedPreference.getCurrentSong(getApplicationContext());
                sharedPreference.saveSongStatus(getApplicationContext(), currentSong, "I");

                Intent intent = new Intent(GameSettingsActivity.this, MainGameActivity.class);
                //send the chosen number and difficulty
                intent.putExtra("SONG_NUMBER", chosenSongNumber);
                intent.putExtra("SONG_DIFFICULTY", chosenDiff);
                startActivity(intent);

            }
        }).setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog ad = adb.create();
        ad.show();


    }
    public void sendNoSongsFoundDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.loading_error);
        adb.setMessage(R.string.msg_no_songs_found);
        adb.setNegativeButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog ad = adb.create();
        ad.show();
    }







}
