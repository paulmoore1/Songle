package com.example.songle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Paul on 07/12/2017.
 */

public class Score {
    private int score;
    private String title;
    private String date;

    public Score(int score, String title){
        this.score = score;
        this.title = title;
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        this.date = df.format(calendar);
    }

    public int getScore(){
        return score;
    }

    public String getTitle(){
        return title;
    }

    public String getDate(){
        return date;
    }

}
