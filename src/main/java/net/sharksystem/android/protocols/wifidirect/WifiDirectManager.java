package net.sharksystem.android.protocols.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Handler;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.protocols.StreamStub;

import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by j4rvis on 25.05.16.
 */
public class WifiDirectManager
        extends BroadcastReceiver
        implements Runnable{

    private final WifiP2pManager _manager;
    private final Context _context;
    private ASIPSpace _interest;
    private WifiDirectStreamStub _stub;
    private WifiP2pManager.Channel _channel;
    private Handler _handler;
    private Map<String, String> _map;
    private WifiP2pDnsSdServiceInfo _serviceInfo;

    private boolean _isStarted = false;
    private boolean _isReceiverRegistered = false;

    private int _INTERVALL = 10000;

    public WifiDirectManager(WifiP2pManager manager, Context context, WifiDirectStreamStub stub) {
        _manager = manager;
        _context = context;
        _stub = stub;

        _channel =_manager.initialize(_context, _context.getMainLooper(), null);
        _manager.setDnsSdResponseListeners(_channel, null, _stub);

        _handler = new Handler();
        _map = new HashMap<>();
    }

    public WifiDirectManager(WifiP2pManager manager, Context context, WifiDirectStreamStub stub, ASIPSpace space) {
        this(manager, context, stub);
        _interest = space;
    }

    @Override
    public void run() {
        _manager.discoverServices(_channel, new WifiActionListener("Discover Services"));
        _handler.postDelayed(this, _INTERVALL);
    }

    public boolean start() {
        if(!_isStarted){
            if(!_isReceiverRegistered){

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                _context.registerReceiver(this, intentFilter);
                _isReceiverRegistered = true;

            }

            initAdvertising();

            _manager.clearServiceRequests(_channel, new WifiActionListener("Clear ServiceRequests"));
            WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
            _manager.addServiceRequest(_channel, serviceRequest, new WifiActionListener("Add ServiceRequest"));

            _isStarted = true;

            _handler.post(this);

        }
        return _isStarted;
    }

    public boolean stop() {
        if(_isStarted){

            _handler.removeCallbacks(this);
            _manager.clearServiceRequests(_channel, new WifiActionListener("Clear ServiceRequests"));
            _manager.clearLocalServices(_channel, new WifiActionListener("Clear LocalServices"));

            if(_isReceiverRegistered){
                _context.unregisterReceiver(this);
                _isReceiverRegistered = false;
            }
        }
        return _isStarted;
    }

    private void resetDNSMap(){
        if(_isStarted){

            _handler.removeCallbacks(this);
            _manager.clearServiceRequests(_channel, new WifiActionListener("Clear ServiceRequests"));
            _manager.clearLocalServices(_channel, new WifiActionListener("Clear LocalServices"));

            initAdvertising();

            WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
            _manager.addServiceRequest(_channel, serviceRequest, new WifiActionListener("Add ServiceRequest"));

            _handler.post(this);
        } else {
            start();
        }
    }

    private void initAdvertising(){
        _manager.clearLocalServices(_channel, new WifiActionListener("Clear LocalServices"));

        if(_interest != null){
            String parsedInterest = "";
            try {
                parsedInterest = ASIPSerializer.serializeASIPSpace(_interest).toString();
                _map.put("interest", parsedInterest);
            } catch (SharkKBException e) {
                e.printStackTrace();
            }
        } else {
            // Add empty interest?
            _map.put("interest", "no interest");
        }
        _serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_sbc", "_presence._tcp", _map);
        _manager.addLocalService(_channel, _serviceInfo, new WifiActionListener("Add LocalService"));
    }

    public void connect(PeerSemanticTag peer){

    }

    public void connect(WifiDirectPeer peer){
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peer.getDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        _manager.connect(_channel, config, new WifiActionListener("Connect"));
    }

    public void offerInterest(ASIPSpace space) {
        _interest = space;
        resetDNSMap();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(_manager==null)
            return;

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
//                setIsWifiP2pEnabled(true);
            } else {
//                setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
//            if (_state != WifiDirectStreamStubState.READY)
//            _manager.requestPeers(_channel, _stub);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()){
                _manager.requestConnectionInfo(_channel, _stub);
            }
        }
    }

    public void requestGroupInfo(WifiP2pManager.GroupInfoListener listener) {
        _manager.requestGroupInfo(_channel, listener);
    }
}
