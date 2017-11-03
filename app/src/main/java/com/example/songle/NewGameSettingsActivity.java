package com.example.songle;

import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;

public class NewGameSettingsActivity extends FragmentActivity {
    private Fragment contentFragment;
    SongListFragment songListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_game);

        FragmentManager fragmentManager = getSupportFragmentManager();

        final Button bSelectSong= findViewById(R.id.btn_select_song);
        bSelectSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show the song selection fragment
                setFragmentTitle(R.id.btn_select_song);
                songListFragment = new SongListFragment();
                switchContent(songListFragment, SongListFragment.ARG_ITEM_ID);
            }
        });

        final Button bSelectDifficulty = findViewById(R.id.btn_select_difficulty);
        bSelectDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        final Button bStartGame = findViewById(R.id.btn_start_game);
        bStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        /**
         * This is called when the orientation is changed.
         * If the song fragment is on the screen, it is retained.
         */
        if (savedInstanceState != null){
            if (savedInstanceState.containsKey("content")){
                String content = savedInstanceState.getString("content");
                if (content.equals(SongListFragment.ARG_ITEM_ID)){
                    if (fragmentManager.findFragmentByTag(SongListFragment.ARG_ITEM_ID) != null){
                        setFragmentTitle(R.string.txt_select_song);
                        contentFragment = fragmentManager.findFragmentByTag(SongListFragment.ARG_ITEM_ID);
                    }
                }
            }
        }


    }

    protected void setFragmentTitle(int resourceID){
        setTitle(resourceID);
        getActionBar().setTitle(resourceID);
    }

    public void switchContent(Fragment fragment, String tag){
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null){
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content_frame, fragment, tag);
            transaction.addToBackStack(tag);
            transaction.commit();
            contentFragment = fragment;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        if (contentFragment instanceof SongListFragment){
            outState.putString("contetnt", SongListFragment.ARG_ITEM_ID);
        }
        super.onSaveInstanceState(outState);
    }

}
