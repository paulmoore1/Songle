package com.example.songle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.FragmentManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.EditText;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends FragmentActivity implements DownloadCallback {
    SharedPreferences sharedpreferences;
    public static final String SavedTimeStamp = "timestampKey";
    private String timestamp;

    //Broadcast receiver that tracks network connectivity changes
    private NetworkReceiver receiver = new NetworkReceiver();

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;




    // boolean telling us whether a download is in progress so we don't trigger overlapping
    // downloads with consecutive button clicks
    private boolean mDownloading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check for internet access first
        boolean networkOn = isNetworkAvailable(this);
        //if there is internet, load as normal
        if (!networkOn) {
            //with no internet, send an alert that will close the app.
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("A network connection is required! Please check your connection and load again");
            alertDialogBuilder.setPositiveButton(R.string.okay,
                    new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface arg0, int arg1){
                            System.exit(0);
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }


        // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);

        //
        sharedpreferences = getSharedPreferences("com.example.songle" ,Context.MODE_PRIVATE);
        // check if the saved Timestamp is in the shared preferences - if it is then load it
        if (sharedpreferences.contains(SavedTimeStamp)){
            timestamp = sharedpreferences.getString(SavedTimeStamp, "");
        }

        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(),
                getResources().getString(R.string.url_songs_xml));
        mNetworkFragment.startXmlDownload();

    }


    //public void newGame(View view){
    //    boolean savedGame = checkSavedGame();

    //}


    public void startMaps(View view){
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }


    private void startDownload(){
        if (!mDownloading && mNetworkFragment != null){
            //Execute the async download.
            mNetworkFragment.startGeneralDownload();
            mDownloading = true;
        }
    }





    @Override
    public void updateFromDownload(Object result) {
        Log.i("MainActivity", result.getClass().toString());
    }



    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                break;
            case Progress.CONNECT_SUCCESS:
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                //could add something like this
                //mDataText.setText("" + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
        }
    }

    @Override
    public void finishDownloading() {

    }

    // checks if a network connection is available
    // Code from https://stackoverflow.com/questions/19240627/how-to-check-internet-connection-available-or-not-when-application-start/19240810#19240810
    public boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
