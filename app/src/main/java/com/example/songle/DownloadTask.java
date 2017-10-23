package com.example.songle;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.URL;

/**
 * Created by Paul Moore on 19-Oct-17.
 */
public class DownloadTask extends AsyncTask<String, Void, DownloadTask.Result> {
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
    protected DownloadTask.Result doInBackground(String... urls){
        Result result = null;
        if (!isCancelled() && urls != null && urls.length > 0){
            String urlString = urls[0];
            try {
                URL url = new URL(urlString);
                String resultString = downloadUrl(url);
                if (resultString != null){
                    result = new Result(resultString);
                } else {
                    throw new IOException("No response received.");
                }
            } catch(Exception e){
                result = new Result(e);
            }
        }
        return result;
    }
    /**
     * Updates the DownloadCallback with the result.
     */
    @Override
    protected void onPostExecute(Result result) {
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
    protected void onCancelled(Result result) {
    }
}
