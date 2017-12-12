package com.example.songle;

import android.Manifest;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssStatus.Callback;
import android.location.GpsStatus;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static android.location.GpsStatus.GPS_EVENT_STARTED;
import static android.location.GpsStatus.GPS_EVENT_STOPPED;

/**
 * Created by Paul on 21/11/2017.
 * From https://stackoverflow.com/questions/19353255/how-to-put-google-maps-v2-on-a-fragment-using-viewpager
 * and https://github.com/googlesamples/android-play-location/blob/master/LocationUpdates/app/src/main/java/com/google/android/gms/location/sample/locationupdates/MainActivity.java
 */

public class MapsFragment extends Fragment implements GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
    private static final String TAG = MapsFragment.class.getSimpleName();
    private SongInfo songInfo;
    private MapView mMapView;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private boolean gpsOn = false;
    private Activity activity;
    private SharedPreference sharedPreference;
    private static final LatLngBounds UNIVERSITY_EDINBURGH = new LatLngBounds(
            new LatLng(55.942617, -3.192473), new LatLng(55.946233, -3.184319)
    );

    private static final LatLng EDINBURGH_POSITION = new LatLng(55.944425, -3.1884);

    private HashMap<String, ArrayList<String>> lyrics;
    private ArrayList<Placemark> placemarks;
    private String mapNumber;
    private String songNumber;
    //BitmapDescriptors for the icons
    private BitmapDescriptor IC_UNCLASSIFIED;
    private BitmapDescriptor IC_BORING;
    private BitmapDescriptor IC_NOTBORING;
    private BitmapDescriptor IC_INTERESTING;
    private BitmapDescriptor IC_VERYINTERESTING;
    private static MediaPlayer markerPop;

    private LocationManager lm;
    private GpsStatus.Listener listener;
    private Callback gnssCallback;





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
        mapNumber = sharedPreference.getCurrentMapNumber();
        songNumber = sharedPreference.getCurrentSongNumber();
        lyrics = sharedPreference.getLyrics(songNumber);
        songInfo = sharedPreference.getSongInfo(songNumber);

        placemarks = sharedPreference.getMap(mapNumber);

        mSettingsClient = LocationServices.getSettingsClient(getContext());

        //createLocationCallback();
        //createLocationRequest();
        //buildLocationSettingsRequest();

        buildGoogleApiClient();

        lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        createGpsStatusListener();

        if (!checkPermissions()){
            Toast.makeText(getActivity(), "Location permissions unexpectedly removed", Toast.LENGTH_SHORT).show();
        }

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

        markerPop = MediaPlayer.create(getContext(), R.raw.marker_pop);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.v(TAG, "onCreateView called");
        View rootView = inflater.inflate(R.layout.maps_tab_fragment, container, false);
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
                marker = mMap.addMarker(new MarkerOptions().position(location).icon(bitmap));
                ArrayList<String> tag = new ArrayList<>(Arrays.asList(word, key));
                marker.setTag(tag);
            }
        }
        //set a listener for marker clicks.
        mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        markerPop.start();
        // Only allow click if location is on and permissions are granted
        if (gpsOn && checkPermissions()) {
            //may change this if necessary
            double requiredDistance = 50;
            //find distance between last location and location of marker
            LatLng mLastLatLong = getLatLngFromLastLocation();
            Log.e(TAG, "current location: " + mLastLatLong);
            if (mLastLatLong != null){
                double distance = SphericalUtil.computeDistanceBetween(mLastLatLong, marker.getPosition());
                Log.e(TAG, "Distance is " + distance);
                //if you are close enough to the marker
                if (distance < requiredDistance) {

                    Object obj = marker.getTag();
                    if (obj == null || ! (obj instanceof ArrayList<?>)) {
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
                    sharedPreference.saveLyrics(songNumber, lyrics);
                    songInfo.incrementNumWordsFound();
                    sharedPreference.saveSongInfo(songNumber, songInfo);
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
                Log.e(TAG, "Last location is null");
                return false;
            }

        } else {
            if (!gpsOn) makeShortToast("GPS not enabled");
            if (!checkPermissions())  makeShortToast("Location permission not granted");
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void setupGpsLocationManagerListener(){
        Log.v(TAG, "Setting up GPS listener");
        try {
            lm.addGpsStatusListener(listener);

            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                sendNoGPSAlertDialog();
            }
        }catch (SecurityException e){
            Log.e(TAG, "Encountered security exception: " + e);
            gpsOn = false;
        }
    }

    private void removeGpsLocationMangerListener(){
        Log.v(TAG, "Removing GPS listener");
        lm.removeGpsStatusListener(listener);
        gpsOn = false;
    }

    private void createGpsStatusListener(){
        Log.v(TAG, "Gps status listener created");
        listener = new android.location.GpsStatus.Listener(){
            public void onGpsStatusChanged(int event) {
                switch (event) {
                    case GPS_EVENT_STARTED:
                        Log.d(TAG, "GPS is switched on");
                        gpsOn = true;
                        break;
                    case GPS_EVENT_STOPPED:
                        Log.e(TAG, "GPS is switched off");
                        gpsOn = false;
                        sendNoGPSAlertDialog();
                        break;
                }
            }
        };
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


    @Override
    public void onResume(){
        super.onResume();
        mMapView.onResume();
        mGoogleApiClient.connect();
        mMapView.getMapAsync(this);
        //avoid any security exceptions
        if(checkPermissions()){
            setupGpsLocationManagerListener();
        }

    }

    @Override
    public void onPause(){
        super.onPause();
        mMapView.onPause();
        mGoogleApiClient.disconnect();
        //avoid any security exceptions
        if (checkPermissions()){
            removeGpsLocationMangerListener();
        }
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


    private void sendNoGPSAlertDialog(){
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
        adb.setTitle(R.string.txt_gps_disabled);
        adb.setMessage(R.string.txt_switch_on_gps);
        adb.setCancelable(false);
        adb.setPositiveButton(R.string.txt_switch_on_gps, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        adb.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = adb.create();
        alertDialog.show();
    }

    private boolean checkPermissions(){
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        createLocationRequest();
        try{
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest,this);
        } catch (SecurityException e){
            Log.e(TAG, "Security exception: " + e);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        switch(i){
            case CAUSE_NETWORK_LOST:

                Log.e(TAG, "Network connection lost");
            case CAUSE_SERVICE_DISCONNECTED:


        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "New location: " + location);
        if (location != null) mLastLocation = location;
    }

    private void makeShortToast(String text){
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
    }
}
