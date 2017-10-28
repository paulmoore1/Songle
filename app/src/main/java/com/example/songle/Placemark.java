package com.example.songle;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Paul Moore on 28-Oct-17.
 */

public class Placemark {
    private int line;
    private int word;
    private String description;
    private LatLng location;

    public Placemark(int line, int word, String description, LatLng location){
        this.line = line;
        this.word = word;
        this.description = description;
        this.location = location;
    }

    public int getLine(){
        return line;
    }

    public int getWord(){
        return word;
    }

    public String getDescription(){
        return description;
    }

    public LatLng getLocation(){
        return location;
    }



}
