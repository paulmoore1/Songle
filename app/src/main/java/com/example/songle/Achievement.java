package com.example.songle;

import android.util.Log;

/**
 * Created by Paul on 05/12/2017.
 * Class for tracking and updating achievements.
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

    Achievement(String title, String description, int stepsGoal, int greyPictureID,
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

    String getDescription(){
        return description;
    }

    int getSteps(){
        return steps;
    }

    int getGreyPictureID(){
        return greyPictureID;
    }

    int getColorPictureID(){
        return colorPictureID;
    }

    boolean isAchieved(){
        return achieved;
    }

    boolean isHidden(){
        return hidden;
    }

    void incrementSteps(){
        steps++;
        if (steps >= stepsGoal) achieved = true;
    }

    void setSteps(int steps){
        this.steps = steps;
        achieved = steps >= stepsGoal;
    }

    void resetAchievement(){
        steps = 0;
        achieved = false;
    }

    String getPercentProgress(){
        float percent = ((float) steps / (float) stepsGoal)*100;
        if (percent < 100) return String.valueOf(Math.round(percent)) + "% complete";
        else return "Completed!";
    }

    public String toString(){
        return getTitle()
                + "\n" + getDescription()
                + "\nSteps so far: " + String.valueOf(steps)
                + "\nSteps required: " + String.valueOf(stepsGoal)
                + "\nHidden: " + String.valueOf(hidden)
                + "\nAchieved: " + String.valueOf(achieved);
    }


}
