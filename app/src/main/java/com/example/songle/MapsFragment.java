package com.example.songle;

import android.Manifest;
import android.app.Activity;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.google.maps.android.SphericalUtil;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Paul on 21/11/2017.
 * From https://stackoverflow.com/questions/19353255/how-to-put-google-maps-v2-on-a-fragment-using-viewpager
 */

public class MapsFragment extends Fragment implements GoogleMap.OnMarkerClickListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = "MapsFragment";

    private MapView mMapView;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private Activity activity;
    private SharedPreference sharedPreference;
    private static final LatLngBounds UNIVERSITY_EDINBURGH = new LatLngBounds(
            new LatLng(55.942617, -3.192473), new LatLng(55.946233, -3.184319)
    );

    private static final CameraPosition EDINBURGH_CAMERA = new CameraPosition.Builder()
            .target(new LatLng(55.944425, -3.1884)).zoom(4.0f).bearing(0).tilt(0).build();

    private HashMap<String, ArrayList<String>> lyrics;
    private ArrayList<Placemark> placemarks;
    private String currentDiff;
    private String songNumber;
    private BitmapDescriptor IC_UNCLASSIFIED;
    private BitmapDescriptor IC_BORING;
    private BitmapDescriptor IC_NOTBORING;
    private BitmapDescriptor IC_INTERESTING;
    private BitmapDescriptor IC_VERYINTERESTING;
    private FusedLocationProviderClient mFusedLocationClient;


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
        placemarks = sharedPreference.getMap(currentDiff);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity().getApplicationContext());

        this.IC_BORING = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_boring);
        this.IC_UNCLASSIFIED = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_unclassified);
        this.IC_NOTBORING  = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_notboring);
        this.IC_INTERESTING = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_interesting);
        this.IC_VERYINTERESTING = BitmapDescriptorFactory.fromResource(
                R.drawable.marker_veryinteresting);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_maps, container, false);
        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); //needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e){
            e.printStackTrace();
        }
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                MapsFragment.this.mMap = mMap;
                //For showing a move to my location button
                try{
                    MapsFragment.this.mMap.setMyLocationEnabled(true);

                } catch (SecurityException e){
                    Log.e(TAG, "Error" + e);
                    e.printStackTrace();
                }

            }
        });

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap){
        // Set view to bounds and move camera there
        googleMap.setLatLngBoundsForCameraTarget(UNIVERSITY_EDINBURGH);
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(EDINBURGH_CAMERA));
        addMarkers();
    }

    /**
     * Adds all the markers on to the map.
     */
    private void addMarkers(){
        int n = placemarks.size();
        for (int i = 0; i < n; i++){
            Placemark placemark = placemarks.get(i);
            String key = placemark.getKey();

            ArrayList<String> lyric = lyrics.get(key);
            // If that lyric is unfound
            if (checkLyric(lyric)){
                String tag = lyric.get(0);
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
                Marker marker = mMap.addMarker(new MarkerOptions().position(location).icon(bitmap));
                marker.setTag(new ArrayList<>(Arrays.asList(tag, key)));
            }
        }
        //set a listener for marker clicks.
        mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        //may change this if necessary
        double requiredDistance = 10;
        //find distance between last location and location of marker
        double mLastLat = mLastLocation.getLatitude();
        double mLastLong = mLastLocation.getLongitude();
        LatLng mLastLatLong = new LatLng(mLastLat, mLastLong);
        double distance = SphericalUtil.computeDistanceBetween(mLastLatLong, marker.getPosition());
        //if you are close enough to the marker
        if (distance < requiredDistance){
            ArrayList<String> lyric = (ArrayList<String>) marker.getTag();
            String word = lyric.get(0);
            String key = lyric.get(1);
            Toast.makeText(getContext(), "Found word: " + word + "!",
                    Toast.LENGTH_SHORT).show();
            //delete the marker
            marker.remove();
            //update lyrics to show that word is found.
            ArrayList<String> newLyric = new ArrayList<>(Arrays.asList(word, "True"));
            lyrics.put(key, newLyric);
            sharedPreference.updateLyrics(lyrics, songNumber);
            sharedPreference.incrementNumberWordsFound(songNumber);

        } else {
            //too far away from marker, show a Toast but nothing more.
            Toast.makeText(getContext(), "Too far from marker", Toast.LENGTH_SHORT).show();
        }
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
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


    private void createLocationRequest(){
        //Set the parameters for the location request
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // preferably every 5 seconds
        mLocationRequest.setFastestInterval(1000); //at most every second
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
/*
        //Can we access the users current location?
        int permissionCheck = ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);
        }
        */
    }


    public void onLocationChanged(Location current) {
        System.out.println(
                "[onLocationChanged] Lat/long now (" +
                        String.valueOf(current.getLatitude()) + "," +
                        String.valueOf(current.getLongitude()) + ")"
        );
        mLastLocation = current;
        //Do something with current location
    }

    @Override
    public void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
    }


    public void onStatusChanged(String s, int i, Bundle bundle) {

    }


    public void onProviderEnabled(String s) {

    }


    public void onProviderDisabled(String s) {

    }


    public void onConnected(Bundle connectionHint) {
        try {createLocationRequest();}
        catch (java.lang.IllegalStateException ise){
            System.out.println("IllegalStateException thrown [onConnected]");
        }
        // Can we access the users's current location?
        if (ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            mLastLocation =
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    public void onConnectionSuspended(int i) {
        System.out.println(" >>>>onConnectionSuspended");
    }


    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println(" >>>>onconnectionFailed");
    }

    @Override
    public void onResume(){
        super.onResume();
        mMapView.onResume();
        //unnecessary?
        //lyrics = sharedPreference.getLyrics(songNumber);
    }

    @Override
    public void onPause(){
        super.onPause();
        mMapView.onPause();
        // Save any changes to the lyrics.
        sharedPreference.saveNewLyrics(lyrics, songNumber);
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

}
