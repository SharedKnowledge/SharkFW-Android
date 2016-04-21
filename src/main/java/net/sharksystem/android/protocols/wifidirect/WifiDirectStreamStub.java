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

    private IntentFilter _intentFilter;
    private Context _context;
    private final WeakReference<Activity> _activity;
    private RequestHandler _handler;

    private boolean _isStarted = false;
    private WifiP2pManager _manager;
    private WifiP2pManager.Channel _channel;
    private WifiP2pDnsSdServiceInfo _serviceInfo;
    private Map<String, String> _txtRecordMap;

    private Handler threadHandler;
    private Runnable thread;
    private int threadRuns = 0;
    private NetworkInfo _networkInfo;

    private boolean isStarted = false;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Map<String, String> txtRecordMap;

    private WifiDirectListener _wifiDirectListener;

    public WifiDirectStreamStub(Context context, WeakReference<Activity> activity) {
        _context = context;
        _activity = activity;

        _manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        _channel =_manager.initialize(context, context.getMainLooper(), null);

        _wifiDirectListener = WifiDirectListener.getInstance(_context);
        _manager.setDnsSdResponseListeners(_channel, null, _wifiDirectListener);
        _wifiDirectListener.onStatusChanged(WifiDirectStatus.INITIATED);

        _intentFilter = new IntentFilter();
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        _intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        _context.registerReceiver(this, _intentFilter);

        _txtRecordMap = new HashMap<>();
        _txtRecordMap.put("entry0", getLocalAddress());
        _txtRecordMap.put("entry1", "This is just a test");
        _txtRecordMap.put("entry2", "to check if discovering is working");
        _serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_shark", "_presence._tcp", _txtRecordMap);


        threadHandler = new Handler();
        thread = new Runnable() {
            @Override
            public void run() {
                removeServiceAdvertiser();
                stopServiceDiscovery();
                addServiceAdvertiser();
                startServiceDiscovery();
//                int timer = threadRuns < 3 ? 30000 - 10000*threadRuns : 10000 ;
//                threadRuns++;
                threadHandler.postDelayed(this, 10000);
            }
        };

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
        _handler = handler;
    }

    @Override
    public void stop() {
        if(_isStarted){
            threadHandler.removeCallbacks(thread);
            stopServiceDiscovery();
            removeServiceAdvertiser();
            _wifiDirectListener.onStatusChanged(WifiDirectStatus.STOPPED);
            _isStarted=!_isStarted;
        }
    }

    @Override
    public void start() throws IOException {
        if(!_isStarted){
            threadHandler.post(thread);
            _wifiDirectListener.onStatusChanged(WifiDirectStatus.DISCOVERING);
            _isStarted=!_isStarted;
        }
    }

    private void startPeerDiscovering(){
        _context.registerReceiver(this, _intentFilter);
        _manager.discoverPeers(_channel, new WifiActionListener("Discover Peers"));
    }

    private void stopPeerDiscovering(){
        _manager.discoverPeers(_channel, new WifiActionListener("Stop Peerdiscovering"));
        _context.unregisterReceiver(this);
    }

    private void addServiceAdvertiser(){
        _manager.addLocalService(_channel, _serviceInfo, new WifiActionListener("Add LocalService"));
    }

    private void removeServiceAdvertiser(){
        _manager.clearLocalServices(_channel, new WifiActionListener("Clear LocalServices"));
    }

    private void startServiceDiscovery(){
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        _manager.addServiceRequest(_channel, serviceRequest, new WifiActionListener("Add ServiceRequest"));
        _manager.discoverServices(
                _channel, new WifiActionListener("Discover services"));
    }

    private void stopServiceDiscovery(){
        _manager.clearServiceRequests(_channel, new WifiActionListener("Clear ServiceRequests"));
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

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(_manager!=null)
            return;

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
            } else {
                // Wi-Fi P2P is not enabled
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            _manager.requestPeers(_channel, _wifiDirectListener);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            _networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (_networkInfo.isConnected()) {
                // We are connected with the other device, request connection
                // info to find group owner IP
                _manager.requestConnectionInfo(_channel, this);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
//        L.d("onConnectionInfoAvailable", info.toString());
        _wifiDirectListener.onStatusChanged(WifiDirectStatus.CONNECTED);
    }

    public void connect(WifiDirectPeer peer) {
        L.d("Connect to peer", this);
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peer.getDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        _manager.connect(_channel, config, new WifiActionListener("Connect"));
    }

    public void disconnect(WifiDirectPeer peer) {
        L.d("Disconnect from peer", this);
//        _manager.cancelConnect(_channel, new WifiActionListener("onDisconnect"));
    }
}
