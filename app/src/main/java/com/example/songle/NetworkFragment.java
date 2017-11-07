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

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
    public static final String TAG = "NetworkFragment";

    private static final String URL_KEY = "UrlKey";

    private DownloadCallback mCallback;
    private DownloadLyricsTask mDownloadLyricsTask;
    private DownloadXmlTask mDownloadXmlTask;
    private DownloadKmlTask mDownloadKmlTask;
    private String mUrlString;
    private String mostRecentXMLTimestamp;
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
        Log.d(TAG, "onCreate() invoked");
        super.onCreate(savedInstanceState);
        // Retain this Fragment across configuration changes in the host Activity.
        setRetainInstance(true);

        mUrlString = getArguments().getString(URL_KEY);
        Log.d(TAG, "URL String:" + mUrlString);
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach called");
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
        Log.d(TAG, "startLyricsDownload called");
        cancelDownload();
        mDownloadLyricsTask = new DownloadLyricsTask();
        downloadType = "Lyrics";
        mDownloadLyricsTask.execute(mUrlString);
    }

    /**
     * Start non-blocking execution of DownloadXmlTask.
     */
    public void startXmlDownload() {
        Log.d(TAG, "startXmlDownload called");
        cancelDownload();
        mDownloadXmlTask = new DownloadXmlTask();
        downloadType = "Xml";
        mDownloadXmlTask.execute(mUrlString);
    }

    public void startKmlDownload(){
        Log.d(TAG, "startKmlDownload called");
        cancelDownload();
        mDownloadKmlTask = new DownloadKmlTask();
        downloadType = "Kml";
        mDownloadKmlTask.execute(mUrlString);
    }

    public void retryDownload(){
        Log.d(TAG, "retryDownload called");
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
        Log.d(TAG, "cancelDownload called");
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
        private String TAG = DownloadLyricsTask.class.getSimpleName();
        private SharedPreference sharedPreferenceDownloadLyrics = new SharedPreference();

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
            Log.v(TAG, "Started loading lyrics in the background");
            String result = null;
            try {
                result = loadLyricsFromNetwork(urls[0]);
            } catch (IOException e){
                Log.e(TAG, "Unable to load content");
            }
            if (result.equals("Parsed")){
                onPostExecute("Updated");
                return "Lyrics updated";
            } else {
                onPostExecute("Not updated");
                return "Lyrics not updated";
            }

        }

        private String loadLyricsFromNetwork(String urlString) throws
        IOException{
            Log.d(TAG,"loadLyrisFromNetwork called");
            InputStream stream = null;
            LyricsTextParser ltp = new LyricsTextParser(getActivity().getApplicationContext());
            try {
                Log.d(TAG, "URL is: " + urlString);
                stream = downloadUrl(urlString);
                ltp.parse(stream);
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
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server.
         * If the network request is successful, it returns the response body in String form. Otherwise,
         * it will throw an IOException.
         */
        private InputStream downloadUrl(String urlString) throws IOException {
            Log.d(TAG, "downloadUrl called");
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /*milliseconds*/);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            return conn.getInputStream();

        }

    }

    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
        private String TAG = DownloadXmlTask.class.getSimpleName();
        private SharedPreference sharedPreferenceDownloadXml = new SharedPreference();

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
            Log.d(TAG, "doInBackground called");
            String timestamp = sharedPreferenceDownloadXml.getMostRecentTimestamp(getContext());
            if (timestamp == null){
                mostRecentXMLTimestamp = getString(R.string.default_timestamp);
            }
            List<Song> songs;
            try {
                songs = loadXmlFromNetwork(urls[0]);
            } catch (IOException e){
                Log.e(TAG,"Unable to load content. Check your network connection");
                return null;
            } catch (XmlPullParserException e){
                Log.e(TAG,"Error parsing XML");
                return null;
            }
            //songs will not be null if the timestamp is new. In that case save the new songs list
            if (songs != null){
                sharedPreferenceDownloadXml.saveSongs(getActivity().getApplicationContext(), songs);
                Log.d(TAG, "finished background task");
                onPostExecute("Updated");
                return "Songs updated";
            } else {
                onPostExecute("Not updated");
                return "Songs not updated";
            }
        }

        private List<Song> loadXmlFromNetwork(String urlString) throws
                XmlPullParserException, IOException{
            Log.d(TAG, "loadXmlFromNetwork called");
            InputStream stream = null;
            //Instantiate the parser.
            XmlSongParser parser = new XmlSongParser(getActivity().getApplicationContext());
            List<Song> songs = null;

            try {
                stream = downloadUrl(urlString);
                songs = parser.parse(stream, mostRecentXMLTimestamp);
            } catch(Exception e){
                Log.e(TAG, "Exception: " + e);
            }
            return songs;
        }

        //Given a string representation of a URL, sets up a connection and gets
        //an input stream
        private InputStream downloadUrl(String urlString) throws IOException {
            Log.d(TAG, "downloadUrl called");
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /*milliseconds*/);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            return conn.getInputStream();

        }



        @Override
        protected void onPostExecute(String result){
            Log.d(TAG, "onPostExecute called");
            if (result != null && mCallback != null) {
                mCallback.updateFromDownload(result);
                Log.d(TAG, "Finished downloading");

                mCallback.finishDownloading();
            }

        }


    }

    private class DownloadKmlTask extends AsyncTask<String, Void, String> {
        private String TAG = DownloadKmlTask.class.getSimpleName();
        private SharedPreference sharedPreferenceDownloadKml = new SharedPreference();

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
            Log.v(TAG, "Started loading KML in the background");
            Boolean allDownloadedCorrectly = true;
            String baseUrl = urls[0];
            String songNumber = sharedPreferenceDownloadKml.getCurrentSongNumber(getActivity().getApplicationContext());
            for (int i = 1; i < 6; i++){
                List<Placemark> placemarks = null;
                String mapNumber = Integer.toString(i);
                String urlString = baseUrl + songNumber + "/map" + mapNumber + ".kml";
                Log.d(TAG, "Map URL: " + urlString);
                try {
                    placemarks = loadKmlFromNetwork(urlString);
                } catch (IOException e){
                    Log.e(TAG, "IOException: " + e);
                } catch (XmlPullParserException e){
                    Log.e(TAG, "XML Exception: " + e);
                }
                if (placemarks != null){
                    Log.d(TAG, "Downloaded map#" + i);
                    sharedPreferenceDownloadKml.saveMap(getActivity().getApplicationContext(),
                            placemarks, mapNumber);
                } else {
                    Log.e(TAG, "Error downloading maps");
                    allDownloadedCorrectly = false;
                }
            }
            if (allDownloadedCorrectly){
                onPostExecute("Updated");
                return "Maps updated";
            } else {
                onPostExecute("Not updated");
                return "Maps not updated";
            }

        }

        private List<Placemark> loadKmlFromNetwork(String urlString) throws
                XmlPullParserException, IOException{
            Log.d(TAG, "loadKmlFromNetwork called");
            InputStream stream = null;
            //Instantiate the parser.
            XmlMapParser parser = new XmlMapParser();
            List<Placemark> placemarks = null;

            try {
                stream = downloadUrl(urlString);
                placemarks = parser.parse(stream);
            } catch(Exception e){
                Log.e(TAG, "Exception: " + e);
            }
            return placemarks;
        }

        //Given a string representation of a URL, sets up a connection and gets
        //an input stream
        private InputStream downloadUrl(String urlString) throws IOException {
            Log.d(TAG, "downloadUrl called");
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /*milliseconds*/);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            return conn.getInputStream();

        }



        @Override
        protected void onPostExecute(String result){
            Log.d(TAG, "onPostExecute called");
            if (result != null && mCallback != null) {
                mCallback.updateFromDownload(result);
                Log.d(TAG, "Finished downloading");

                mCallback.finishDownloading();
            }

        }


    }
}