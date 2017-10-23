package com.example.songle;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.URL;


/**
 * Created by Paul Moore on 19-Oct-17.
 */

public class NetworkFragment extends Fragment {
    public static final String TAG = "NetworkFragment";

    private static final String URL_KEY = "UrlKey";

    private DownloadCallback mCallback;
    private DownloadTask mDownloadTask;
    private String mUrlString;
    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url){
        NetworkFragment networkFragment = new NetworkFragment();
        Bundle args = new Bundle();
        args.putString(URL_KEY, url);
        networkFragment.setArguments(args);
        fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mUrlString = getArguments().getString(URL_KEY);
        //may include more
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        // host Activity will handle callbacks from a task.
        mCallback = (DownloadCallback) context;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        // Clear reference to host Activity to avoid memory leak.
        mCallback = null;
    }

    @Override
    public void onDestroy(){
        // Cancel task when Fragment is destroyed.
        cancelDownload();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadTask
     */
    public void startDownload(){
        cancelDownload();
        mDownloadTask = new DownloadTask();
        mDownloadTask.execute(mUrlString);
    }

    /**
     * Cancel (and interrupt if necessary any ongoing DownloadTask execution
     */
    public void cancelDownload(){
        if (mDownloadTask != null){
            mDownloadTask.cancel(true);
        }
    }
    private class DownloadTask extends AsyncTask<String, Void, com.example.songle.DownloadTask.Result> {
        private DownloadCallback<String> mCallback;

        DownloadTask(DownloadCallback<String> callback){
            setCallback(callback);
        }
        void setCallback(DownloadCallback<String> callback){
            mCallback = callback;
        }
        /**
         * Wrapper class that serves as a union of a result value and an exception. When the download
         * task has completed, either the result value or exception can be a non-null value.
         * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
         */
        static class Result {
            public String mResultValue;
            public Exception mException;
            public Result(String resultValue){
                mResultValue = resultValue;
            }
            public Result(Exception exception){
                mException = exception;
            }
        }
        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute(){
            if (mCallback != null){
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)){
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        @Override
        protected com.example.songle.DownloadTask.Result doInBackground(String... urls){
            com.example.songle.DownloadTask.Result result = null;
            if (!isCancelled() && urls != null && urls.length > 0){
                String urlString = urls[0];
                try {
                    URL url = new URL(urlString);
                    String resultString = downloadUrl(url);
                    if (resultString != null){
                        result = new com.example.songle.DownloadTask.Result(resultString);
                    } else {
                        throw new IOException("No response received.");
                    }
                } catch(Exception e){
                    result = new com.example.songle.DownloadTask.Result(e);
                }
            }
            return result;
        }
        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(com.example.songle.DownloadTask.Result result) {
            if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback.updateFromDownload(result.mException.getMessage());
                } else if (result.mResultValue != null) {
                    mCallback.updateFromDownload(result.mResultValue);
                }
                mCallback.finishDownloading();
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(com.example.songle.DownloadTask.Result result) {
        }
    }
}
