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


/**
 * Created by Paul on 12/12/2017.
 * This is the fragment where the user will select new games
 */

public class HomeFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = HomeFragment.class.getSimpleName();
    private static MediaPlayer buttonSound;
    private Context mContext;
    private SharedPreference sharedPreference;
    //Stops more than one button being clicked at a time if for some reason one takes a while to respond.
    private boolean buttonClicked;
    private OnFragmentInteractionListener mListener;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        sharedPreference = new SharedPreference(mContext);
        buttonSound = MediaPlayer.create(mContext, R.raw.button_click);


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
        }
        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
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
    }

    public void notifyGameNeedsDownload(){
        if (mListener!= null){
            String msg = getString(R.string.download_required);
            mListener.onFragmentInteraction(msg);
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener){
            mListener = (OnFragmentInteractionListener) context;
        } else {
            Log.e(TAG, "Must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mListener = null;
    }

    private void newGame(){
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

    private void continueGame(){
        Log.v(TAG, "Continue Game button clicked");
        if (!buttonClicked){
            buttonClicked = true;
            Song song = sharedPreference.getCurrentSong();
            String diffLevel = sharedPreference.getCurrentDifficultyLevel();
            //check there is actually a song in the current song list and a difficulty chosen
            if (song != null && diffLevel != null){
                //song is incomplete as expected, check that necessary files are present
                if (song.isSongIncomplete()){
                    String songNumber = song.getNumber();
                    //If the lyrics are not stored
                    if(!sharedPreference.checkLyricsStored(songNumber) ||
                            !sharedPreference.checkMaps(songNumber)){
                        Log.e(TAG, "Lyrics/maps not stored correctly");
                        sendGameNotFoundDialog();
                        buttonClicked = false;
                        return;
                    }
                    //Lyrics and map stored correctly, can load game.
                    Intent intent = new Intent(mContext, MainGameActivity.class);
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
        buttonClicked = false;

    }

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

}
