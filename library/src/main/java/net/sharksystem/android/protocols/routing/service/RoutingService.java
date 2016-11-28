package net.sharksystem.android.protocols.routing.service;

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
import net.sharksystem.android.peer.AndroidSharkEngine;
import net.sharksystem.android.protocols.routing.RouterKP;
import net.sharksystem.android.protocols.routing.TimeUnit;

import java.io.IOException;

public class RoutingService extends Service {
    public class LocalBinder extends Binder {
        public RoutingService getInstance() {
            return RoutingService.this;
        }
    }

    public static final String KEY_SHARK = "net.sharksystem.android";
    public static final String KEY_IS_ROUTING_ENABLED = KEY_SHARK + ".isRoutingEnabled";

    private IBinder _binder = new LocalBinder();
    private boolean mRoutingStarted = false;

    private AndroidSharkEngine mEngine;
    private RouterKP mRouterKP;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        mEngine = new AndroidSharkEngine(this);

        mPrefs = getSharedPreferences(KEY_SHARK, Context.MODE_PRIVATE);
        mRouterKP = new RouterKP(mEngine, this);

        Log.e("SERVICE", "Service created");
//        testing();
    }

    // TODO Start service when android starts
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("SERVICE", "Service started");

        boolean startRouting = mPrefs.getBoolean(KEY_IS_ROUTING_ENABLED, false);
        if (startRouting) {
            Log.e("SERVICE", "Starting Routing");
        } else {
            Log.e("SERVICE", "Notn Starting routing");
        }

        if (mPrefs.getBoolean(KEY_IS_ROUTING_ENABLED, false)) {
            startRouting();
        }

        return START_STICKY;
    }

    // TODO offerInterest really necessary? Exception otherwise
    public void startRouting() {
        if (!mRoutingStarted) {
            try {
                mEngine.offerInterest("Interest", "Name");
                mEngine.startWifiDirect();
                mEngine.startTCP(7072);
                mRouterKP.startRouting();
            } catch (IOException | SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            mRoutingStarted = true;

            mPrefs.edit().putBoolean(KEY_IS_ROUTING_ENABLED, true).apply();
        }
    }

    public void stopRouting() {
        if (mRoutingStarted) {
            try {
                mEngine.stopWifiDirect();
                mEngine.stopTCP();
                mRouterKP.stopRouting();
            } catch (SharkProtocolNotSupportedException e) {
                e.printStackTrace();
            }
            mRoutingStarted = false;

            mPrefs.edit().putBoolean(KEY_IS_ROUTING_ENABLED, false).apply();
        }
    }

    @Override
    public void onDestroy() {
        Log.e("SERVICE", "Service destroyed");

        stopRouting();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    public void sendBroadcast(String text) {
        mEngine.sendBroadcast(text);
    }

    public STSet getTopicsToRoute() {
        return mRouterKP.getTopicsToRoute();
    }

    public void setTopicsToRoute(STSet topics) {
        mRouterKP.setTopicsToRoute(topics);
    }

    public boolean getRouteAnyTopics() {
        return mRouterKP.getRouteAnyTopics();
    }

    public void setRouteAnyTopics(boolean routeAnyTopics) {
        mRouterKP.setRouteAnyTopics(routeAnyTopics);
    }

    public int getMaxCopies() {
        return mRouterKP.getMaxCopies();
    }

    public void setMaxCopies(int maxCopies) {
        mRouterKP.setMaxCopies(maxCopies);
    }

    public long getMessageTtl() {
        return mRouterKP.getMessageTtl();
    }

    public void setMessageTtl(long messageTtl) {
        mRouterKP.setMessageTtl(messageTtl);
    }

    public TimeUnit getMessageTtlUnit() {
        return mRouterKP.getMessageTtlUnit();
    }

    public void setMessageTtlUnit(TimeUnit unit) {
        mRouterKP.setMessageTtlUnit(unit);
    }

    public int getMessageCheckInterval() {
        return mRouterKP.getMessageCheckInterval();
    }

    public void setMessageCheckInterval(int messageCheckInterval) {
        mRouterKP.setMessageCheckInterval(messageCheckInterval);
    }

    public int getMaxMessages() {
        return mRouterKP.getMaxMessages();
    }

    public void setMaxMessages(int maxMessages) {
        mRouterKP.setMaxMessages(maxMessages);
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
