package net.sharksystem.android.protocols.routing.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import net.sharksystem.android.peer.AndroidSharkEngine;
import net.sharksystem.android.protocols.routing.RouterKP;

public class LocationService extends Service
{
    private LocationManager mLocationManager = null;

    private static final String TAG = "LOCATION";

    private static final int LOCATION_INTERVAL = 1 * 60 * 1000; //One minute
    private static final float LOCATION_DISTANCE = 0;

    private RouterKP mRouterKP;

    LocationListener[] mLocationListeners = new MovingRouterLocationListener[] {
            new MovingRouterLocationListener(LocationManager.GPS_PROVIDER, this),
            new MovingRouterLocationListener(LocationManager.NETWORK_PROVIDER, this)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        mRouterKP = new RouterKP(new AndroidSharkEngine(this), this);
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[1]);
        } catch (SecurityException ex) {
            this.requestPermissions();
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
        } catch (SecurityException ex) {
            this.requestPermissions();
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void requestPermissions() {
        Toast t = Toast.makeText(this, "please turn on positioning stuff", Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(settingsIntent);
    }
}
