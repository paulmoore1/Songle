package com.example.songle;

import android.app.Application;
import android.content.Intent;

/**
 * Created by Paul on 12/12/2017.
 * This an related classes for error reporting used from
 * https://stackoverflow.com/questions/19897628/need-to-handle-uncaught-exception-and-send-log-file
 */

public class MyApplication extends Application {
    public void onCreate(){
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException (Thread thread, Throwable e){
                handleUncaughtException(thread, e);
            }
        });
    }

    private void handleUncaughtException(Thread thread, Throwable e){
        e.printStackTrace();
        Intent intent = new Intent();
        intent.setAction("com.example.songle.SEND_LOG");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        System.exit(1); //kill off the crashed app
    }
}
