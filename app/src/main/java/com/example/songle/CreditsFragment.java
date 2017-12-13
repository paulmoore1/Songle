package com.example.songle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * Created by Paul on 12/12/2017.
 * Simple fragment for displaying the credits for the app.
 */

public class CreditsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.credits_fragment, container, false);
        TextView credits = view.findViewById(R.id.text_view_credit);
        credits.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }

}
