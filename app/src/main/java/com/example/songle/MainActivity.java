package com.example.songle;

import android.content.Context;
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
import java.util.Calendar;
import java.util.List;

public class MainActivity extends FragmentActivity implements DownloadCallback {
    //Broadcast receiver that tracks network connectivity changes
    private NetworkReceiver receiver = new NetworkReceiver();

    // Keep a reference to the NetworkFragment , which owns the AsyncTask object that is
    //used to execute network ops.
    private NetworkFragment mNetworkFragment;

    // boolean telling us whether a download is in progress so we don't trigger overlapping
    // downloads with consecutive button clicks
    private boolean mDownloading = false;

    Context context = getActivity();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);


        mNetworkFragment = NetworkFragment.getInstance(getFragmentManager(), "https://www.google.com");

        //check for internet access first (need for saved data)
        boolean networkOn = isNetworkAvailable(this);
        //if there is internet, load as normal
        if (!networkOn) {
            //with no internet, send an alert that will close the app.
            NoInternetAccessDialogFragment networkError = new NoInternetAccessDialogFragment();
            //TODO doesn't work - just goes blank
            networkError.getDialog();
            System.exit(0);
        }
    //TODO check this is the correct way to find the string value
        SharedPreferences save = getSharedPreferences(String.valueOf(R.string.saved_timestamp), 0);


    }
    // checks if a network connection is available
    // Code from https://stackoverflow.com/questions/19240627/how-to-check-internet-connection-available-or-not-when-application-start/19240810#19240810
    public boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    //public void newGame(View view){
    //    boolean savedGame = checkSavedGame();

    //}


    public void startMaps(View view){
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
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

    //For network connectivity
    private void startDownload(){
        if (!mDownloading && mNetworkFragment != null){
            //Execute the async download.
            mNetworkFragment.startDownload();
            mDownloading = true;
        }
    }





    @Override
    public void updateFromDownload(Object result) {

    }

    @Override
    public NetworkInfo getActiveNetworkInfo(){

    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {

    }

    @Override
    public void finishDownloading() {

    }

    public Context getActivity() {
        return this;
    }

    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
        private String TAG = DownloadXmlTask.class.getSimpleName();
        private String mostRecentXML;

        @Override
        protected  String doInBackground(String... urls){
            Log.v(TAG, "Started loading XML in the background");
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e){
                return "Unable to load content. Check your network connection";
            } catch (XmlPullParserException e){
                return "Error parsing XML";
            }
        }

        //Given a string representation of a URL, sets up a connection and gets
        //an input stream
        private InputStream downloadUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /*milliseconds*/);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            Log.v(TAG, "Preparing to download");
            conn.connect();
            return conn.getInputStream();

        }


        private String loadXmlFromNetwork(String urlString) throws
                XmlPullParserException, IOException{
            StringBuilder result = new StringBuilder();
            InputStream stream = null;
            //Instantiate the parser.
            XmlParser parser = new XmlParser();
            List<XmlParser.Song> songs = null;
            String title = null;
            String url = null;
            String summary = null;
            Calendar rightNow = Calendar.getInstance();
            DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");


            try {
                stream = downloadUrl(urlString);
                songs = parser.parse(stream);

                String timestamp = parser.getXmlTimestamp(stream);
                if (timestamp.equals(mostRecentXML)){
                    return "XML up to date";
                } else {
                    List<XmlParser.Song> songs = parser.parse(stream);
                    mostRecentXML = timestamp;
                }
            }
            return result.toString();
        }



        @Override
        protected void onPostExecute(String result){
            setContentView(R.layout.activity_main);
            //Displays the HTML string in the UI via a WebView
            WebView myWebView = (WebView) findViewById(R.id.webview);
            myWebView.loadData(result, "text/html", null);
        }


    }
}
