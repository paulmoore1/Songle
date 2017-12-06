package com.example.songle;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Paul on 21/11/2017.
 */

public class WordsFragment extends Fragment {
    private static final String TAG = "WordsFragment";
    private HashMap<String, ArrayList<String>> lyrics;
    private String songNumber;
    private SharedPreference sharedPreference;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private TextView wordsTextView;

    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        this.sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        sharedPreference.registerOnSharedPreferenceChangedListener(listener);
        songNumber = sharedPreference.getCurrentSongNumber();
        Log.v(TAG, "songNumber " + songNumber);
        lyrics = sharedPreference.getLyrics(songNumber);


        // refresh the lyrics when the shared preferences are updated as lyrics are found
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                refreshLyrics();
            }
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View rootView = inflater.inflate(R.layout.fragment_words, container, false);
        wordsTextView = (TextView) rootView.findViewById(R.id.wordsTextView);
        wordsTextView.setText(lyricsToString());
        return rootView;
    }

    private void refreshLyrics(){
        songNumber = sharedPreference.getCurrentSongNumber();
        lyrics = sharedPreference.getLyrics(songNumber);
        wordsTextView.setText(lyricsToString());
    }



    private String lyricsToString(){
        Log.d(TAG, "lyricsToString called");
        ArrayList<String> sizes = lyrics.get("SIZE");
        int numLines = sizes.size();
        StringBuilder sb = new StringBuilder("\n");
        for (int line = 1; line < numLines + 1; line++) {
            int numWords = Integer.parseInt(sizes.get(line - 1));
            for (int word = 1; word < numWords + 1; word++) {
                String key = String.valueOf(line) + ":" + String.valueOf(word);
                String lyric = showLyric(lyrics.get(key));
                // If the word is the last one on the line put a new line
                if (word == numWords) {
                    lyric = lyric + "\n";
                }
                sb.append(lyric);
            }
        }
        //append newlines as fix to lyrics hidden below status bar
        sb.append("\n\n\n\n\n");
        return sb.toString();
    }

    /**
     * Returns how the lyric should be displayed
     * @param lyricList - item in the lyrics to show
     * @return - the word if it is found, \n if it's a new line, ___ if it's blank
     */
    private String showLyric(ArrayList<String> lyricList){
        //TODO remove - just to check for debugging.
        if (lyricList == null) return " null";
        boolean display = Boolean.valueOf(lyricList.get(1));
        // If lyric is found, return it
        if (display) return " " + lyricList.get(0);
        // If lyric is a blank it must be a new line
        else if(lyricList.get(0).equals("")) return "\n";
        // Otherwise not found, so show blank space.
        else return " ____";// + lyricList.get(0);
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
}
