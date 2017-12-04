package com.example.songle;

import android.Manifest;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Paul on 21/11/2017.
 * From https://stackoverflow.com/questions/19353255/how-to-put-google-maps-v2-on-a-fragment-using-viewpager
 * and https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
 */

public class MapsFragment extends Fragment implements GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback{
    private static final String TAG = MapsFragment.class.getSimpleName();

    private MapView mMapView;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private Activity activity;
    private SharedPreference sharedPreference;
    private static final LatLngBounds UNIVERSITY_EDINBURGH = new LatLngBounds(
            new LatLng(55.942617, -3.192473), new LatLng(55.946233, -3.184319)
    );

    private static final LatLng EDINBURGH_POSITION = new LatLng(55.944425, -3.1884);

    private HashMap<String, ArrayList<String>> lyrics;
    private ArrayList<Placemark> placemarks;
    private String currentDiff;
    private String songNumber;
    //BitmapDescriptors for the icons
    private BitmapDescriptor IC_UNCLASSIFIED;
    private BitmapDescriptor IC_BORING;
    private BitmapDescriptor IC_NOTBORING;
    private BitmapDescriptor IC_INTERESTING;
    private BitmapDescriptor IC_VERYINTERESTING;

    private LocationManager lm;


    // Code used in requesting runtime permissions
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    //Constant used in the location settings dialog
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    //Desired interval for location updates. Inexact. Updates may be more or less frequent
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    //Fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS/2;
    //Keys for storing activity state in the Bundle
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    //Provides access to the Fused Location Provider API.
    private FusedLocationProviderClient mFusedLocationClient;
    //Provides access to the Location Settings API
    private SettingsClient mSettingsClient;

    // Stores parameters for requests to the FusedLocationProviderApi.
    private LocationRequest mLocationRequest;

    // Stores the types of location services the client is interested in using. Used for checking
    // settings to determine if the device has optimal location settings.
    private LocationSettingsRequest mLocationSettingsRequest;

    //Callback for Location events
    private LocationCallback mLocationCallback;

    private Location mLastLocation;

    private Boolean mRequestingLocationUpdates;

    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        MapsInitializer.initialize(getActivity().getApplicationContext());
        activity = getActivity();
        sharedPreference = new SharedPreference(getActivity().getApplicationContext());
        currentDiff = sharedPreference.getCurrentDifficultyLevelNumber();
        songNumber = sharedPreference.getCurrentSongNumber();
        lyrics = sharedPreference.getLyrics(songNumber);
        lm  = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        /*
        //Used for debugging
        Log.e(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        for (String key : lyrics.keySet()){
            String value = lyrics.get(key).toString();
            Log.v(TAG, key + " " + value);
        }
        Log.e(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        */

        placemarks = sharedPreference.getMap(currentDiff);

        mRequestingLocationUpdates = false;

        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(
                getActivity().getApplicationContext());

        mSettingsClient = LocationServices.getSettingsClient(getContext());

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        IC_BORING = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_boring);
        IC_UNCLASSIFIED = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_unclassified);
        IC_NOTBORING  = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_notboring);
        IC_INTERESTING = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_interesting);
        IC_VERYINTERESTING = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_veryinteresting);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.v(TAG, "onCreateView called");
        View rootView = inflater.inflate(R.layout.fragment_maps, container, false);
        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); //needed to get the map to display immediately
/*
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e){
            e.printStackTrace();
        }*/
        return rootView;
    }



    @Override
    public void onMapReady(GoogleMap googleMap){
        Log.d(TAG, "onMapReady called");
        mMap = googleMap;

        //Remove any old markers.
        mMap.clear();

        //For showing a move to my location button
        try{
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e){
            Log.e(TAG, "Error" + e);
            e.printStackTrace();
        }
        // Set view to bounds and move camera there
        mMap.setLatLngBoundsForCameraTarget(UNIVERSITY_EDINBURGH);
        mMap.setMinZoomPreference(15.0f);
        if (getLatLngFromLastLocation() != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(getLatLngFromLastLocation(), 17.0f));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(EDINBURGH_POSITION, 17.0f));
        }

        addMarkers();
    }

    /**
     * Adds all the markers on to the map.
     */
    private void addMarkers(){
        Log.v(TAG, "addMarkers called");
        int n = placemarks.size();
        Log.v(TAG, "n = " + n);
        for (int i = 0; i < n; i++){
            Placemark placemark = placemarks.get(i);
            String key = placemark.getKey();
            ArrayList<String> lyric = lyrics.get(key);
            if (lyric == null){
                Log.e(TAG, "null lyric  for key: " + key);
                continue;
            }
            // If that lyric is unfound
            if (checkLyric(lyric)){
                Marker marker;
                String word = lyric.get(0);
                LatLng location = placemark.getLocation();
                String description = placemark.getDescription();
                BitmapDescriptor bitmap;

                switch (description) {
                    case "unclassified":
                        bitmap = IC_UNCLASSIFIED;
                        break;
                    case "boring":
                        bitmap = IC_BORING;
                        break;
                    case "notboring":
                        bitmap = IC_NOTBORING;
                        break;
                    case "interesting":
                        bitmap = IC_INTERESTING;
                        break;
                    case "veryinteresting":
                        bitmap = IC_VERYINTERESTING;
                        break;
                    default:
                        bitmap = IC_UNCLASSIFIED;
                        break;
                }
                marker = mMap.addMarker(new MarkerOptions().position(location).icon(bitmap).
                        title("Word type: " + description));
                ArrayList<String> tag = new ArrayList<>(Arrays.asList(word, key));
                marker.setTag(tag);

            }
        }
        //set a listener for marker clicks.
        mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        // Only allow click if location checking is on
        if (mRequestingLocationUpdates) {
            //may change this if necessary
            double requiredDistance = 50;
            //find distance between last location and location of marker
            LatLng mLastLatLong = getLatLngFromLastLocation();
            double distance = SphericalUtil.computeDistanceBetween(mLastLatLong, marker.getPosition());
            //if you are close enough to the marker
            if (distance < requiredDistance) {

                Object obj = marker.getTag();
                if (obj == null) {
                    Log.e(TAG, "Clicked on null marker");
                    return false;
                }
                ArrayList<String> lyric = (ArrayList<String>) obj;
                String word = lyric.get(0);
                String key = lyric.get(1);
                Toast.makeText(getContext(), "Found word: " + word,
                        Toast.LENGTH_SHORT).show();
                //update lyrics to show that word is found.
                ArrayList<String> newLyric = new ArrayList<>(Arrays.asList(word, "True"));
                lyrics.put(key, newLyric);
                sharedPreference.updateLyrics(lyrics, songNumber);
                sharedPreference.incrementNumberWordsFound();
                marker.remove();
                return true;

            } else {
                //too far away from marker, show a Toast but nothing more.
                Toast.makeText(getContext(), "Too far from marker", Toast.LENGTH_SHORT).show();
                // Return false to indicate that we have not consumed the event and that we wish
                // for the default behavior to occur (which is for the camera to move such that the
                // marker is centered and for the marker's info window to open, if it has one).
                return false;
            }
        } else {
            Toast.makeText(getContext(), "Location not enabled", Toast.LENGTH_SHORT);
            return false;
        }

    }

    /**
     * Checks if a given key appears in the lyrics file.
     * Returns true if it is there AND the lyric isn't marked as True yet
     * @param lyric - ArrayList<String> of lyric to check
     * @return true if there is an unfound lyric with that key, false otherwise
     */
    private boolean checkLyric(ArrayList<String> lyric) {
        return lyric != null && !Boolean.valueOf(lyric.get(1));

    }

    private LatLng getLatLngFromLastLocation(){
        if (mLastLocation != null){
            double mLastLat = mLastLocation.getLatitude();
            double mLastLong = mLastLocation.getLongitude();
            return new LatLng(mLastLat, mLastLong);
        } else {
            return null;
        }
    }




    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mLastLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
        }
    }


    /**
     * Sets up the location request.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mLastLocation = locationResult.getLastLocation();
            }
        };
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }





    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        Log.d(TAG, "location updates started");
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(getActivity(), new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        try {
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                    mLocationCallback, Looper.myLooper());
                        } catch (SecurityException e){
                            Log.e(TAG, "Permission not granted");
                        }

                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }
                    }
                });
    }


    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;

                    }
                });
    }

    @Override
    public void onResume(){
        super.onResume();
        mMapView.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
        mMapView.getMapAsync(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        mMapView.onPause();
        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mLastLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(getView(),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private boolean checkPermissions(){
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.msg_location_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.msg_location_permission_denied,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

}
