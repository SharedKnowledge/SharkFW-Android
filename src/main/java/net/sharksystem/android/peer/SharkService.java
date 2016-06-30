package net.sharksystem.android.peer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.routing.service.LocationReceiver;
import net.sharksystem.android.protocols.wifidirect.RadarKP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by j4rvis on 12.04.16.
 */
public class SharkService extends Service implements KPNotifier {

    public class LocalBinder extends Binder {
        public SharkService getInstance() {
            return SharkService.this;
        }
    }

    private IBinder _binder = new LocalBinder();
    private boolean _isEngineStarted = false;

    private AndroidSharkEngine _engine;
    private ArrayList<KnowledgePort> _knowledgePorts;

    private String mNameToOffer;
    private String mInterestToOffer;

    private ArrayList<KPListener> mListeners;

    private AlarmManager mAlarmManager;
    public PendingIntent mLocationIntent;


    @Override
    public void onCreate() {
        _engine = new AndroidSharkEngine(this);
        _knowledgePorts = new ArrayList<>();
        mListeners = new ArrayList<>();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intentToFire = new Intent(this, LocationReceiver.class);
        mLocationIntent = PendingIntent.getBroadcast(this, 0, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT);
        Log.e("SERVICE", "Service created");

//        testing();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        Log.e("SERVICE", "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("SERVICE", "Service destroyed");

        stopEngine();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    @Override
    public void notifyInterestReceived(ASIPInterest asipInterest, ASIPConnection asipConnection) {
        if (mListeners.size() > 0) {
            // there are listeners, so notify them
            for (KPListener listener : mListeners) {
                listener.onNewInterest(asipInterest);
            }
        } else {
            // maybe send an android notification?
        }
    }

    @Override
    public void notifyKnowledgeReceived(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {
        if (mListeners.size() > 0) {
            // there are listeners, so notify them
            for (KPListener listener : mListeners) {
                listener.onNewKnowledge(asipKnowledge);
            }
        } else {
            // maybe send an android notification?
        }
    }

    public void startRouting() {
        long interval = 60 * 1000;
        int alarmType = AlarmManager.RTC_WAKEUP;
        long timetoRefresh = Calendar.getInstance().getTimeInMillis();
        mAlarmManager.setInexactRepeating(alarmType, timetoRefresh, interval, mLocationIntent);
    }

    public void stopRouting() {
        mAlarmManager.cancel(mLocationIntent);
        LocationReceiver.stopLocationListener();
    }

    public void addKPListener(KPListener listener) {
        mListeners.add(listener);
    }

    public void removeKPListener(KPListener listener) {
        mListeners.remove(listener);
    }

    public void addKP(KnowledgePort kp) {
        if (!_knowledgePorts.contains(kp))
            _knowledgePorts.add(kp);
    }

    public void startEngine() {
        L.d("Starting", this);
        if (!_isEngineStarted) {
            if (_knowledgePorts.isEmpty())
                addKP(new RadarKP(_engine, InMemoSharkKB.createInMemoASIPInterest(), this));
            try {
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

    public void stopEngine() {
        if (_isEngineStarted) {
            try {
                L.d("Stop Wifi", this);
                _engine.stopWifiDirect();
            } catch (SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            _isEngineStarted = false;
        }
    }

    public void sendBroadcast(String text) {
        _engine.sendBroadcast(text);
    }

    public void setNameToOffer(String mNameToOffer) {
        this.mNameToOffer = mNameToOffer;
    }

    public void setInterestToOffer(String mInterestToOffer) {
        this.mInterestToOffer = mInterestToOffer;
    }

    public void testing() {

        WifiManager _wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (_wifiManager.isWifiEnabled())
            _wifiManager.setWifiEnabled(false);
        _wifiManager.setWifiEnabled(true);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
