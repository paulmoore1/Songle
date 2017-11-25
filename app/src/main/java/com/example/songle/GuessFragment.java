package com.example.songle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Paul on 21/11/2017.
 */

public class GuessFragment extends Fragment {
    private static final String TAG = "GuessFragment";

    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
    }
/*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_guess_tab, container);

        return view;
    }
    */

}
