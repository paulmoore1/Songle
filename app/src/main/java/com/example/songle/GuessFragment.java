package com.example.songle;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
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

public class GuessFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "GuessFragment";
    private SharedPreference sharedPreference;
    private Button giveUp;
    private TextView revealArtistText;
    private EditText enterGuess;
    private Song currentSong;
    private String songNumber;
    private String currentDifficulty;
    private ArrayList<String> mChosenDifficulty;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private ProgressBar lineProgress;
    private ProgressBar artistProgress;
    //Stores the number of words needed to get a hint for a line/artist
    private int lineMax;
    private int artistMax;
    private SongInfo songInfo;

    //Possible achievements attainable while in this fragment.
    private Achievement achWatchVideo;
    private Achievement achRickrolled;
    private Achievement achTime;
    private Achievement achRevealArtist;
    private Achievement achSingleLine;
    private Achievement achMultiLine;
    private Achievement achFirstSong;
    private Achievement achTripleSong;
    private Achievement ach10Songs;
    private Achievement ach20Songs;
    private Achievement achGiveup;

    private Context context;

    private static MediaPlayer winGame;
    private static MediaPlayer loseGame;
    private static MediaPlayer showMenu;
    private static MediaPlayer achievementComplete;
    private static MediaPlayer buttonClick;
    private static MediaPlayer radioButtonClick;

    private TextView lineText;
    private TextView artistText;

    private FloatingActionMenu fam;
    private FloatingActionButton fabArtist;

    private FragmentActivity mainActivity;


    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        mainActivity = getActivity();
        context = getActivity().getApplicationContext();
        sharedPreference = new SharedPreference(context);
        currentSong = sharedPreference.getCurrentSong();
        songNumber = sharedPreference.getCurrentSongNumber();
        songInfo = sharedPreference.getSongInfo(songNumber);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                refreshProgress();
            }
        };
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.guess_tab_fragment, container, false);
        giveUp = view.findViewById(R.id.btn_give_up);
        //If there wasn't an incorrect guess before, make giving up invisible
        if (!songInfo.isIncorrectlyGuessed()){
            giveUp.setVisibility(View.INVISIBLE);
        }
        revealArtistText = view.findViewById(R.id.textViewArtist);
        if (songInfo.isArtistRevealed()){
            hintRevealArtist();
        }
        enterGuess = view.findViewById(R.id.editTextGuess);

        createProgressBars(view);

        Button guess = view.findViewById(R.id.btn_guess);
        guess.setOnClickListener(this);
        giveUp = view.findViewById(R.id.btn_give_up);
        giveUp.setOnClickListener(this);

        FloatingActionButton fabLine = view.findViewById(R.id.fab_reveal_line);
        fabArtist = view.findViewById(R.id.fab_reveal_artist);
        fam = view.findViewById(R.id.fab_menu);

        //Set colors here (wouldn't work in xml layout file)
        fabLine.setColorNormalResId(R.color.fab_menu_hint);
        fabLine.setColorPressedResId(R.color.fab_menu_hint_pressed);
        fabArtist.setColorNormalResId(R.color.fab_menu_hint);
        fabArtist.setColorPressedResId(R.color.fab_menu_hint_pressed);


        //handling menu status (open or close)
        fam.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if (opened) showMenu.start();

            }
        });

        fam.setOnClickListener(this);
        fabLine.setOnClickListener(this);
        fabArtist.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_guess){
            buttonClick.start();
            String str = enterGuess.getText().toString();
            if (checkGuess(str)){
                winGame();
            } else {
                sharedPreference.saveSongInfo(songNumber, songInfo);
                //If this is the first time the word has been guessed wrong.
                if (!songInfo.isIncorrectlyGuessed())
                    incorrectGuess();
            }
        } else if (v.getId() == R.id.btn_give_up){
            buttonClick.start();
            showCheckGiveUpDialog();
        } else if (v.getId() == R.id.fab_menu){
            if (fam.isOpened()){
                fam.close(true);
            }
        } else if (v.getId() == R.id.fab_reveal_line){
            buttonClick.start();
            hintRevealLine();
            fam.close(true);
        } else if (v.getId() == R.id.fab_reveal_artist){
            buttonClick.start();
            hintRevealArtist();
            fam.close(true);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        refreshProgress();
        getAchievements();
        setupMediaPlayers();
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
    }

    @Override
    public void onPause(){
        releaseMediaPlayers();
        sharedPreference.unregisterOnSharedPreferenceChangedListener(listener);
        super.onPause();
    }

    private boolean checkGuess(String guess){
        String correctTitle = currentSong.getTitle().toLowerCase();
        guess = guess.toLowerCase();
        return(guess.equals(correctTitle));
    }

    private void incorrectGuess(){
        songInfo.setIncorrectlyGuessed();
        sharedPreference.saveSongInfo(songNumber, songInfo);
        giveUp.setVisibility(View.VISIBLE);
    }

    private void winGame(){
        checkGameWinAchievements();
        String songNumber = currentSong.getNumber();
        winGame.start();
        int points = calculateScorePoints();
        Score score = new Score(points, songInfo.getDistanceWalked(), currentSong.getTitle(),
                songInfo.getTimeTaken());
        sharedPreference.saveScore(score);
        sharedPreference.completeSong(songNumber);
        showWinDialog(points);
    }

    // Based on https://stackoverflow.com/questions/42024058/how-to-open-youtube-video-link-in-android-app
    private void watchYouTubeVideo(){
        if (updateAchievement(achWatchVideo)) achWatchVideo = null;

        String link = songInfo.getLink();

        //If they done got rickrolled, show this.
        if (link.equals("https://youtu.be/dQw4w9WgXcQ")){
            if (updateAchievement(achRickrolled)) achRickrolled = null;
        }
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

    private void showWinDialog(int points){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.congratulations);
        String msgFormat = getString(R.string.msg_game_win);
        String message = String.format(msgFormat, points);
        adb.setMessage(message);
        adb.setPositiveButton(R.string.watch_video, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
                watchYouTubeVideo();
            }
        });
        adb.setNeutralButton(R.string.txt_new_game, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
                startNewGame();
            }
        });
        adb.setNegativeButton(R.string.txt_home, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
                dialog.dismiss();
                Intent intent = new Intent(context, HomeActivity.class);
                startActivity(intent);
            }
        });
        AlertDialog alertDialog = adb.create();
        adb.setCancelable(false);
        alertDialog.show();
    }

    private void showCheckGiveUpDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.txt_are_you_sure);
        if (!currentDifficulty.equals(getString(R.string.difficulty_very_easy))){
            adb.setMessage(R.string.msg_check_give_up);
        } else {
            adb.setMessage(R.string.msg_check_give_up_very_easy);
        }

        adb.setPositiveButton(R.string.txt_give_up, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loseGame.start();
                if (updateAchievement(achGiveup)) achGiveup = null;
                showGiveUpDialog();
            }
        });
        //Only show the option to make it easier if the difficulty is not already very easy
        if (!currentDifficulty.equals(getString(R.string.difficulty_very_easy)))
        adb.setNeutralButton(R.string.txt_change_difficulty, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
                sendSelectEasierDifficultyDialog();
            }
        });
        adb.setNegativeButton(R.string.txt_keep_playing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
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
                buttonClick.start();
                startNewGame();
            }
        });
        adb.setNeutralButton(R.string.txt_home, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
                Intent intent = new Intent(context, HomeActivity.class);
                startActivity(intent);
            }
        });
        adb.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                buttonClick.start();
                dialog.dismiss();
                quitAppSafe();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void sendSelectEasierDifficultyDialog(){
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
                        radioButtonClick.start();
                        String chosenDiff = diffListShort[i].toString();
                        mChosenDifficulty.add(0, chosenDiff);

                    }
                }).setPositiveButton(R.string.txt_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buttonClick.start();
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

    private int calculateScorePoints(){
        float init = 1;
        float percentWords = getPercentWords();
        int difficultyMultiplier = getDifficultyMultiplier();
        int timeMultiplier = getTimeMultiplier();
        int usedLineHint = 3;
        if (songInfo.isLineRevealed()) usedLineHint = 1;
        int usedArtistHint = 6;
        if (songInfo.isArtistRevealed()) usedArtistHint = 1;
        //Calculate the score, then cast it to integer form.
        init = init * percentWords * difficultyMultiplier * timeMultiplier * usedLineHint * usedArtistHint;
        return (int) init;
    }

    private float getPercentWords(){
        String mapNumber = sharedPreference.getCurrentMapNumber();
        int total = sharedPreference.getMapNumWords(mapNumber);
        int found = songInfo.getNumWordsFound();
        if (total > 0){
            return (float) found / (float) total;
        } else {
            Log.e(TAG, "Total was zero, should be larger");
            return 1;
        }
    }

    private int getDifficultyMultiplier(){
        switch(currentDifficulty){
            case "Insane":
                return 16;
            case "Hard":
                return 8;
            case "Moderate":
                return 4;
            case "Easy":
                return 2;
            case "Very Easy":
                return 1;
            default:
                Log.e(TAG, "Unexpected difficulty: " + currentDifficulty);
                return 1;
        }
    }

    private int getTimeMultiplier(){
        //Subtract time taken from max time allowed in minutes
        int timeTaken =  fetchInteger(R.integer.max_time_taken) - songInfo.minutesTaken();
        //Return the time in minutes, or at least 1
        if (timeTaken >= 1) return timeTaken;
        else return 1;
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

    private void getAchievements(){
        //Load achievements
        achWatchVideo = sharedPreference.getIncompleteAchievement(getString(R.string.ach_watch_video_title));
        achRickrolled = sharedPreference.getIncompleteAchievement(getString(R.string.ach_rickrolled_title));
        achTime = sharedPreference.getIncompleteAchievement(getString(R.string.ach_time_title));
        achRevealArtist = sharedPreference.getIncompleteAchievement(getString(R.string.ach_artist_help_title));
        achSingleLine = sharedPreference.getIncompleteAchievement(getString(R.string.ach_line_help_1_title));
        achMultiLine = sharedPreference.getIncompleteAchievement(getString(R.string.ach_line_help_10_title));
        achFirstSong = sharedPreference.getIncompleteAchievement(getString(R.string.ach_song_1_title));
        achTripleSong = sharedPreference.getIncompleteAchievement(getString(R.string.ach_song_3_title));
        ach10Songs = sharedPreference.getIncompleteAchievement(getString(R.string.ach_song_10_title));
        ach20Songs = sharedPreference.getIncompleteAchievement(getString(R.string.ach_song_20_title));
        achGiveup = sharedPreference.getIncompleteAchievement(getString(R.string.ach_give_up_title));
    }

    private void createProgressBars(View view){
        currentDifficulty = sharedPreference.getCurrentDifficultyLevel();
        lineProgress = view.findViewById(R.id.hintLineProgress);
        artistProgress = view.findViewById(R.id.hintArtistProgress);
        lineText = view.findViewById(R.id.hintLinetextView);
        artistText = view.findViewById(R.id.hintArtistTextView);
        int wordsAvailable = songInfo.getNumWordsAvailable();

        setupProgressBars(wordsAvailable, currentDifficulty);
    }

    private int fetchInteger(int id){
        return getContext().getResources().getInteger(id);
    }

    private void refreshProgress(){
        songInfo = sharedPreference.getSongInfo(songNumber);

        //If artist has been revealed previously show it, and hide bars
        if(songInfo.isArtistRevealed()){
            showArtist();
        }

        int wordsAvailable = songInfo.getNumWordsAvailable();
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

    private void hintRevealLine(){
        int wordsAvailable = songInfo.getNumWordsAvailable();
        // If the user has enough words to reveal a line
        if (wordsAvailable >= lineMax){
            HashMap<String, ArrayList<String>> lyrics = sharedPreference.getLyrics(songNumber);
            if (lyrics == null) return;
            ArrayList<String> sizes = lyrics.get("SIZE");

            // Pick a random line by making a list of lines with more than one blank word
            ArrayList<Integer> blanks = getNumBlankWords(sizes, lyrics);


            //Check that there are still lines left to reveal
            if (blanks.size() > 0){
                // Randomize the order
                Collections.shuffle(blanks);
                // Pick a random line
                String lineNum = String.valueOf(blanks.get(0));
                Log.v(TAG, "Revealed line #" + lineNum);

                int lineLength = Integer.parseInt(sizes.get(Integer.parseInt(lineNum) - 1));
                for (int word = 1; word < lineLength + 1; word++){
                    String key = lineNum + ":" + String.valueOf(word);
                    ArrayList<String> lyric = lyrics.get(key);
                    lyric.set(1, "True");
                    lyrics.put(key, lyric);
                }
                //Remove those words
                songInfo.removeNumWordsAvailable(lineMax);
                songInfo.setLineRevealed();
                sharedPreference.saveSongInfo(songNumber, songInfo);
                sharedPreference.saveLyrics(songNumber, lyrics);
                //Show achievements if they're unlocked.
                if (updateAchievement(achSingleLine)) achSingleLine = null;
                if (updateAchievement(achMultiLine)) achMultiLine = null;
            } else {
                String msg = getString(R.string.msg_lines_too_short);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
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
            if (numBlanks > 1)blanks.add(line);
        }
        return blanks;
    }

    private boolean isLyricBlank(ArrayList<String> lyric){
        return !Boolean.valueOf(lyric.get(1));
    }


    private void hintRevealArtist(){
        int wordsAvailable = songInfo.getNumWordsAvailable();
        if (wordsAvailable >= artistMax){
            showArtist();
            if (updateAchievement(achRevealArtist)) achRevealArtist = null;
            songInfo.removeNumWordsAvailable(artistMax);
            songInfo.setArtistRevealed();
            sharedPreference.saveSongInfo(songNumber, songInfo);
        } else {
            String msg = getString(R.string.toast_not_enough_words);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }

    }

    private void showArtist(){
        String artist = songInfo.getArtist();
        String artistFormat = getString(R.string.artist_revealed);
        String artistMessage = String.format(artistFormat, artist);
        Log.v(TAG, "message is: " + artistMessage);
        revealArtistText.setText(artistMessage);
        if (artistProgress != null) artistProgress.setVisibility(View.GONE);
        if (artistText != null) artistText.setVisibility(View.GONE);
        if  (fabArtist != null) fabArtist.setVisibility(View.GONE);
    }

    private void showAchievement(String title){
        Toast.makeText(context, "Achievement unlocked: " + title, Toast.LENGTH_SHORT).show();
    }

    private void checkGameWinAchievements(){
        if (updateAchievement(achFirstSong)) achFirstSong = null;
        if (updateAchievement(achTripleSong)) achTripleSong = null;
        if (updateAchievement(ach10Songs)) ach10Songs = null;
        if (updateAchievement(ach20Songs)) ach20Songs = null;
        //If they took less than 30 mins, mark the time achievement as complete.
        int maxTime = fetchInteger(R.integer.max_time_taken);
        if ((maxTime - songInfo.minutesTaken()) >= maxTime - 30){
            if (updateAchievement(achTime)) achTime = null;
        }
    }

    /**
     * Shows the achievement if it is achieved, saves it regardless of progress
     * @param achievement - achievement to update.
     * @return true if the achievement is achieved, false otherwise;
     */
    private boolean updateAchievement(Achievement achievement){
        if (achievement != null){
            achievement.incrementSteps();
            if (achievement.isAchieved()){
                achievementComplete.start();
                showAchievement(achievement.getTitle());
                sharedPreference.saveAchievement(achievement);
                return true;
            } else{
                sharedPreference.saveAchievement(achievement);
            }
        }
        return false;
    }

    private void releaseMediaPlayers(){
        buttonClick.release();
        radioButtonClick.release();
        winGame.release();
        loseGame.release();
        achievementComplete.release();
        showMenu.release();
    }

    private void setupMediaPlayers(){
        buttonClick = MediaPlayer.create(context, R.raw.button_click);
        radioButtonClick = MediaPlayer.create(context, R.raw.radio_button);
        winGame = MediaPlayer.create(context, R.raw.win_game);
        loseGame = MediaPlayer.create(context, R.raw.lose_game);
        achievementComplete = MediaPlayer.create(context, R.raw.happy_jingle);
        showMenu = MediaPlayer.create(context, R.raw.select_menu);
    }



}
