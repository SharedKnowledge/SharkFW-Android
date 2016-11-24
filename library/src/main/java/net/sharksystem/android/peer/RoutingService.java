package net.sharksystem.android.peer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.routing.RouterKP;

import java.io.IOException;


public class RoutingService extends Service {
    public class LocalBinder extends Binder {
        public RoutingService getInstance() {
            return RoutingService.this;
        }
    }

    public static final String SHARK_KEY = "net.sharksystem.android";
    public static final String IS_ROUTING_ENABLED_KEY = SHARK_KEY + ".isRoutingEnabled";
    public static final String IS_ENGINE_RUNNING_KEY = SHARK_KEY + ".isEngineRunning";

    private IBinder _binder = new LocalBinder();
    private boolean mIsEngingeStarted = false;

    private AndroidSharkEngine mEngine;
    private RouterKP mRouterKP;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        mEngine = new AndroidSharkEngine(this);

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
            try {
                mEngine.startWifiDirect();
                mEngine.startTCP(7072);
            } catch (IOException | SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            mIsEngingeStarted = true;

            mPrefs.edit().putBoolean(IS_ENGINE_RUNNING_KEY, true).apply();
        }
    }

    public void stopEngine() {
        if (mIsEngingeStarted) {
            L.d("Stop Wifi", this);
            try {
                mEngine.stopWifiDirect();
                mEngine.stopTCP();
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

    public void sendBroadcast(String text) {
        mEngine.sendBroadcast(text);
    }

    public void setTopicsToRoute(STSet topics) {
        mRouterKP.setTopicsToRoute(topics);
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
