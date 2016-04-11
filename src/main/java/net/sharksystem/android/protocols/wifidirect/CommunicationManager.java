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
public class CommunicationManager implements WifiP2pManager.DnsSdTxtRecordListener, WifiDirectStatus, WifiP2pManager.PeerListListener {

    private static CommunicationManager instance = new CommunicationManager();
    private List<WifiDirectPeer> peers = new LinkedList<>();
    private WifiDirectConnectionController wifiDirectConnectionController;

    private WifiDirectPeerListener wifiDirectPeerListener;
    private StubController stubControllerListener;
    private Context context = null;
    private WifiDirectPeer connectedPeer;
    public CommunicationManager() {}

    public static CommunicationManager getInstance(){
        return instance;
    }

    public void setConnectionController(WifiDirectConnectionController wifiDirectConnectionController) {
        this.wifiDirectConnectionController = wifiDirectConnectionController;
    }

    public void setStubControllerListener(StubController stubControllerListener) {
        this.stubControllerListener = stubControllerListener;
    }

    public void setWifiDirectPeerListener(WifiDirectPeerListener wifiDirectPeerListener) {
        this.wifiDirectPeerListener = wifiDirectPeerListener;
    }

    public void setConnectedPeer(WifiDirectPeer connectedPeer) {
        this.connectedPeer = connectedPeer;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public WifiDirectPeer getConnectedPeer() {
        return connectedPeer;
    }

    public List<WifiDirectPeer> getPeers() {
        return peers;
    }

    public void restartStub(){
        this.stubControllerListener.onStubRestart();
    }

    public void startStub() {
        try {
            this.stubControllerListener.onStubStart();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopStub(){
        this.stubControllerListener.onStubStop();
    }

    public void connect(WifiDirectPeer peer){
        wifiDirectConnectionController.onConnect(peer);
    }

    public void disconnect(WifiDirectPeer peer){
        wifiDirectConnectionController.onDisconnect(peer);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {

    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
//        L.d("onDnsSdTxtRecordAvailable", srcDevice.toString());

        WifiDirectPeer newPeer = new WifiDirectPeer(srcDevice, txtRecordMap);
        if(this.peers.contains(newPeer)){
            WifiDirectPeer peer = this.peers.get(this.peers.indexOf(newPeer));
            if(peer.getLastUpdated()< newPeer.getLastUpdated()){
                this.peers.remove(peer);
                this.peers.add(newPeer);
            }
        } else {
            this.peers.add(newPeer);
        }
        if(!peers.isEmpty())
            wifiDirectPeerListener.onNewPeer(peers);
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
        if(context!=null)
            Toast.makeText(this.context,toastText, Toast.LENGTH_SHORT).show();
    }
}
