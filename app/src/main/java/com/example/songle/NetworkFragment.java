package com.example.songle;

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Implementation of headless Fragment that runs an AsyncTask to fetch data from the network.
 */
public class NetworkFragment extends Fragment {
    private static final String TAG = "NetworkFragment";
    private static final String URL_KEY = "UrlKey";
    private DownloadCallback mCallback;
    private DownloadLyricsTask mDownloadLyricsTask;
    private DownloadXmlTask mDownloadXmlTask;
    private DownloadKmlTask mDownloadKmlTask;
    private String mUrlString;
    private String downloadType;

    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url) {
        // Recover NetworkFragment in case we are re-creating the Activity due to a config change.
        // This is necessary because NetworkFragment might have a task that began running before
        // the config change and has not finished yet.
        // The NetworkFragment is recoverable via this method because it calls
        // setRetainInstance(true) upon creation.
        NetworkFragment networkFragment = (NetworkFragment) fragmentManager
                .findFragmentByTag(NetworkFragment.TAG);
        if (networkFragment == null) {
            networkFragment = new NetworkFragment();
            Bundle args = new Bundle();
            args.putString(URL_KEY, url);
            networkFragment.setArguments(args);
            fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        }
        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this Fragment across configuration changes in the host Activity.
        setRetainInstance(true);
        mUrlString = getArguments().getString(URL_KEY);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
        mCallback = (DownloadCallback)context;
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach called");
        super.onDetach();
        // Clear reference to host Activity.
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelDownload();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadLyricsTask.
     */
    public void startLyricsDownload() {
        Log.v(TAG, "startLyricsDownload called");
        cancelDownload();
        mDownloadLyricsTask = new DownloadLyricsTask();
        downloadType = "Lyrics";
        mDownloadLyricsTask.execute(mUrlString);
    }

    /**
     * Start non-blocking execution of DownloadXmlTask.
     */
    public void startXmlDownload() {
        Log.v(TAG, "startDownloadingSongs called");
        cancelDownload();
        mDownloadXmlTask = new DownloadXmlTask();
        downloadType = "Xml";
        mDownloadXmlTask.execute(mUrlString);
    }

    public void startKmlDownload(){
        Log.v(TAG, "startKmlDownload called");
        cancelDownload();
        mDownloadKmlTask = new DownloadKmlTask();
        downloadType = "Kml";
        mDownloadKmlTask.execute(mUrlString);
    }

    public void retryDownload(){
        Log.d(TAG, "retryDownload called for: " + downloadType);
        //check that a download task was executed already
        switch (downloadType) {
            case "Lyrics":
                startLyricsDownload();
                break;
            case "Xml":
                startXmlDownload();
                break;
            case "Kml":
                startKmlDownload();
                break;
            default:
                break;
        }
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing DownloadTask execution.
     */
    public void cancelDownload() {
        Log.v(TAG, "cancelDownload called");
        if (mDownloadLyricsTask != null) {
            mDownloadLyricsTask.cancel(true);
            mDownloadLyricsTask = null;
        } else if (mDownloadXmlTask != null){
            mDownloadXmlTask.cancel(true);
            mDownloadXmlTask = null;
        } else if (mDownloadKmlTask != null){
            mDownloadKmlTask.cancel(true);
            mDownloadKmlTask = null;
        }
    }


    /**
     * Implementation of AsyncTask that runs a network operation on a background thread.
     */
    private class DownloadLyricsTask extends AsyncTask<String, Integer, String> {
        private final String TAG = DownloadLyricsTask.class.getSimpleName();
        private SharedPreference sharedPreferenceLyrics =
                new SharedPreference(getActivity().getApplicationContext());

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected String doInBackground(String... urls) {
            Log.v(TAG, "Started loading_layout lyrics in the background");
            String result = loadLyricsFromNetwork();
            if (result != null && result.equals("Parsed")){
                Log.e(TAG,"Was parsed");
                onPostExecute("Updated");
                return "Updated";
            } else {
                Log.e(TAG, "Was not parsed");
                onPostExecute("Not updated");
                return "Not updated";
            }
        }

        private String loadLyricsFromNetwork(){
            Log.v(TAG, "loadLyricsFromNetwork called");
            String songNumber = sharedPreferenceLyrics.getCurrentSongNumber();
            String mUrlString = getString(R.string.url_general) +
                    songNumber + "/words.txt";
            try {
                Log.d(TAG, "URL is: " + mUrlString);
                downloadUrl(mUrlString);
                return "Parsed";
            } catch (IOException e){
                Log.e(TAG, "Error: " + e);
                return "Error";
            }
        }

        /**
         * Send DownloadCallback a progress update.
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values.length >= 2) {
                mCallback.onProgressUpdate(values[0], values[1]);
            }
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "onPostExecute called");
            if (result != null && mCallback != null) {
                mCallback.updateFromDownload(result);
                Log.d(TAG, "Finished downloading");
                mCallback.finishDownloading();
            }
        }


        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(String result) {
            Log.d(TAG, "onCancelled called");
            cancelDownload();
            mCallback.finishDownloading();
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server.
         * Downloads and parses the entire stream.
         */
        private void downloadUrl(String urlString) throws IOException {
            Log.v(TAG, "downloadUrl called");
            String songNumber = sharedPreferenceLyrics.getCurrentSongNumber();
            LyricsTextParser ltp = new LyricsTextParser(getActivity().getApplicationContext(), songNumber);

            InputStream stream = null;
            URL url = new URL(urlString);
            HttpURLConnection conn = null;
            try{
                conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /*milliseconds*/);
                conn.setConnectTimeout(15000 /*milliseconds*/);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                stream = conn.getInputStream();
                ltp.parse(stream);
            } finally{
                if (stream != null){
                    stream.close();
                }
                if (conn != null){
                    conn.disconnect();
                }

            }

        }

    }

    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
        private String TAG = DownloadXmlTask.class.getSimpleName();
        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        @Override
        protected String doInBackground(String... urls){
            Log.v(TAG, "doInBackground called");
            loadXmlFromNetwork(urls[0]);
            return "Updated";
        }

        private void loadXmlFromNetwork(String urlString){
            Log.v(TAG, "loadXmlFromNetwork called on string: " + urlString);
            try {
                downloadUrl(urlString);
            } catch(Exception e){
                Log.e(TAG, "Exception when downloading: " + e);
            }

        }


        /**
         * Given a string representation of a URL, sets up a connection, downloads and parses it
         * The parsing is done here because when the connection disconnected, it seemed that
         * the input stream was sometimes closed too early.
         * Simply leaving the connection open (as in some tutorials) resulted in a leak that caused
         * crashes later on.
         * The simplest solution was to download and parse in the same function, so that the stream
         * could be fully parsed, and the connection fully disconnected at the end.
         * @param urlString - URL to download
         * @throws IOException - exception if there is an error
         */
        private void downloadUrl(String urlString) throws IOException {
            Log.v(TAG, "downloadUrl called");
            XmlSongParser parser = new XmlSongParser(getActivity().getApplicationContext());
            InputStream stream;
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /*milliseconds*/);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            stream = conn.getInputStream();
            try {
                parser.parse(stream);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error in parsing: " + e);
                e.printStackTrace();
            }
            conn.disconnect();
        }



        @Override
        protected void onPostExecute(String result){
            Log.d(TAG, "onPostExecute called");
            if (result != null && mCallback != null) {
                mCallback.updateFromDownload(result);
                mCallback.finishDownloading();
            }

        }


    }

    private class DownloadKmlTask extends AsyncTask<String, Void, String> {
        private String TAG = DownloadKmlTask.class.getSimpleName();
        //needs separate shared preferences
        private SharedPreference sharedPreferenceKml =
                new SharedPreference(getActivity().getApplicationContext());

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        @Override
        protected String doInBackground(String... urls){
            Log.v(TAG, "Started loading_layout KML in the background");
            Boolean allDownloadedCorrectly = true;
            String baseUrl = urls[0];
            String songNumber = sharedPreferenceKml.getCurrentSongNumber();
            for (int i = 1; i < 6; i++){
                List<Placemark> placemarks = null;
                String mapNumber = Integer.toString(i);
                String urlString = baseUrl + songNumber + "/map" + mapNumber + ".kml";
                Log.d(TAG, "Map URL: " + urlString);

                placemarks = loadKmlFromNetwork(urlString);

                if (placemarks != null){
                    Log.d(TAG, "Downloaded map#" + i);
                    sharedPreferenceKml.saveMap(
                            placemarks, mapNumber, songNumber);
                } else {
                    Log.e(TAG, "Error downloading maps");
                    allDownloadedCorrectly = false;
                }
            }
            if (allDownloadedCorrectly){
                onPostExecute("Updated");
                return "Updated";
            } else {
                onPostExecute("Not updated");
                return "Maps not updated";
            }
        }

        private List<Placemark> loadKmlFromNetwork(String urlString){
            Log.v(TAG, "loadKmlFromNetwork called");
            List<Placemark> placemarks = null;
            try {
                placemarks = downloadPlacemarks(urlString);
            } catch(Exception e){
                Log.e(TAG, "Exception: " + e);
            }
            return placemarks;
        }

        //Given a string representation of a URL, sets up a connection and parses the placemarks
        // Parsing done here as odd errors were coming up with the input stream.
        private List<Placemark> downloadPlacemarks(String urlString) throws IOException {
            Log.v(TAG, "downloadPlacemarks called");
            XmlMapParser parser = new XmlMapParser();
            List<Placemark> placemarks = null;
            InputStream stream;
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /*milliseconds*/);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            stream = conn.getInputStream();
            try {
                placemarks = parser.parse(stream);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            conn.disconnect();
            return placemarks;

        }

        @Override
        protected void onPostExecute(String result){
            Log.d(TAG, "onPostExecute called");
            if (result != null && mCallback != null) {
                Log.e(TAG, "Result = " + result);
                mCallback.updateFromDownload(result);
                Log.d(TAG, "Finished downloading maps");

                mCallback.finishDownloading();
            }
        }
    }
}