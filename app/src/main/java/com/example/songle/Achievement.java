package com.example.songle;

import android.util.Log;

/**
 * Created by Paul on 05/12/2017.
 */

public class Achievement {
    private static final String TAG = Achievement.class.getSimpleName();
    private String title;
    private String description;
    private int steps;
    private int stepsGoal;
    private int greyPictureID;
    private int colorPictureID;
    private boolean achieved;
    private boolean hidden;

    public Achievement(String title, String description, int stepsGoal, int greyPictureID,
                       int colorPictureID, boolean hidden){
        if (stepsGoal > 0){
            this.title = title;
            this.description = description;
            this.steps = 0;
            this.stepsGoal = stepsGoal;
            this.greyPictureID = greyPictureID;
            this.colorPictureID = colorPictureID;
            achieved = false;
            this.hidden = hidden;
        } else {
            Log.e(TAG, "Invalid goal set");
        }
    }

    public String getTitle(){
        return title;
    }

    public String getDescription(){
        return description;
    }

    public int getSteps(){
        return steps;
    }

    public int getStepsGoal(){
        return stepsGoal;
    }

    public int getGreyPictureID(){
        return greyPictureID;
    }

    public int getColorPictureID(){
        return colorPictureID;
    }

    public boolean isAchieved(){
        return achieved;
    }

    public boolean isHidden(){
        return hidden;
    }

    public void incrementSteps(){
        steps++;
    }

    public void markAsAchieved(){
        achieved = true;
    }

    public float percentProgress(){
        return ((float) steps / (float) stepsGoal)*100;
    }

    public void addSteps(int numToAdd){
        steps = steps + numToAdd;
    }


}
