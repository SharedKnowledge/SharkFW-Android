package net.sharksystem.android.peer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.peer.SharkEngine;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.wifidirect.AndroidKP;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;

import java.io.IOException;

/**
 * Created by j4rvis on 12.04.16.
 */
public class SharkService extends Service {

    public class LocalBinder extends Binder {

        public SharkService getInstance() {
            return SharkService.this;
        }

    }
    private static WifiManager _wifiManager;
    private IBinder _binder = new LocalBinder();
    private boolean _isStarted = false;

    // Shark things
    private boolean _isEngineStarted = false;
    private AndroidSharkEngine _engine;
    private KnowledgePort _kp;

    @Override
    public void onCreate() {
//        testing();
    }

    public void setEngine(AndroidSharkEngine engine){
        _engine = engine;
    }

    public void setKP(KnowledgePort kp){
        _kp = kp;
    }

    public void startEngine(){
        L.d("Starting", this);
        if(!_isEngineStarted){
            if(_engine==null)
                _engine = new AndroidSharkEngine(this);
            if(_kp==null)
                _kp = new AndroidKP(_engine);

            try {
                _engine.startWifiDirect();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            _isEngineStarted = true;
        }
    }

    public void stopEngine(){
        if(_isEngineStarted){
            try {
                _engine.stopWifiDirect();
            } catch (SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            _isEngineStarted = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopEngine();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    public void disconnect() {
        _engine.disconnect();
    }

    public void connect(WifiDirectPeer peer){
        _engine.connect(peer);
    }

    public void sendMessage(String text) {
        _engine.sendWifiMessage(text);
    }

    public void testing(){

        _wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(_wifiManager.isWifiEnabled())
            _wifiManager.setWifiEnabled(false);
        _wifiManager.setWifiEnabled(true);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
