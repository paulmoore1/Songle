package com.example.songle;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

/**
 * Created by Paul on 06/12/2017.
 */

public class AchievementListFragment extends Fragment{ //implements AdapterView.OnItemClickListener {
    public static final String TAG = AchievementListFragment.class.getSimpleName();
    public static final String ARG_ITEM_ID = "achievement_list";

    Activity activity;
    ListView achievementListView;
    List<Achievement> achievements;
    AchievementListAdapter achievementListAdapter;

    SharedPreference sharedPreference;
    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        activity = getActivity();
        sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        achievements = sharedPreference.getAchievements();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.achievements_layout, container, false);
        findViewsById(view);

        achievementListAdapter = new AchievementListAdapter(activity, achievements);
        achievementListView.setAdapter(achievementListAdapter);
        //achievementListView.setOnItemClickListener(this);
        return view;
    }

    private void findViewsById(View view){
        achievementListView = view.findViewById(R.id.list_achievement);
    }
/*
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
    */

    @Override
    public void onResume(){
        getActivity().setTitle(R.string.txt_achievements);
        getActivity().getActionBar().setTitle(R.string.txt_achievements);
        super.onResume();
    }







}