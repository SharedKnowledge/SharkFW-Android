package net.sharksystem.android.protocols.wifidirect;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;

/**
 * Created by j4rvis on 26.04.16.
 */
public class WifiDirectServiceController
        extends BroadcastReceiver
        implements WifiP2pManager.ConnectionInfoListener, ServiceConnection {

    @Override
    public void onReceive(Context context, Intent intent) {

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
