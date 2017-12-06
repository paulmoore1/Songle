package com.example.songle;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Paul on 06/12/2017.
 */

public class AchievementListAdapter extends ArrayAdapter<Achievement> {
    private static final String TAG = AchievementListAdapter.class.getSimpleName();
    private Context context;
    private int currentSongNumber = -1;
    private List<Achievement> achievements;
    private SharedPreference sharedPreference;

    public AchievementListAdapter(Context context, List<Achievement> achievements){
        super(context, R.layout.achievement_list_item, achievements);
        this.context = context;
        this.achievements = achievements;
        sharedPreference = new SharedPreference(context);
    }

    private class ViewHolder {
        TextView achievementTitle;
        TextView achievementProgress;
        ImageView achievementImage;
    }

    @Override
    public int getCount(){
        return achievements.size();
    }

    @Override
    public Achievement getItem(int position){
        return achievements.get(position);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent){
        ViewHolder holder;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.achievement_list_item, null);
            holder = new ViewHolder();
            holder.achievementTitle = (TextView) convertView.findViewById(R.id.achievement_title);
            holder.achievementProgress = (TextView) convertView.findViewById(R.id.achievement_progress);
            holder.achievementImage = (ImageView) convertView.findViewById(R.id.imgbtn_achievement);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();

        }
        Achievement achievement = getItem(position);
        holder.achievementTitle.setText(achievement.getTitle());
        holder.achievementProgress.setText(achievement.getPercentProgress());

        //make the icon colored if the achievement is achieved, grey otherwise
        if (achievement.isAchieved()){
            holder.achievementImage.setImageResource(achievement.getColorPictureID());
        } else {
            holder.achievementImage.setImageResource(achievement.getGreyPictureID());
        }
       /* //TODO fix weird coloring bug here
       //show last chosen song as the primary color
        if (currentSongNumber != -1){
            if (currentSongNumber == position) {
                holder.achievementTitle.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.achievementProgress.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            }

        }
        */
        return convertView;

    }

}

