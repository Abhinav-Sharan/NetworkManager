package com.abhinav.networkmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener,LocationListener{

    protected static final String TAG = "MainActivity";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    SharedPreferences sharedpreferences;
    GoogleApiClient mGoogleApiClient;

    protected Location mLastLocation;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedpreferences = getSharedPreferences(Constants.MyPref, Context.MODE_PRIVATE);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();

    }

    public void startService(View view){
        if(mGoogleApiClient.isConnected()){
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        try {
            Intent intent = new Intent(this,AndroidServiceStartOnBoot.class);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(Constants.shouldServiceStartOnBoot, true);
            editor.commit();
            startService(intent);
            Toast.makeText(getApplicationContext(),"Service started",Toast.LENGTH_LONG);
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG);
        }

    }
    public void stopService(View view){
        try{
            Intent intent = new Intent(this,AndroidServiceStartOnBoot.class);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(Constants.shouldServiceStartOnBoot, false);
            editor.commit();
            stopService(intent);
            Toast.makeText(getApplicationContext(),"Service stopped",Toast.LENGTH_LONG);
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG);
        }

    }

    public void setLastKnownLocation(View view){
        SharedPreferences.Editor editor = sharedpreferences.edit();
        Set<String> h = new HashSet<String>(Arrays.asList(Double.toString(mLastLocation.getLatitude()), Double.toString(mLastLocation.getLongitude())));
        editor.putStringSet(Constants.LocationKey, h);
        editor.commit();
        updateUIFromPreference();
    }


    public void setCurrentSetting(View view){
        Switch switchHomeWifi = (Switch) findViewById(R.id.switch1);
        //Switch switchHomeData = (Switch) findViewById(R.id.switch3);
        Switch switchAwayWifi = (Switch) findViewById(R.id.switch2);
        //Switch switchAwayData = (Switch) findViewById(R.id.switch5);

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(Constants.homeWifiKey, switchHomeWifi.isChecked());
        //editor.putBoolean(Constants.homeDataKey, switchHomeData.isChecked());
        //editor.putBoolean(Constants.awayDataKey, switchAwayData.isChecked());
        editor.putBoolean(Constants.awayWifiKey, switchAwayWifi.isChecked());
        editor.commit();
        Toast.makeText(MainActivity.this,"Thanks!You have updated your settings",Toast.LENGTH_LONG).show();
    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
        updateUIFromPreference();
        updateUI();
    }
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
            mLastLocation  = location;
            updateUIFromPreference();
            updateUI();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    private String getAddress(Location location){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<android.location.Address> addresses = null;
        String errorMessage = "";
        StringBuilder stringBuilder = new StringBuilder();

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, get just a single address.
                    1);

        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "Service not available";
            Log.e(TAG, errorMessage, ioException);
            stringBuilder.append("Connection not available");
            return stringBuilder.toString();

        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "invalid lattitude and longitude used";
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        if(addresses == null || addresses.toArray().length < 1) {
            stringBuilder.append("Your current location is not set");
            return stringBuilder.toString();

        }
        for (int i=0;i<addresses.get(0).getMaxAddressLineIndex();i++ ){
            stringBuilder.append(addresses.get(0).getAddressLine(i));
        }
        return stringBuilder.toString();
    }

    private void updateUI(){
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Your last location is : "+ getAddress(mLastLocation));
    }
    private void updateUIFromPreference(){
        if(!sharedpreferences.contains(Constants.LocationKey)) return;;
        Set<String> h = sharedpreferences.getStringSet(Constants.LocationKey,new HashSet<String>());
        if(h.toArray().length < 1) return;
        Location location = new Location("Location");
        int index = 0;
        for (Iterator<String> it = h.iterator(); it.hasNext(); ) {
            String f = it.next();
            if (index == 0){
                location.setLatitude(Double.parseDouble(f));
                index++;
            }
            else {
                location.setLongitude(Double.parseDouble(f));
            }
        }
        Button myButton = (Button) findViewById(R.id.button4);
        myButton.setText(getAddress(location));
    }
}
