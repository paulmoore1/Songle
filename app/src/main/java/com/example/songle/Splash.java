package com.example.songle;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * Created by Paul on 05/12/2017.
 *
 */

public class Splash extends Activity {
    private static final String TAG = Splash.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        final MediaPlayer startSound = MediaPlayer.create(getApplicationContext(), R.raw.start_app);
        startSound.start();
        startSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                startSound.release();
            }
        });

        /*New Handler to start the Home Activity, and close Splash-screen
           after some seconds
         */
        int SPLASH_DISPLAY_LENGTH = 2000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent mainIntent = new Intent(Splash.this, HomeActivity.class);
                //Indicates to the home activity that this the app has just launched.
                mainIntent.putExtra("JUST_STARTED", true);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }
}
