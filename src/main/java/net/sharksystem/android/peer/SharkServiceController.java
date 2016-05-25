package net.sharksystem.android.peer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import net.sharkfw.system.L;
import net.sharksystem.android.protocols.wifidirect.WifiDirectListener;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;

import java.util.ArrayList;

/**
 * Created by j4rvis on 26.04.16.
 */
public class SharkServiceController
        extends BroadcastReceiver
        implements ServiceConnection {

    private ArrayList<WifiDirectPeer> _peers = null;

    private boolean _isBound;
    private Intent _intent = null;
    private Context _context = null;
    private static SharkServiceController _instance;
    private SharkService _sharkService;

    public static synchronized SharkServiceController getInstance(Context context) {
        if (_instance == null)
            _instance = new SharkServiceController(context);
        return _instance;
    }

    public SharkServiceController(Context context) {
        _context = context.getApplicationContext();
        _peers = new ArrayList<>();

        _intent = new Intent(_context, SharkService.class);
        _context.startService(_intent);
    }

    public void stopService(){
        _context.stopService(_intent);
    }

    public void bindToService(){
        registerReceiver();
        Toast.makeText(_context, "Binding...", Toast.LENGTH_SHORT).show();
        if(!_isBound){
            _isBound = _context.bindService(_intent, this, _context.BIND_AUTO_CREATE);
        }
    }

    public void unbindFromService(){
        LocalBroadcastManager.getInstance(_context).unregisterReceiver(this);
        if(_isBound){
            Toast.makeText(_context, "Unbinding...", Toast.LENGTH_SHORT).show();
            _context.unbindService(this);
            _isBound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Toast.makeText(_context, "Service is connected", Toast.LENGTH_SHORT).show();
        _isBound = true;
        SharkService.LocalBinder localBinder = (SharkService.LocalBinder) service;
        _sharkService = localBinder.getInstance();

        // Set engine and kp if wanted

        _sharkService.startEngine();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Toast.makeText(_context, "Service is disconnected", Toast.LENGTH_SHORT).show();
        _isBound = false;
        _sharkService = null;
    }

    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiDirectListener.NEW_PEERS_ACTION);
        intentFilter.addAction(WifiDirectListener.NEW_RECORDS_ACTION);
        intentFilter.addAction(WifiDirectListener.CONNECTION_ESTABLISHED_ACTION);
        LocalBroadcastManager.getInstance(_context).registerReceiver(
                this, intentFilter);
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
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) _context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SharkService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<WifiDirectPeer> getPeers(){
        return _peers;
    }

    public void connect(WifiDirectPeer peer){
        _sharkService.connect(peer);
    }

    public void disconnect(){
        _sharkService.disconnect();
    }

    public void sendMessage(String text){
        _sharkService.sendMessage(text);
    }

    public void sendBroadcast(String text){ _sharkService.sendBroadcast(text);}
}
