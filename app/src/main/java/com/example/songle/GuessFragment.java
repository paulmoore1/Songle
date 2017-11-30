package com.example.songle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by Paul on 21/11/2017.
 * Floating action menu adapted from http://www.devexchanges.info/2016/07/floating-action-menu-in-android.html
 */

public class GuessFragment extends Fragment {
    private static final String TAG = "GuessFragment";
    private SharedPreference sharedPreference;
    private Button guess;
    private Button giveUp;
    private TextView revealArtistText;
    private EditText enterGuess;
    private Song currentSong;
    private String currentSongNumber;
    private String currentDifficulty;
    private ArrayList<String> mChosenDifficulty;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private ProgressBar lineProgress;
    private ProgressBar artistProgress;
    //Stores the number of words needed to get a hint for a line/artist
    private int lineMax;
    private int artistMax;

    private TextView lineText;
    private TextView artistText;

    private FloatingActionMenu fam;
    private FloatingActionButton fabLine, fabArtist;

    private FragmentActivity mainActivity;


    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        mainActivity = getActivity();
        sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);

        currentSong = sharedPreference.getCurrentSong();
        currentSongNumber = sharedPreference.getCurrentSongNumber();
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                // Only refresh if the song is still being worked on.
                if (sharedPreference.getSongLocation(currentSongNumber) != -1) refreshProgress();
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
        revealArtistText = view.findViewById(R.id.textViewArtist);
        if (sharedPreference.isArtistRevealed()){
            revealArtist();
        }

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
                showCheckGiveUpDialog();
            }
        });

        fabLine = (FloatingActionButton) view.findViewById(R.id.fab_reveal_line);
        fabArtist = (FloatingActionButton) view.findViewById(R.id.fab_reveal_artist);
        fam = (FloatingActionMenu) view.findViewById(R.id.fab_menu);

        //Set colors here (wouldn't work in xml layout file)
        fabLine.setColorNormalResId(R.color.fab_menu_hint);
        fabLine.setColorPressedResId(R.color.fab_menu_hint_pressed);
        fabArtist.setColorNormalResId(R.color.fab_menu_hint);
        fabArtist.setColorPressedResId(R.color.fab_menu_hint_pressed);


        //handling menu status (open or close)
        fam.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {

            }
        });

        fam.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (fam.isOpened()){
                    fam.close(true);
                }
            }
        });

        fabLine.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                revealLine();
                fam.close(true);
            }
        });

        fabArtist.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                revealArtist();
                fam.close(true);
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
        String correctTitle = currentSong.getTitle().toLowerCase();
        guess = guess.toLowerCase();
        return(guess.equals(correctTitle));
    }

    private void incorrectGuess(){
        sharedPreference.saveIncorrectGuess();
        giveUp.setVisibility(View.VISIBLE);
    }

    private void winGame(){

        String songNumber = currentSong.getNumber();
        sharedPreference.completeSong(songNumber);

        showWinDialog();
    }

    // from https://stackoverflow.com/questions/42024058/how-to-open-youtube-video-link-in-android-app
    private void watchYouTubeVideo(){
        String link = currentSong.getLink();
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
                quitAppSafe();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private void showCheckGiveUpDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.txt_are_you_sure);
        adb.setMessage(R.string.msg_check_give_up);
        adb.setPositiveButton(R.string.txt_give_up, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showGiveUpDialog();
            }
        });
        //Only show the option to make it easier if the difficulty is not already very easy
        if (!currentDifficulty.equals(getString(R.string.difficulty_very_easy)))
        adb.setNeutralButton(R.string.txt_change_difficulty, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendSelectEasierDifficultyDialog();
            }
        });
        adb.setNegativeButton(R.string.txt_keep_playing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private void showGiveUpDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.txt_unlucky);
        String messageFormat = getString(R.string.msg_give_up);
        String artist = currentSong.getArtist();
        String title = currentSong.getTitle();
        String message = String.format(messageFormat, title, artist);
        adb.setMessage(message);
        adb.setPositiveButton(R.string.txt_new_game, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame();
            }
        });
        adb.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                quitAppSafe();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    public void sendSelectEasierDifficultyDialog(){
        Log.d(TAG, "sendSelectDifficultyDialog called");
        mChosenDifficulty = new ArrayList<>(2);
        CharSequence[] diffList = getResources().getStringArray(R.array.difficulty_levels);
        // Find how many difficulties should be hidden based on current difficulty
        int numToKeep = calculateDifficultiesToRemove(currentDifficulty);
        final CharSequence[] diffListShort = Arrays.copyOfRange(diffList, 0, numToKeep);
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.txt_select_difficulty);
        adb.setSingleChoiceItems(diffListShort, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String chosenDiff = diffListShort[i].toString();
                        mChosenDifficulty.add(0, chosenDiff);

                    }
                }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String chosenDiff = mChosenDifficulty.get(0);
                //save value in private string
                currentDifficulty = chosenDiff;
                //save value in shared preferences
                sharedPreference.saveCurrentDifficultyLevel(chosenDiff);
                refreshProgress();
            }
        });
        AlertDialog ad = adb.create();
        ad.show();

    }

    private int calculateDifficultiesToRemove(String difficulty){
        if(difficulty.equals(getString(R.string.difficulty_insane))) return 4;
        else if (difficulty.equals(getString(R.string.difficulty_hard))) return 3;
        else if (difficulty.equals(getString(R.string.difficulty_moderate))) return 2;
        else if (difficulty.equals(getString(R.string.difficulty_easy))) return 1;
        else return 0;
    }

    /**
     * Quits app by returning to the home, with LOGOUT set to true.
     * This stops the app, and if it reselected from recent screens, it will load from the start.
     */
    private void quitAppSafe(){
        Intent intent = new Intent(getActivity(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("LOGOUT", true);
        startActivity(intent);
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
        String artistMessage = "";

        //Set the messages appropriately depending on how many words are needed.
        if (neededForLine > 1){
            String lineFormat = mainActivity.getString(R.string.txt_words_to_line_plural);
            lineMessage = String.format(lineFormat, neededForLine);
        } else if (neededForLine == 1){
            lineMessage = mainActivity.getString(R.string.txt_words_to_line_singular);
        } else if (neededForLine < 1 ){
            lineMessage = mainActivity.getString(R.string.txt_words_to_line_enough);
        }

        if (neededForArtist > 1){
            String lineFormat = mainActivity.getString(R.string.txt_words_to_artist_plural);
            artistMessage = String.format(lineFormat, neededForArtist);
        } else if (neededForArtist == 1){
            artistMessage = mainActivity.getString(R.string.txt_words_to_artist_singular);
        } else if (neededForArtist < 1 ){
            artistMessage = mainActivity.getString(R.string.txt_words_to_artist_enough);
        }

        lineText.setText(lineMessage);
        artistText.setText(artistMessage);
    }

    public void revealLine(){
        int wordsAvailable = sharedPreference.getNumAvailableWords();
        // If the user has enough words to reveal a line
        if (wordsAvailable >= lineMax){
            String songNumber = currentSong.getNumber();
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
            Log.v(TAG, "Revealed line #" + lineNum);

            int lineLength = Integer.parseInt(sizes.get(Integer.parseInt(lineNum) - 1));
            for (int word = 1; word < lineLength + 1; word++){
                String key = lineNum + ":" + String.valueOf(word);
                ArrayList<String> lyric = lyrics.get(key);
                lyric.set(1, "True");
                lyrics.put(key, lyric);
            }
            //Remove those words
            sharedPreference.removeNumAvailableWords(lineMax);
            sharedPreference.updateLyrics(lyrics, songNumber);
        } else {
            String msg = getString(R.string.toast_not_enough_words);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }


    }

    public void revealArtist(){
        int wordsAvailable = sharedPreference.getNumAvailableWords();
        if (wordsAvailable >= artistMax){
            String artist = currentSong.getArtist();

            String artistFormat = getString(R.string.artist_revealed);
            String artistMessage = String.format(artistFormat, artist);
            Log.v(TAG, "message is: " + artistMessage);
            revealArtistText.setText(artistMessage);
            sharedPreference.removeNumAvailableWords(artistMax);
            sharedPreference.artistRevealed();
        } else {
            String msg = getString(R.string.toast_not_enough_words);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }

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
