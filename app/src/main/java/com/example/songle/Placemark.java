package com.example.songle;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Paul Moore on 28-Oct-17.
 * Class for storing Placemarks.
 */

class Placemark {
    private final String key;
    private final String description;
    private final LatLng location;

    Placemark(String key, String description, LatLng location){
        this.key = key;
        this.description = description;
        this.location = location;
    }

    public String getKey(){
        return key;
    }

    String getDescription(){
        return description;
    }

    LatLng getLocation(){
        return location;
    }



}
