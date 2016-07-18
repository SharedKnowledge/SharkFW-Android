package net.sharksystem.android.peer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    public static final String SHARK_KEY = "net.sharksystem.android";
    public static final String IS_ROUTING_ENABLED_KEY = SHARK_KEY + ".isRoutingEnabled";
    public static final String IS_ENGINE_RUNNING_KEY = SHARK_KEY + ".isEngineRunning";

    private IBinder _binder = new LocalBinder();
    private boolean mIsEngingeStarted = false;

    private AndroidSharkEngine mEngine;
    private RouterKP mRouterKP;
    private ArrayList<KnowledgePort> mKnowledgePorts;

    private String mNameToOffer;
    private String mInterestToOffer;

    private ArrayList<KPListener> mListeners;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        mEngine = new AndroidSharkEngine(this);
        mKnowledgePorts = new ArrayList<>();
        mListeners = new ArrayList<>();

        mPrefs = getSharedPreferences(SHARK_KEY, Context.MODE_PRIVATE);
        mRouterKP = new RouterKP(mEngine, this);

        Log.e("SERVICE", "Service created");

//        testing();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.e("SERVICE", "Service started");
        } else {
            Log.e("SERVICE", "Service RE-started");
            if (mPrefs.getBoolean(IS_ENGINE_RUNNING_KEY, false)) {
                startEngine();
            }
            if (mPrefs.getBoolean(IS_ROUTING_ENABLED_KEY, false)) {
                startRouting();
            }
        }

        return START_STICKY;
    }

    public void startRouting() {
        Log.e("ROUTING", "Routing started");

        mRouterKP.startRouting();

        mPrefs.edit().putBoolean(IS_ROUTING_ENABLED_KEY, true).apply();
    }

    public void stopRouting() {
        Log.e("ROUTING", "Routing stopped");

        mRouterKP.stopRouting();

        mPrefs.edit().putBoolean(IS_ROUTING_ENABLED_KEY, false).apply();
    }

    public void startEngine() {
        L.d("Starting", this);
        if (!mIsEngingeStarted) {
            if (mKnowledgePorts.isEmpty()) {
                RadarKP radarKP = new RadarKP(mEngine, InMemoSharkKB.createInMemoASIPInterest());
                radarKP.addNotifier(this);
                radarKP.addNotifier(mEngine);
                addKP(radarKP);
            }

            try {
                mEngine.offerInterest(mInterestToOffer, mNameToOffer);
                mEngine.startWifiDirect();
            } catch (IOException | SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            mIsEngingeStarted = true;

            mPrefs.edit().putBoolean(IS_ENGINE_RUNNING_KEY, true).apply();
        }
    }

    public void stopEngine() {
        if (mIsEngingeStarted) {
            try {
                L.d("Stop Wifi", this);
                mEngine.stopWifiDirect();
            } catch (SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            mIsEngingeStarted = false;

            mPrefs.edit().putBoolean(IS_ENGINE_RUNNING_KEY, false).apply();

        }
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

    public void addKPListener(KPListener listener) {
        mListeners.add(listener);
    }

    public void removeKPListener(KPListener listener) {
        mListeners.remove(listener);
    }

    public void addKP(KnowledgePort kp) {
        if (!mKnowledgePorts.contains(kp))
            mKnowledgePorts.add(kp);
    }

    public void sendBroadcast(String text) {
        mEngine.sendBroadcast(text);
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
