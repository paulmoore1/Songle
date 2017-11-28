package com.example.songle;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by Paul on 21/11/2017.
 */

public class GuessFragment extends Fragment {
    private static final String TAG = "GuessFragment";
    private SharedPreference sharedPreference;
    private Button guess;
    private Button giveUp;
    private TextView artist;
    private EditText enterGuess;
    private Song correctSong;
    private String currentDifficulty;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private ProgressBar lineProgress;
    private ProgressBar artistProgress;
    //Stores the number of words needed to get a hint for a line/artist
    private int lineMax;
    private int artistMax;

    private TextView lineText;
    private TextView artistText;


    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);

        sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);

        correctSong = sharedPreference.getCurrentSong();
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                refreshProgress();
            }
        };
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
        artist = view.findViewById(R.id.textViewArtist);
        enterGuess = view.findViewById(R.id.editTextGuess);

        createProgressBars(view);


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

        giveUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                sharedPreference.incrementNumberWordsFound();
            }
        });

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
    }

    @Override
    public void onPause(){
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        super.onPause();
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

    private void createProgressBars(View view){
        currentDifficulty = sharedPreference.getCurrentDifficultyLevel();
        lineProgress = view.findViewById(R.id.hintLineProgress);
        artistProgress = view.findViewById(R.id.hintArtistProgress);
        lineText = view.findViewById(R.id.hintLinetextView);
        artistText = view.findViewById(R.id.hintArtistTextView);
        int wordsAvailable = sharedPreference.getNumAvailableWords();

        setupProgressBars(wordsAvailable, currentDifficulty);
    }

    private int fetchInteger(int id){
        return getContext().getResources().getInteger(id);
    }

    private void refreshProgress(){
        int wordsAvailable = sharedPreference.getNumAvailableWords();
        // Check the difficulty hasn't changed
        String checkDifficulty = sharedPreference.getCurrentDifficultyLevel();
        if(currentDifficulty.equals(checkDifficulty)){
            // Just need to update the bars
            updateProgressBars(wordsAvailable);
        } else {
            // Need to setup the bars as if they're new
            setupProgressBars(wordsAvailable, checkDifficulty);
            currentDifficulty = checkDifficulty;
        }
    }

    // Sets up progress bars depending on difficulty.
    // Was cleaner to split them into individual functions.
    private void setupProgressBars(int wordsAvailable, String difficulty){
        if (difficulty.equals(getString(R.string.difficulty_very_easy))){
            setupVeryEasyProgressBars(wordsAvailable);
        } else if (difficulty.equals(getString(R.string.difficulty_easy))){
            setupEasyProgressBars(wordsAvailable);
        } else if (difficulty.equals(getString(R.string.difficulty_moderate))){
            setupModerateProgressBars(wordsAvailable);
        } else if (difficulty.equals(getString(R.string.difficulty_hard))){
            setupHardProgressBars(wordsAvailable);
        } else if (difficulty.equals(getString(R.string.difficulty_insane))){
            setupInsaneProgressBars(wordsAvailable);
        }
    }


    private void setupVeryEasyProgressBars(int wordsAvailable){
        lineMax = fetchInteger(R.integer.hint_line_very_easy);
        artistMax = fetchInteger(R.integer.hint_artist_very_easy);
        lineProgress.setMax(lineMax);
        artistProgress.setMax(artistMax);
        updateProgressBars(wordsAvailable);
    }

    private void setupEasyProgressBars(int wordsAvailable){
        lineMax = fetchInteger(R.integer.hint_line_easy);
        artistMax = fetchInteger(R.integer.hint_artist_easy);
        lineProgress.setMax(lineMax);
        artistProgress.setMax(artistMax);
        updateProgressBars(wordsAvailable);
    }

    private void setupModerateProgressBars(int wordsAvailable){
        lineMax = fetchInteger(R.integer.hint_line_moderate);
        artistMax = fetchInteger(R.integer.hint_artist_moderate);
        lineProgress.setMax(lineMax);
        artistProgress.setMax(artistMax);
        updateProgressBars(wordsAvailable);
    }

    private void setupHardProgressBars(int wordsAvailable){
        lineMax = fetchInteger(R.integer.hint_line_hard);
        artistMax = fetchInteger(R.integer.hint_artist_hard);
        lineProgress.setMax(lineMax);
        artistProgress.setMax(artistMax);
        updateProgressBars(wordsAvailable);
    }

    private void setupInsaneProgressBars(int wordsAvailable){
        lineMax = fetchInteger(R.integer.hint_line_insane);
        artistMax = fetchInteger(R.integer.hint_artist_insane);
        lineProgress.setMax(lineMax);
        artistProgress.setMax(artistMax);
        updateProgressBars(wordsAvailable);
    }


    //Updates the progress on the bars and the text below each
    private void updateProgressBars(int wordsAvailable){
        lineProgress.setProgress(wordsAvailable, true);
        artistProgress.setProgress(wordsAvailable, true);
        setProgressBarTextViews(wordsAvailable);
    }


    private void setProgressBarTextViews(int wordsAvailable){
        int neededForLine = lineMax - wordsAvailable;
        int neededForArtist = artistMax - wordsAvailable;

        String lineMessage = "";
        String artistMessage="";

        //Set the messages appropriately depending on how many words are needed.
        if (neededForLine > 1){
            String lineFormat = getString(R.string.txt_words_to_line_plural);
            lineMessage = String.format(lineFormat, neededForLine);
        } else if (neededForLine == 1){
            String lineFormat = getString(R.string.txt_words_to_line_singular);
            lineMessage = String.format(lineFormat, neededForLine);
        } else if (neededForLine < 1 ){
            lineMessage = getString(R.string.txt_words_to_line_enough);
        }

        if (neededForArtist > 1){
            String lineFormat = getString(R.string.txt_words_to_artist_plural);
            artistMessage = String.format(lineFormat, neededForArtist);
        } else if (neededForArtist == 1){
            String lineFormat = getString(R.string.txt_words_to_artist_singular);
            artistMessage = String.format(lineFormat, neededForArtist);
        } else if (neededForArtist < 1 ){
            artistMessage = getString(R.string.txt_words_to_artist_enough);
        }

        lineText.setText(lineMessage);
        artistText.setText(artistMessage);
    }

    public void revealLine(){
        String songNumber = correctSong.getNumber();
        HashMap<String, ArrayList<String>> lyrics = sharedPreference.getLyrics(songNumber);
        if (lyrics == null) return;
        ArrayList<String> sizes = lyrics.get("SIZE");

        // Pick a random line by making a list of the number of blank words in each line
        // e.g. blanks(2) = 5 means line 3 (add one) has 5 blank words.
        ArrayList<Integer> blanks = getNumBlankWords(sizes, lyrics);
        // Remove blank lines of length one as they aren't useful at all
        blanks = filterLengthOne(blanks);
        // Randomize the order
        Collections.shuffle(blanks);
        // Pick a random line
        String lineNum = String.valueOf(blanks.get(0) + 1);

        int lineLength = Integer.parseInt(sizes.get(Integer.parseInt(lineNum) - 1));
        for (int word = 1; word < lineLength + 1; word++){
            String key = lineNum + ":" + String.valueOf(word);
            ArrayList<String> lyric = lyrics.get(key);
            lyric.set(1, "True");
            lyrics.put(key, lyric);
        }
        sharedPreference.updateLyrics(lyrics, songNumber);
    }

    public void revealArtist(){
        String artist = correctSong.getArtist();
        String artistFormat = getString(R.string.artist_revealed);
        String artistMessage = String.format(artistFormat, artist);
        artistText.setText(artistMessage);
    }

    private ArrayList<Integer> getNumBlankWords(ArrayList<String> sizes,
                                HashMap<String, ArrayList<String>> lyrics){
        int numLines = sizes.size();
        ArrayList<Integer> blanks = new ArrayList<>(numLines);
        for (int line = 1; line < numLines + 1; line++){
            int lineLength = Integer.parseInt(sizes.get(line - 1));
            int numBlanks = 0;
            for (int word = 1; word < lineLength + 1; word++){
                String key = String.valueOf(line) + ":"  + String.valueOf(word);
                ArrayList<String> lyric = lyrics.get(key);
                if (isLyricBlank(lyric)) numBlanks++;
            }
            blanks.add(numBlanks);
        }
        return blanks;
    }

    private boolean isLyricBlank(ArrayList<String> lyric){
        return !Boolean.valueOf(lyric.get(1));
    }

    private ArrayList<Integer> filterLengthOne(ArrayList<Integer> blanks){
        ArrayList<Integer> filtered = new ArrayList<>();
        int n = blanks.size();
        for (int i = 0; i < n; i++){
            if (blanks.get(i) != 1) filtered.add(i);
        }
        return filtered;
    }




}
