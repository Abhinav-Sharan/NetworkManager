package com.abhinav.networkmanager;

/**
 * Created by Abhinav on 24/02/2017.
 */
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashSet;
import java.util.Set;

public class AndroidServiceStartOnBoot extends Service implements
        ConnectionCallbacks, OnConnectionFailedListener,LocationListener{

    protected static final String TAG = "location-updates-sample";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;
    private Location mSettingLocation;
    private SharedPreferences sharedPreferences;
    private boolean homeWifi;
    private boolean homeData;
    private boolean awayWifi;
    private boolean awayData;
    private LocationType locationType = LocationType.NONE;

    private enum LocationType{
        HOME,AWAY,NONE
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    private void updateDataFromPreference() {
        sharedPreferences  = getSharedPreferences(Constants.MyPref, Context.MODE_PRIVATE);
        if(!isSettingAvailable())
            return;
        Set<String> locationSetting = sharedPreferences.getStringSet(Constants.LocationKey,new HashSet<String>());
        mSettingLocation = new Location("SettingLocation");
        mSettingLocation.setLatitude(Double.parseDouble(locationSetting.toArray()[0].toString()));
        mSettingLocation.setLongitude(Double.parseDouble(locationSetting.toArray()[1].toString()));

        homeWifi = sharedPreferences.getBoolean(Constants.homeWifiKey,true);
        homeData = sharedPreferences.getBoolean(Constants.homeWifiKey,false);
        awayWifi = sharedPreferences.getBoolean(Constants.awayWifiKey,false);
        awayData = sharedPreferences.getBoolean(Constants.awayDataKey,true);
    }

    private boolean isSettingAvailable() {
        Set<String> locationSetting = sharedPreferences.getStringSet(Constants.LocationKey,new HashSet<String>());
        if(locationSetting.toArray().length < 1) return false;
        return true;
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        updateDataFromPreference();
        mGoogleApiClient.connect();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        if(mGoogleApiClient.isConnected()){
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        boolean isHome = mSettingLocation.distanceTo(mCurrentLocation) < 28;
        if(isHome){
            locationType = LocationType.HOME;
            setWifi(homeWifi);
            setData(homeData);
        }
        if(sharedPreferences.contains(Constants.shouldSetWiFiWhenAway) && sharedPreferences.getBoolean(Constants.shouldSetWiFiWhenAway,false))
        {
            locationType = LocationType.AWAY;
            setWifi(awayWifi);
            setData(awayData);
        }
    }
    public void setWifi(boolean wifiState){
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(wifiState);
    }

    public void setData(boolean dataState) {
        //the new API levels this support has gone.I will try to find some way to implement this
    }

}
