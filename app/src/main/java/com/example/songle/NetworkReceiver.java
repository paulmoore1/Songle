package com.example.songle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Arrays;

/**
 * Created by Paul Moore on 18-Oct-17.
 */

public class NetworkReceiver extends BroadcastReceiver {


    private String networkPref = "wifi";
    private final String WIFI = "wifi";
    private final String ANY = "any";


    public void setNetworkPref(String preference){
        if (preference.equals(WIFI) || preference.equals(ANY)){
            this.networkPref = preference;
        } else {
            System.err.println("Unexpected preference String");
        }

    }

    @Override
    public void onReceive(Context context, Intent intent){
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        
        if (networkPref.equals(WIFI) && networkInfo != null
                && networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
            //WIFI is connected so use WIFI

        } else if (networkPref.equals(ANY) && networkInfo != null){
            //Have a network connection and permission so use data
        } else {
            //No wifi and no permission, or no network connection
        }
    }
}
