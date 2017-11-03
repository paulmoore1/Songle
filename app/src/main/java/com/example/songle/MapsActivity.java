package com.example.songle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private Location mLastLocation;
    private static final String TAG="MapsActivity";

    private LatLngBounds UNIVERSITYEDINBURGH = new LatLngBounds(
            new LatLng(55.942617, -3.192473), new LatLng(55.946233, -3.184319)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }
    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            //Visualise current position with a small blue circle
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException se){
            System.out.println("Security exception thrown [onMapReady]");
        }
        // Add "My location" button to the user interface
        mMap.getUiSettings().setMyLocationButtonEnabled(true);


    }

    protected void createLocationRequest(){
        //Set the parameters for the location request
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); // preferably every 5 seconds
        mLocationRequest.setFastestInterval(1000); //at most every second
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Can we access the users current location?
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);
        }
    }

    @Override
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
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {createLocationRequest();}
        catch (java.lang.IllegalStateException ise){
            System.out.println("IllegalStateException thrown [onConnected]");
        }
        // Can we access the users's current location?
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED){
            mLastLocation =
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println(" >>>>onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println(" >>>>onconnectionFailed");
    }

    public void addMarkers(ArrayList<ArrayList<String>> lyrics,
                                          ArrayList<ArrayList<Boolean>> bools, ArrayList<Placemark> placemarks){
        int n = placemarks.size();
        ArrayList<Marker> markers = new ArrayList<Marker>(n);
        for (int i = 0; i < n; i++){
            Placemark placemark = placemarks.get(i);
            int line = placemark.getLine();
            int word = placemark.getWord();
            //check if the word has been found or not
            boolean check = bools.get(line).get(word);
            // word not found yet so create Marker
            if (!check){

                String lyric = lyrics.get(line).get(word);
                LatLng location = placemark.getLocation();
                String description = placemark.getDescription();
                BitmapDescriptor bitmap;

                if (description.equals("unclassified")){
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_unclassified);
                } else if (description.equals("boring")){
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_boring);
                } else if (description.equals("notboring")){
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_notboring);
                } else if (description.equals("interesting")){
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_interesting);
                } else if (description.equals("veryinteresting")){
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_veryinteresting);
                } else {
                    bitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_unclassified);
                }

                Marker marker = mMap.addMarker(new MarkerOptions().position(location).icon(bitmap));
                marker.setTag(lyric);

            }

        }
        //set a listener for marker clicks.
        mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
        return;

    }

    /**
     * Called when a user clicks a marker
     * @param marker
     * @return
     */

    public boolean onMarkerClick(final Marker marker){
        //may change this if necessary
        double requiredDistance = 10;
        //find distance between last location and location of marker
        double mLastLat = mLastLocation.getLatitude();
        double mLastLong = mLastLocation.getLongitude();
        LatLng mLastLatLong = new LatLng(mLastLat, mLastLong);
        double distance = SphericalUtil.computeDistanceBetween(mLastLatLong, marker.getPosition());
        //if you are close enough to the marker
        if (distance < requiredDistance){
            Toast.makeText(this, "Found word: " + marker.getTag().toString(),
                    Toast.LENGTH_SHORT).show();
            //delete the marker
            marker.remove();
            //TODO update bools and main lyrics display

        } else {
            //too far away from marker, show a Toast but nothing more.
            Toast.makeText(this, "Too far from marker", Toast.LENGTH_SHORT).show();
        }
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }
}