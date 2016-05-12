package net.sharksystem.android.protocols.wifidirect;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import net.sharkfw.system.L;

import java.util.ArrayList;

/**
 * Created by j4rvis on 26.04.16.
 */
public class WifiDirectServiceController
        extends BroadcastReceiver
        implements ServiceConnection {

    private ArrayList<WifiDirectPeer> _peers = null;
    private boolean _bound;
    private Intent _intent = null;
    private Context _context = null;
    private static WifiDirectServiceController _instance;
    private WifiDirectService _wifiDirectService;

    public static synchronized WifiDirectServiceController getInstance(Context context) {
        if (_instance == null)
            _instance=new WifiDirectServiceController(context);
        return _instance;
    }

    public WifiDirectServiceController(Context context) {
        _context = context;
        _peers = new ArrayList<>();

        _intent = new Intent(_context, WifiDirectService.class);
        _context.startService(_intent);
        _context.bindService(_intent, this, _context.BIND_AUTO_CREATE);
    }


    public void stopService(){
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(_context).unregisterReceiver(this);
        if(_bound){
            _wifiDirectService.stop();
            _bound=false;
        }
        L.d("Service stopped and unbound: " + _bound, this);
    }

    public void startService(){
        registerReceiver();
        _context.startService(_intent);
        if(!_bound){
            _context.bindService(_intent, this, _context.BIND_AUTO_CREATE);
            _bound=true;
        }
        L.d("Service started and bound: " + _bound, this);
    }

    public void restartService(){
        if(_bound){
            try {
                stopService();
                Thread.sleep(3000);
                startService();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerReceiver(){

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiDirectListener.NEW_PEERS_ACTION);
        intentFilter.addAction(WifiDirectListener.NEW_RECORDS_ACTION);
        intentFilter.addAction(WifiDirectListener.CONNECTION_ESTABLISHED_ACTION);
        LocalBroadcastManager.getInstance(_context).registerReceiver(
                this, intentFilter);
    }

    public void connect(WifiDirectPeer peer){
        _wifiDirectService.connect(peer);
    }

    public void disconnect(){
        _wifiDirectService.disconnect();
    }

    public void sendMessage(String text){
        _wifiDirectService.sendMessage(text);
    }

    public ArrayList<WifiDirectPeer> getPeers(){
        return _peers;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiDirectListener.NEW_PEERS_ACTION.equals(action)){
            _peers = intent.getParcelableArrayListExtra("WifiDirectPeers");
        } else if(WifiDirectListener.NEW_RECORDS_ACTION.equals(action)){
            _peers = intent.getParcelableArrayListExtra("WifiDirectPeers");
        } else if(WifiDirectListener.CONNECTION_ESTABLISHED_ACTION.equals(action)){
            WifiP2pGroup group = intent.getParcelableExtra("WifiP2PGroup");
            WifiP2pInfo info = intent.getParcelableExtra("WifiP2PInfo");
//            L.d(group.toString(), this);
//            L.d(info.toString(), this);
//            _wifiDirectService.sendMessage("RAAAAWWWRRR..!");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Toast.makeText(_context, "Service is connected", Toast.LENGTH_SHORT).show();
        _bound = true;
        WifiDirectService.LocalBinder localBinder = (WifiDirectService.LocalBinder) service;
        _wifiDirectService = localBinder.getInstance();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Toast.makeText(_context, "Service is disconnected", Toast.LENGTH_SHORT).show();
        _bound = false;
        _wifiDirectService = null;
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) _context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WifiDirectService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
