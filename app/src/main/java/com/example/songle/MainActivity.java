package com.example.songle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity implements DownloadCallback {

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
            sendNetworkWarningDialog();
        }


        // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);


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
        Log.i("MainActivity", result.toString());
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

    public Activity getAcitivity(){
        return this;
    }

    //use if a network connection is required for the selected option to work
    private void sendNetworkErrorDialog(){
        //with no internet, send an alert that will take user to settings or close the app.
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.network_error);
        adb.setMessage(R.string.msg_data_required);
        adb.setItems(new CharSequence[]{
                        getString(R.string.btn_open_wifi_settings),
                        getString(R.string.btn_open_data_settings),
                        getString(R.string.btn_exit)
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                //go to Wifi settings
                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                break;
                            case 1:
                                //go to data settings
                                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                                break;
                            case 2:
                                //exit the app
                                finish();
                                break;
                        }
                    }
                });

        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    //use if a network connection will probably be required (but not for certain)
    private void sendNetworkWarningDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.network_warning);
        adb.setMessage(R.string.msg_data_warning);
        //dismiss dialog if 'Continue' selected
        adb.setPositiveButton(R.string.btn_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //stop app if 'Exit' selected
        adb.setNegativeButton(R.string.btn_exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }
}
