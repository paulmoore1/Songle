package com.example.songle;

import android.app.Activity;
import android.app.AlertDialog;
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

public class AchievementListFragment extends Fragment implements AdapterView.OnItemClickListener {
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

        View view = inflater.inflate(R.layout.achievements_fragment, container, false);
        findViewsById(view);

        achievementListAdapter = new AchievementListAdapter(activity, achievements);
        achievementListView.setAdapter(achievementListAdapter);
        achievementListView.setOnItemClickListener(this);
        return view;
    }

    private void findViewsById(View view){
        achievementListView = view.findViewById(R.id.list_achievement);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Achievement achievement = (Achievement) parent.getItemAtPosition(position);
        String description = achievement.getDescription();
        sendDescriptionDialog(description);
    }


    public void sendDescriptionDialog(String description){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.title_achievement_info);
        adb.setMessage(description);
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }







}