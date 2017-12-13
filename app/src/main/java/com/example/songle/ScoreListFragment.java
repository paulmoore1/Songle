package com.example.songle;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

/**
 * Created by Paul on 12/12/2017.
 * This class working with the adapter shows the scores the user has achieved.
 */

public class ScoreListFragment extends Fragment implements AdapterView.OnItemClickListener{
    private Activity activity;
    private List<Score> scores;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        activity = getActivity();
        SharedPreference sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        // Get the latest scores
        scores = sharedPreference.getScores();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.scores_fragment, container, false);
        //Set up adapter and listener for the scores
        ListView scoresListView = view.findViewById(R.id.list_score);
        ScoreListAdapter scoreListAdapter = new ScoreListAdapter(activity, scores);
        scoresListView.setAdapter(scoreListAdapter);
        scoresListView.setOnItemClickListener(this);
        return view;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        // Get the score
        Score score = (Score) parent.getItemAtPosition(position);
        // Extract details from the score for the dialog.
        String distanceWalked = String.valueOf(score.getDistance());
        String songTitle = score.getTitle();
        String points = String.valueOf(score.getScore());
        String date = score.getDate();
        String timeTaken = score.getTimeTaken();
        String rank = String.valueOf(position + 1);
        // Display the dialog
        sendInfoDialog(songTitle, distanceWalked, points, date, timeTaken, rank);
    }

    private void sendInfoDialog(String songTitle, String distanceWalked, String points, String date,
                               String timeTaken, String rank){
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.title_score_info);
        String formatMsg = getString(R.string.msg_score_info_format);
        String msg = String.format(formatMsg, songTitle, distanceWalked, points, date, rank);
        adb.setMessage(msg);
        AlertDialog ad = adb.create();
        ad.show();
    }

}
