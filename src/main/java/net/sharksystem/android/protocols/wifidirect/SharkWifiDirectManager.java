package net.sharksystem.android.protocols.wifidirect;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.widget.Toast;

import net.sharkfw.system.L;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SharkWifiDirectManager implements WifiP2pManager.DnsSdTxtRecordListener, WifiDirectStatus, WifiP2pManager.PeerListListener {

    private static SharkWifiDirectManager _instance = null;
    private List<WifiDirectPeer> _peers = new LinkedList<>();

    private WifiDirectConnectionController _wifiDirectConnectionController;
    private WifiDirectPeerListener _wifiDirectPeerListener;
    private StubController _stubControllerListener;

    private Context _context = null;
    private WifiDirectPeer _connectedPeer;

    private SharkWifiDirectManager(Context context) {
        this._context = context;
    }

    public static SharkWifiDirectManager getInstance(Context context){
        if(SharkWifiDirectManager._instance==null){
            SharkWifiDirectManager._instance = new SharkWifiDirectManager(context);
        }
        return SharkWifiDirectManager._instance;
    }

    public void setConnectionController(WifiDirectConnectionController wifiDirectConnectionController) {
        this._wifiDirectConnectionController = wifiDirectConnectionController;
    }

    public void setStubControllerListener(StubController stubControllerListener) {
        this._stubControllerListener = stubControllerListener;
    }

    public void setWifiDirectPeerListener(WifiDirectPeerListener wifiDirectPeerListener) {
        this._wifiDirectPeerListener = wifiDirectPeerListener;
    }

    public void setConnectedPeer(WifiDirectPeer connectedPeer) {
        this._connectedPeer = connectedPeer;
    }

    public WifiDirectPeer getConnectedPeer() {
        return _connectedPeer;
    }

    public List<WifiDirectPeer> getPeers() {
        return _peers;
    }

    public void restartStub(){
        this._stubControllerListener.onStubRestart();
    }

    public void startStub() {
        try {
            this._stubControllerListener.onStubStart();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopStub(){
        this._stubControllerListener.onStubStop();
    }

    public void connect(WifiDirectPeer peer){
        _wifiDirectConnectionController.onConnect(peer);
    }

    public void disconnect(WifiDirectPeer peer){
        _wifiDirectConnectionController.onDisconnect(peer);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {

    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
//        L.d("onDnsSdTxtRecordAvailable", srcDevice.toString());

        WifiDirectPeer newPeer = new WifiDirectPeer(srcDevice, txtRecordMap);
        if(this._peers.contains(newPeer)){
            WifiDirectPeer peer = this._peers.get(this._peers.indexOf(newPeer));
            if(peer.getLastUpdated()< newPeer.getLastUpdated()){
                this._peers.remove(peer);
                this._peers.add(newPeer);
            }
        } else {
            this._peers.add(newPeer);
        }
        if(!_peers.isEmpty())
            _wifiDirectPeerListener.onNewPeer(_peers);
    }

    @Override
    public void onStatusChanged(int status) {
        String toastText="";
        switch (status){
            case WifiDirectStatus.DISCOVERING:
                L.d("onStatusChanged", "DISCOVERING...");
                toastText="DISCOVERING";
                break;
            case WifiDirectStatus.CONNECTED:
                L.d("onStatusChanged", "CONNECTED.");
                toastText="CONNECTED";
                break;
            case WifiDirectStatus.DISCONNECTED:
                L.d("onStatusChanged", "DISCONNECTED.");
                toastText="DISCONNECTED";
                break;
            case WifiDirectStatus.STOPPED:
                L.d("onStatusChanged", "STOPPED.");
                toastText="STOPPED";
                break;
            case WifiDirectStatus.INITIATED:
                L.d("onStatusChanged", "INITIATED.");
                toastText="INITIATED";
                break;
        }
        if(_context !=null)
            Toast.makeText(this._context,toastText, Toast.LENGTH_SHORT).show();
    }
}
