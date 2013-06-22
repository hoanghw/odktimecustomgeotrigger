package org.odk.collect.android.triggers;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationService extends Service implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener  {

    private static final String TAG = "LOCATIONSERVICE";
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    private PowerManager.WakeLock wakeLock;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

        mLocationClient.connect();
        //PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService");
        //wakeLock.acquire();
    }
    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onConnectionFailed");
        mLocationClient.connect();
    }

    @Override
    public void onConnected(Bundle arg0) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onConnected");
        Intent intent = new Intent(this,LocationUpdatesIntentService.class);
        PendingIntent pIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mLocationClient.requestLocationUpdates(mLocationRequest, pIntent);

        //mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDisconnected");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", location.toString());

    }

    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        // If the client is connected
        if (mLocationClient.isConnected()) {
            Intent intent = new Intent(this,LocationUpdatesIntentService.class);
            PendingIntent pIntent = PendingIntent.getService(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mLocationClient.removeLocationUpdates(pIntent);

            //mLocationClient.removeLocationUpdates(this);
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        //wakeLock.release();
        super.onDestroy();
    }
}