package com.example.songle;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Paul on 12/12/2017.
 * Adapted slightly from stackoverflow (see class MyApplication)
 */

public class SendLog extends Activity implements View.OnClickListener {
    private static final String TAG = SendLog.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.send_log);
        Button sendLogs = findViewById(R.id.btn_send_logs);
        Button restart = findViewById(R.id.btn_restart);
        sendLogs.setOnClickListener(this);
        restart.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_send_logs){
            sendLogFile();
        } else if (v.getId() == R.id.btn_restart){
            Intent intent = new Intent(this, Splash.class);
            startActivity(intent);
        }
    }

    private String extractLogToFile(){
        Log.e(TAG, "ExtractLogToFile");
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {

        }
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        //Make file name - file must be saved to external storage or it won't be readable to the
        //email app
        String path = Environment.getExternalStorageDirectory() + "/" + "Songle/";
        String fullName = path + "log_file.txt";

        //Extract to file
        File file = new File(fullName);
        InputStreamReader reader = null;
        FileWriter writer = null;
        try {
            String cmd = "logcat -d -v time";
            //get input stream
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());

            //write output stream
            writer = new FileWriter(file);
            writer.write("Android version: " + Build.VERSION.SDK_INT + "\n");
            writer.write("Device: " + model + "\n");
            writer.write("App version: " + (info == null ? "(null)" : info.versionCode));

            char [] buffer = new char[10000];
            do {
                int n = reader.read(buffer, 0, buffer.length);
                if (n == -1) break;
                writer.write(buffer, 0, n);
            } while (true);
            reader.close();
            writer.close();

        } catch (IOException e){
            Log.e(TAG, "IOException" + e);
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e2){
                    Log.e(TAG, "Exception" + e2);
                }
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e2){
                    Log.e(TAG, "Exception" + e2);
                }
                Log.e(TAG, "Error: ended up as null");
            return null;
        }
        Log.e(TAG, "Finished, with fullName = " + fullName);
        return fullName;
    }

    private void sendLogFile(){
        String fullName = extractLogToFile();
        if (fullName == null){
            Log.e(TAG, "Full name was null");
            return;
        }
        Log.e(TAG, "Full name was " + fullName);


        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String []{"lapilosew2003@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Songle log file");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + fullName));
        intent.putExtra(Intent.EXTRA_TEXT, "Log file attached.");
        startActivity(intent);
    }
}
