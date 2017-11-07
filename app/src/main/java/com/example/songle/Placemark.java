package com.example.songle;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Paul Moore on 28-Oct-17.
 */

public class Placemark {
    private String key;
    private String description;
    private LatLng location;

    public Placemark(String key, String description, LatLng location){
        this.key = key;
        this.description = description;
        this.location = location;
    }

    public String getKey(){
        return key;
    }

    public String getDescription(){
        return description;
    }

    public LatLng getLocation(){
        return location;
    }



}
