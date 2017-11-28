package com.example.songle;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by Paul on 21/11/2017.
 */

public class GuessFragment extends Fragment {
    private static final String TAG = "GuessFragment";
    private SharedPreference sharedPreference;
    private Button guess;
    private Button giveUp;
    private TextView guessMessage;
    private EditText enterGuess;
    private Song correctSong;

    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        correctSong = sharedPreference.getCurrentSong();
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_guess_tab, container, false);
        guess = view.findViewById(R.id.btn_guess);
        giveUp = view.findViewById(R.id.btn_give_up);
        //If there wasn't an incorrect guess before, make giving up invisible
        if (!sharedPreference.getIncorrectGuess()){
            giveUp.setVisibility(View.INVISIBLE);
        }
        guessMessage = view.findViewById(R.id.textViewGuess);
        enterGuess = view.findViewById(R.id.editTextGuess);

        guess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = enterGuess.getText().toString();
                if (checkGuess(str)){
                    winGame();
                } else {
                    incorrectGuess();
                }
            }
        });

        return view;
    }

    private boolean checkGuess(String guess){
        String correctTitle = correctSong.getTitle().toLowerCase();
        guess = guess.toLowerCase();
        return(guess.equals(correctTitle));
    }

    private void incorrectGuess(){
        sharedPreference.saveIncorrectGuess();
        giveUp.setVisibility(View.VISIBLE);
    }

    private void winGame(){

        String songNumber = correctSong.getNumber();
        sharedPreference.completeSong(songNumber);

        showWinDialog();
    }

    // from https://stackoverflow.com/questions/42024058/how-to-open-youtube-video-link-in-android-app
    private void watchYouTubeVideo(){
        String link = correctSong.getLink();
        //extract last 11 characters of link as the id
        String id = link.substring(link.length() - 11);
        Intent applicationIntent = new  Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            startActivity(applicationIntent);
        } catch (ActivityNotFoundException ex){
            startActivity(browserIntent);
        }
    }

    private void startNewGame(){
        Intent intent = new Intent(getActivity().getApplicationContext(), GameSettingsActivity.class);
        intent.putExtra("GAME_TYPE", getString(R.string.txt_new_game));
        startActivity(intent);
    }

    private void showWinDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.congratulations);
        adb.setMessage(R.string.msg_game_win);
        adb.setPositiveButton(R.string.watch_video, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                watchYouTubeVideo();
            }
        });
        adb.setNeutralButton(R.string.txt_new_game, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame();
            }
        });
        adb.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                getActivity().finish();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }


}
