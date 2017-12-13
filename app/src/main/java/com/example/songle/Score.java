package com.example.songle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Paul on 07/12/2017.
 * A Class for keeping track of scores and interesting related information.
 */

public class Score {
    private final int score;
    private final float distance;
    private final String timeTaken;
    private final String title;
    private final String date;

    Score(int score, float distance, String title, String timeTaken){
        this.score = score;
        this.distance = distance;
        this.title = title;
        this.timeTaken = timeTaken;
        DateFormat df = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        Date date = new Date();
        this.date = df.format(date);
    }

    int getScore(){
        return score;
    }

    float getDistance(){
        return  distance;
    }

    public String getTitle(){
        return title;
    }

    public String getTimeTaken(){
        return timeTaken;
    }

    String getDate(){
        return date;
    }



}
