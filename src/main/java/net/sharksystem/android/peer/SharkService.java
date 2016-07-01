package net.sharksystem.android.peer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
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
import net.sharksystem.android.protocols.routing.LocationReceiver;
import net.sharksystem.android.protocols.routing.MovingRouterLocationListener;
import net.sharksystem.android.protocols.routing.RouterKP;
import net.sharksystem.android.protocols.wifidirect.RadarKP;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by j4rvis on 12.04.16.
 */
public class SharkService extends Service implements KPNotifier {

    public class LocalBinder extends Binder {
        public SharkService getInstance() {
            return SharkService.this;
        }
    }

    private final String sharkKey = "net.sharksystem.android";
    private final String isRoutingEnabledKey = sharkKey + ".isRoutingEnabled";
    private final String isEngineRunningKey = sharkKey + ".isEngineRunning";

    private IBinder _binder = new LocalBinder();
    private boolean _isEngineStarted = false;

    private AndroidSharkEngine _engine;
    private RouterKP mRouterKP;
    private ArrayList<KnowledgePort> _knowledgePorts;

    private String mNameToOffer;
    private String mInterestToOffer;

    private ArrayList<KPListener> mListeners;

    private AlarmManager mAlarmManager;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private PendingIntent mLocationIntent;

    private Runnable mRunnable;
    private Handler mHandler;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        _engine = new AndroidSharkEngine(this);
        _knowledgePorts = new ArrayList<>();
        mListeners = new ArrayList<>();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intentToFire = new Intent(this, LocationReceiver.class);
        mLocationIntent = PendingIntent.getBroadcast(this, 0, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT);

//        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
//        mLocationListener = new MovingRouterLocationListener(LocationManager.NETWORK_PROVIDER, this);

        mPrefs = getSharedPreferences(sharkKey, Context.MODE_PRIVATE);

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e("SERVICE", "Checking messages...");
                mHandler.postDelayed(mRunnable, 1000);
            }
        };

        Log.e("SERVICE", "Service created");

//        testing();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.e("SERVICE", "Service started");
        } else {
            Log.e("SERVICE", "Service RE-started");
            if (mPrefs.getBoolean(isEngineRunningKey, false)) {
                startEngine();
            }
            if (mPrefs.getBoolean(isRoutingEnabledKey, false)) {
                startRouting();
            }
        }

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
        Log.e("ROUTING", "Routing started");

        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 2 * 60 * 1000, mLocationIntent);
        mHandler.postDelayed(mRunnable, 2000);

        mPrefs.edit().putBoolean(isRoutingEnabledKey, true).apply();

        //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000, 0, mLocationListener);
    }

    public void stopRouting() {
        Log.e("ROUTING", "Routing stopped");

        mAlarmManager.cancel(mLocationIntent);
        LocationReceiver.stopLocationListener();
        mHandler.removeCallbacks(mRunnable);

        mPrefs.edit().putBoolean(isRoutingEnabledKey, false).apply();


        //mLocationManager.removeUpdates(mLocationListener);
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

            mPrefs.edit().putBoolean(isEngineRunningKey, true).apply();

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

            mPrefs.edit().putBoolean(isEngineRunningKey, false).apply();

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
