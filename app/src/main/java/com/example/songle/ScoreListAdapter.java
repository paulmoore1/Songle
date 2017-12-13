package com.example.songle;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

/**
 * Created by Paul on 12/12/2017.
 * Shows scores as a list.
 */

class ScoreListAdapter extends ArrayAdapter<Score>{
    private final Context context;
    private final List<Score> scores;

    ScoreListAdapter(Context context, List<Score> scores){
        super(context, R.layout.scores_list_item, scores);
        this.context = context;
        this.scores = scores;
    }

    private class ViewHolder {
        TextView scorePoints;
        TextView scoreDate;
        ImageView trophy;
    }

    @Override
    public int getCount(){
        if (scores != null) return scores.size();
        else return 0;
    }

    @Override
    public Score getItem(int position){
        return scores.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent){
        ViewHolder holder;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.scores_list_item, null);
            holder = new ViewHolder();
            holder.scorePoints = convertView.findViewById(R.id.score_points);
            holder.scoreDate = convertView.findViewById(R.id.score_date);
            holder.trophy = convertView.findViewById(R.id.img_score);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Score score = getItem(position);
        holder.scorePoints.setText(String.valueOf(score.getScore()));
        holder.scoreDate.setText(score.getDate());
        if (position == 0)
            holder.trophy.setImageResource(R.drawable.ic_gold_medal);
        else if (position == 1)
            holder.trophy.setImageResource(R.drawable.ic_silver_medal);
        else if (position == 2)
            holder.trophy.setImageResource(R.drawable.ic_bronze_medal);
        return convertView;
    }


}
