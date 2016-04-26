package net.sharksystem.android.protocols.wifidirect;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
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

    public class LocalBinder extends Binder {

        public WifiDirectService getInstance() {
            return WifiDirectService.this;
        }
    }
    private static WifiManager _wifiManager;

    private IBinder _binder = new LocalBinder();
    private boolean _started = false;
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
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        _engine = new AndroidSharkEngine(this);
        _kp = new AndroidKP(_engine);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            // Ensure that startWifiDirect() get only called one time.
            if(!_started)
                _started=true;
            if(_started){
                L.d("Service started.", this);
                _engine.startWifiDirect();
            }
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
        return _binder;
    }

    public void stop(){
        try {
            _engine.stopWifiDirect();
            stopSelf();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        _engine.disconnect();
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

    public void connect(WifiDirectPeer peer){
        _engine.connect(peer);
    }
}
