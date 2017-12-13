package com.example.songle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;


/**
 * Created by Paul on 12/12/2017.
 * This is the fragment where the user will select new games
 */

public class HomeFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = HomeFragment.class.getSimpleName();
    private static MediaPlayer buttonSound;
    private static MediaPlayer radioButton;
    private Context mContext;
    private SharedPreference sharedPreference;
    //Stops more than one button being clicked at a time if for some reason one takes a while to respond.
    private boolean buttonClicked, locationGranted;
    private FragmentListener mListener;



    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        sharedPreference = new SharedPreference(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater. inflate(R.layout.home_fragment, container, false);
        if (view != null){
            Button startGame = view.findViewById(R.id.btn_new_game);
            if (startGame != null){
                startGame.setOnClickListener(this);
            }
            Button continueGame = view.findViewById(R.id.btn_continue_game);
            if (continueGame != null){
                continueGame.setOnClickListener(this);
            }
            Button loadGame = view.findViewById(R.id.btn_load_game);
            if (loadGame != null){
                loadGame.setOnClickListener(this);
            }
            Button resetGame = view.findViewById(R.id.btn_reset);
            if (resetGame != null) {
                resetGame.setOnClickListener(this);
            }
        }
        Bundle args = getArguments();
        locationGranted = args.getBoolean(getString(R.string.location_permission));
        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        setupSounds();
    }

    @Override
    public void onClick(View v) {
        if(locationGranted){
            if (v.getId() == R.id.btn_new_game){
                buttonSound.start();
                newGame();
            } else if (v.getId() == R.id.btn_continue_game){
                buttonSound.start();
                continueGame();
            } else if (v.getId() == R.id.btn_load_game){
                buttonSound.start();
                loadGame();
            }
        } else {
            notifyPermissionsRequired();
        }
        if (v.getId() == R.id.btn_reset){
            buttonSound.start();
            sendResetGameDialog();
        }
    }

    // Notifies the home activity that the game needs to be downloaded
    private void notifyGameNeedsDownload(){
        if (mListener!= null){
            String msg = getString(R.string.download_required);
            mListener.onFragmentInteraction(msg);
        }
    }

    // Notifies the home activity that location permissions are required.
    private void notifyPermissionsRequired(){
        if (mListener != null){
            String msg = getString(R.string.location_permission);
            mListener.onFragmentInteraction(msg);
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof FragmentListener){
            mListener = (FragmentListener) context;
        } else {
            Log.e(TAG, "Must implement FragmentListener");
        }

    }

    @Override
    public void onDetach(){
        super.onDetach();
        mListener = null;
        releaseSounds();
    }

    // Start a new game.
    private void newGame(){
        // This checks for all the buttons that one was not previously clicked.
        // Prevents multiple clicks from being acted on while a task is being done.
        if (!buttonClicked){
            buttonClicked = true;
            if (sharedPreference.getAllSongs() != null){
                //now ready to start game settings
                Intent intent = new Intent(mContext, GameSettingsActivity.class);
                //send the game type with the intent so the settings activity loads correctly.
                intent.putExtra("GAME_TYPE", getString(R.string.txt_new_game));
                startActivity(intent);
            } else {
                notifyGameNeedsDownload();
            }
            buttonClicked = false;
        }

    }

    // Continue the most recently saved game
    private void continueGame(){
        Log.v(TAG, "Continue Game button clicked");
        if (!buttonClicked){
            buttonClicked = true;
            Song song = sharedPreference.getCurrentSong();
            String diffLevel = sharedPreference.getCurrentDifficultyLevel();
            // Check there is actually a song in the current song list and a difficulty chosen
            if (song != null && diffLevel != null){
                // Check song is incomplete
                if (song.isSongIncomplete()){
                    String songNumber = song.getNumber();
                    // Check the lyrics and map are stored correctly
                    if(!sharedPreference.checkLyricsStored(songNumber) ||
                            !sharedPreference.checkMaps(songNumber)){
                        Log.e(TAG, "Lyrics/maps not stored correctly");
                        sendGameNotFoundDialog();
                        buttonClicked = false;
                        return;
                    }
                    // Lyrics and map stored correctly, can load game.
                    Intent intent = new Intent(mContext, MainGameActivity.class);
                    startActivity(intent);

                } else if (song.isSongComplete()) {
                    // Send alert dialog that the song has already been done.
                    sendGameCompletedAlreadyDialog();

                } else if (song.isSongNotStarted()){
                    sendGameNotFoundDialog();
                    // Error, should have been marked as incomplete if it is in currentSong
                    Log.e(TAG, "Song marked as not started when tried to continue");
                }
            } else {
                // Send alert that no game was found
                sendGameNotFoundDialog();
            }
        }
        // Allow buttons to be clicked again
        buttonClicked = false;
    }

    // Loads an old game
    private void loadGame(){
        Log.d(TAG, "loadGame called");
        if (!buttonClicked){
            buttonClicked = true;
            //Check that there is at least one old game.
            if (sharedPreference.getCurrentSong() != null && sharedPreference.getCurrentDifficultyLevel() != null){
                //now ready to start new activity
                Intent intent = new Intent(mContext, GameSettingsActivity.class);
                //make sure game type is old game for the game settings activity.
                intent.putExtra("GAME_TYPE", getString(R.string.txt_load_old_game));
                startActivity(intent);
            } else {
                sendGameNotFoundDialog();
            }
            buttonClicked = false;
        }

    }

    private void sendGameNotFoundDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
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
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
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

    private void sendResetGameDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.warning);
        final CharSequence[] resetOptions = getResources().getStringArray(R.array.reset_options);
        final ArrayList selectedItems = new ArrayList();

        adb.setMultiChoiceItems(resetOptions, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                radioButton.start();
                if(isChecked){
                    selectedItems.add(which);
                } else if (selectedItems.contains(which)){
                    selectedItems.remove(Integer.valueOf(which));
                }
            }
        }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonSound.start();
                if (selectedItems.contains(0)){
                    sharedPreference.resetAllSongs();
                }
                if (selectedItems.contains(1)){
                    sharedPreference.resetScores();
                }
                if (selectedItems.contains(2)){
                    sharedPreference.resetAchievements();
                }
            }
        }). setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonSound.start();
            }
        });
        AlertDialog ad = adb.create();
        ad.show();

    }

    private void setupSounds(){
        buttonSound = MediaPlayer.create(mContext, R.raw.button_click);
        radioButton = MediaPlayer.create(mContext, R.raw.radio_button);
    }

    private void releaseSounds(){
        buttonSound.release();
        radioButton.release();
    }

}
