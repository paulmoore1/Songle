package com.example.songle;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

/**
 * Created by Paul on 12/12/2017.
 * Fragment which when combined with the GridViewAdapter displays the user's achievements.
 */

public class AchievementsFragment extends Fragment{
    private static final String TAG = AchievementsFragment.class.getSimpleName();
    private SharedPreference sharedPreference;
    private String[] gridViewString;
    private int[] gridViewImageId;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        List<Achievement> achievements = sharedPreference.getAchievements();
        if (achievements != null){
            int n = achievements.size();
            gridViewString = new String[n];
            gridViewImageId = new int[n];
            for (int i = 0; i < n; i++){
                Achievement a = achievements.get(i);
                gridViewString[i] = a.getTitle();
                Log.e(TAG, "Title: "+ a.getTitle());
                if (a.isAchieved()){
                    gridViewImageId[i] = a.getColorPictureID();
                } else {
                    gridViewImageId[i] = a.getGreyPictureID();
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.achievements_gridview, container, false);
        // Set up adapter and listener
        AchievementsGridViewAdapter adapter = new AchievementsGridViewAdapter(getContext(), gridViewString, gridViewImageId);
        GridView androidGridView = view.findViewById(R.id.grid_view_image_text);
        androidGridView.setAdapter(adapter);
        androidGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                // The title, not the achievement is stored in the grid
                String title = gridViewString[i];
                // Find achievement from the shared preferences and send a description of it
                Achievement achievement = sharedPreference.getAchievement(title);
                String description = achievement.getDescription();
                String percentComplete = achievement.getPercentProgress();
                // Send a dialog informing the user about the achievement
                sendDescriptionDialog(title, description, percentComplete);
            }
        });
        return view;

    }

    private void sendDescriptionDialog(String title, String description, String percentComplete){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(title);
        String formatMsg = getString(R.string.msg_achievement_description_format);
        String message = String.format(formatMsg, description, percentComplete);
        adb.setMessage(message);
        AlertDialog ad = adb.create();
        ad.show();
    }
}
