package net.sharksystem.android.peer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.wifidirect.RadarKP;
import net.sharksystem.android.protocols.wifidirect.WifiDirectKPNotifier;

import java.io.IOException;
import java.util.ArrayList;

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
    private ArrayList<KnowledgePort> _knowledgePorts;
    private WifiDirectKPNotifier _kpNotifier;

    private String mNameToOffer;
    private String mInterestToOffer;


    @Override
    public void onCreate() {
        _engine = new AndroidSharkEngine(this);
        _knowledgePorts = new ArrayList<>();
        _kpNotifier = new WifiDirectKPNotifier(this);
        L.d("Service created", this);
//        testing();
    }

    public void addKP(KnowledgePort kp){
        if(!_knowledgePorts.contains(kp))
            _knowledgePorts.add(kp);
    }

    public void startEngine(){
        L.d("Starting", this);
        if(!_isEngineStarted){
            if(_knowledgePorts.isEmpty())
                addKP(new RadarKP(_engine, InMemoSharkKB.createInMemoASIPInterest(), _kpNotifier));
            try {
//                _engine.
                _engine.offerInterest(mInterestToOffer, mNameToOffer);
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
                L.d("Stop Wifi", this);
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
        L.d("Service destroyed", this);
        stopEngine();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

//    public void sendBroadcast(String text) {
//        _engine.sendBroadcast(text);
//    }

    public void sendBroadcast(String text){
        _engine.sendBroadcast(text);
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

    public void setNameToOffer(String mNameToOffer) {
        this.mNameToOffer = mNameToOffer;
    }

    public void setInterestToOffer(String mInterestToOffer) {
        this.mInterestToOffer = mInterestToOffer;
    }
}
