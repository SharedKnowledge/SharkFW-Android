package net.sharksystem.android.protocols.wifidirect;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.system.L;
import net.sharksystem.android.peer.AndroidSharkEngine;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by j4rvis on 12.04.16.
 */
public class WifiDirectService extends Service {

    private static WifiManager _wifiManager;

    // Shark things
    private AndroidSharkEngine _engine;
    private AndroidKP _kp;

    @Override
    public void onCreate() {
        _wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(_wifiManager.isWifiEnabled())
            _wifiManager.setWifiEnabled(false);
        _wifiManager.setWifiEnabled(true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        _engine = new AndroidSharkEngine(this);
        _kp = new AndroidKP(_engine);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.d("Service started.", this);

        try {
            _engine.startWifiDirect();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

        try {
            _engine.stopWifiDirect();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        }

        L.d("Service destroyed.");
    }

    public static boolean hotspotIsEnabled(Context context) {
        try {
            _wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = _wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);

            return (Boolean) method.invoke(_wifiManager, (Object[]) null);
        } catch (Exception ex) {
            ex.printStackTrace();
            L.d("Failed to check tethering state, or it is not enabled.");
        }

        return false;
    }
}
