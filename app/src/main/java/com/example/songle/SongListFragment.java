package com.example.songle;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Paul Moore on 25-Oct-17.
 *
 * This class displays a list of songs.
 * On item click it starts the download of that song's map and lyrics, and starts a game Activity with
 * that song. It changes that song's status to incomplete.
 */

public class SongListFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final String TAG = "SongListFragment";
    public static final String ARG_ITEM_ID = "song_list";

    Activity activity;
    ListView songListView;
    List<Song> songs;
    SongListAdapter songListAdapter;

    SharedPreference sharedPreference;
    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        activity = getActivity();
        sharedPreference = new SharedPreference(getActivity().getApplicationContext());

        Bundle bundle = this.getArguments();
        String gameType = bundle.getString("GAME_TYPE");
        Log.d(TAG, "Gametype tag found in bundle: " + gameType);
        if (gameType.equals(getString(R.string.txt_new_game))){
            songs = sharedPreference.getAllSongs();
        } else if (gameType.equals(getString(R.string.txt_load_old_game))){
            songs = sharedPreference.getOldSongs();
        } else {
            Log.e(TAG, "Unexpected game type given");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.fragment_song_list, container, false);
        findViewsById(view);

        songListAdapter = new SongListAdapter(activity, songs);
        songListView.setAdapter(songListAdapter);
        songListView.setOnItemClickListener(this);
        return view;
    }

    private void findViewsById(View view){
        songListView = view.findViewById(R.id.list_song);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Song song = (Song) parent.getItemAtPosition(position);
        String songNum = song.getNumber();
        //save these values to shared preferences.
        sharedPreference.saveCurrentSong(song);
        sharedPreference.saveCurrentSongNumber(songNum);

        //close the fragment.
        getFragmentManager().popBackStackImmediate();

    }

    @Override
    public void onResume(){
        getActivity().setTitle(R.string.txt_select_song);
        getActivity().getActionBar().setTitle(R.string.txt_select_song);
        super.onResume();
    }







}
