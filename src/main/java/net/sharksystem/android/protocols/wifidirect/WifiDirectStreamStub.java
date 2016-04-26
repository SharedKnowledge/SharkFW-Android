package net.sharksystem.android.protocols.wifidirect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Handler;

import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.StreamConnection;
import net.sharkfw.protocols.StreamStub;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkNotSupportedException;
import net.sharksystem.android.peer.AndroidSharkEngine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class WifiDirectStreamStub
        extends BroadcastReceiver
        implements StreamStub, WifiP2pManager.ConnectionInfoListener{

    private final AndroidSharkEngine _engine;
    private Context _context;

    private boolean _isStarted = false;
    private WifiP2pManager _manager;
    private WifiP2pManager.Channel _channel;
    private WifiP2pDnsSdServiceInfo _serviceInfo;

    private Handler threadHandler;
    private Runnable thread;

    private WifiDirectListener _wifiDirectListener;
    private boolean _receiverRegistered = false;

    public WifiDirectStreamStub(Context context, AndroidSharkEngine engine) {
        _context = context;
        _engine = engine;

        threadHandler = new Handler();
        thread = new Runnable() {
            @Override
            public void run() {
                startServiceDiscovery();
                threadHandler.postDelayed(this, 10000);
            }
        };

        _manager = (WifiP2pManager) _context.getSystemService(Context.WIFI_P2P_SERVICE);
        _channel =_manager.initialize(_context, _context.getMainLooper(), null);

        _wifiDirectListener = WifiDirectListener.getInstance(_context);
        _manager.setDnsSdResponseListeners(_channel, _wifiDirectListener, _wifiDirectListener);
        _wifiDirectListener.onStatusChanged(WifiDirectStatus.INITIATED);
    }


    private void registerReceiver(){
        if(!_receiverRegistered){
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            _context.registerReceiver(this, intentFilter);
            _receiverRegistered=true;
            L.d("Receiver registered", this);
        }
    }

    @Override
    public void stop() {
        if(_isStarted){

            threadHandler.removeCallbacks(thread);
            stopServiceDiscovery();
//            _manager.stopPeerDiscovery(_channel, new WifiActionListener("Stop Peerdiscovering"));
            removeServiceAdvertiser();
            if(_receiverRegistered){
                _context.unregisterReceiver(this);
                _receiverRegistered=false;
            }

            _wifiDirectListener.onStatusChanged(WifiDirectStatus.STOPPED);
            _isStarted=false;
        }
    }

    @Override
    public void start() throws IOException {
        if(!_isStarted){
            registerReceiver();
            addServiceAdvertiser();
            addServiceRequest();
            startServiceDiscovery();
//            _manager.discoverPeers(_channel, new WifiActionListener("Discover Peers"));
            threadHandler.postDelayed(thread, 10000);
            _wifiDirectListener.onStatusChanged(WifiDirectStatus.DISCOVERING);
            _isStarted=true;
        }
    }

    private void addServiceAdvertiser(){
        _manager.clearLocalServices(_channel, null);

        Map<String, String> txtRecordMap = new HashMap<>();
        txtRecordMap.put("entry0", getLocalAddress());
        txtRecordMap.put("entry1", "This is just a test");
        txtRecordMap.put("entry2", "to check if discovering is working");
        _serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_shark", "_presence._tcp", txtRecordMap);

        _manager.addLocalService(_channel, _serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
//                _manager.createGroup(_channel, new WifiActionListener("Create Group"));
            }

            @Override
            public void onFailure(int reason) {
                L.d("Failed addServiceAdvertiser. Reason: " + reason, this);
            }
        });
    }

    private void removeServiceAdvertiser(){
        _manager.clearLocalServices(_channel, new WifiActionListener("Clear LocalServices"));
    }

    private void addServiceRequest(){
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        _manager.addServiceRequest(_channel, serviceRequest, new WifiActionListener("Add ServiceRequest"));
    }

    private void startServiceDiscovery(){
        _manager.discoverServices(_channel, new WifiActionListener("Discover services"));
    }

    private void stopServiceDiscovery(){
        _manager.clearServiceRequests(_channel, new WifiActionListener("Clear ServiceRequests"));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

//        L.d(action, this);

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
            _manager.requestPeers(_channel, _wifiDirectListener);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            L.d("connection changed", this);
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()){
                _manager.requestConnectionInfo(_channel, WifiDirectStreamStub.this);
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        // InetAddress from WifiP2pInfo struct.
        String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
        L.d("Device address:" + groupOwnerAddress, this);

        L.d(info.groupOwnerAddress.toString(), this);

        _wifiDirectListener.onStatusChanged(WifiDirectStatus.CONNECTED);
        if (info.groupFormed && info.isGroupOwner) {

            try {
                _engine.startTCP(7070);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        }
        // After the group negotiation, we can determine the group owner.

    }

    public void connect(WifiDirectPeer peer) {
        L.d("Connect to peer: " + peer.getName(), this);
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peer.getDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        _manager.connect(_channel, config, new WifiActionListener("Connect"));
    }

    public void disconnect() {
        _manager.requestGroupInfo(_channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null && _manager != null && _channel != null
                        && group.isGroupOwner()) {
                    _manager.removeGroup(_channel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            L.d("removeGroup onSuccess -", this);
                        }

                        @Override
                        public void onFailure(int reason) {
                            L.d("removeGroup onFailure -" + reason, this);
                        }
                    });
                }
            }
        });
    }

    @Override
    public StreamConnection createStreamConnection(String addressString) throws IOException {
        return null;
    }

    @Override
    public String getLocalAddress() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/class/net/wlan0/address"));
            String address = br.readLine();
            br.close();
            return address;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setHandler(RequestHandler handler) {
    }

    @Override
    public boolean started() {
        return _isStarted;
    }

    @Override
    public void offer(ASIPSpace asipSpace) throws SharkNotSupportedException {

    }

    @Override
    public void offer(Knowledge knowledge) throws SharkNotSupportedException {

    }

}
