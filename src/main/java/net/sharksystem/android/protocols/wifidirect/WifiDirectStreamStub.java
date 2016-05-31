package net.sharksystem.android.protocols.wifidirect;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.knowledgeBase.PeerSTSet;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.StreamConnection;
import net.sharkfw.protocols.StreamStub;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkNotSupportedException;
import net.sharksystem.android.peer.AndroidSharkEngine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class WifiDirectStreamStub
        implements StreamStub,
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.DnsSdTxtRecordListener,
        WifiP2pManager.GroupInfoListener {

    private final AndroidSharkEngine _engine;
    private final WifiDirectManager _wifiDirectManager;
    private Context _context;

    private WifiP2pManager _manager;

    // ASIP
    private PeerSemanticTag _peerSemanticTag;
    private ASIPSpace _interest;
    private Knowledge _knowledge;

//    private ArrayList<WifiDirectPeer> _peers = new ArrayList<>();
    private boolean _isStarted = false;

    public WifiDirectStreamStub(Context context, AndroidSharkEngine engine) {
        _context = context;
        _engine = engine;

        _manager = (WifiP2pManager) _context.getSystemService(Context.WIFI_P2P_SERVICE);

        STSet topics = InMemoSharkKB.createInMemoSTSet();
        ASIPSpace space = null;
        try {
            topics.createSemanticTag("Java", "www.java.com");
            topics.createSemanticTag("Android", "www.android.com");
            space = InMemoSharkKB.createInMemoASIPInterest(topics, null, _engine.getOwner(), null, null, null, null, ASIPSpace.DIRECTION_INOUT);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }

        _wifiDirectManager = new WifiDirectManager(_manager, _context, this, space);

    }

    @Override
    public void stop() {
        if (_isStarted) _isStarted = _wifiDirectManager.stop();

    }

    @Override
    public void start() throws IOException {
        if (!_isStarted) _isStarted = _wifiDirectManager.start();
    }

    public void connect(WifiDirectPeer peer) {
        _wifiDirectManager.connect(peer);
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {

        WifiDirectPeer peer = new WifiDirectPeer(srcDevice, txtRecordMap);
        ASIPInMessage msg = new ASIPInMessage(_engine, peer.getInterest(), _engine.getAsipStub());
        msg.setTtl(10);
        msg.setSender(peer.getTag());

        _engine.getAsipStub().callListener(msg);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
        _peerSemanticTag = InMemoSharkKB.createInMemoPeerSemanticTag("Receiver", "www.receiver.de", "tcp://" + groupOwnerAddress + ":7071");

        if (info.groupFormed && info.isGroupOwner) {

            try {
                _engine.startTCP(7071);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        } else if (info.groupFormed) {
            // The device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
        }
        // After the group negotiation, we can determine the group owner.
//        _wifiDirectListener.onStatusChanged(WifiDirectStatus.CONNECTED);

        _wifiDirectManager.requestGroupInfo(new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {

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
        _wifiDirectManager.offerInterest(asipSpace);
    }

    @Override
    public void offer(Knowledge knowledge) throws SharkNotSupportedException {

    }

    public void sendBroadcast(String text) {

        _knowledge = InMemoSharkKB.createInMemoKnowledge();
        try {
            PeerSTSet approvers = InMemoSharkKB.createInMemoPeerSTSet();
            approvers.merge(_engine.getOwner());
            ASIPSpace space = InMemoSharkKB.createInMemoASIPInterest(null, null, _engine.getOwner(), approvers, null, null, null, ASIPSpace.DIRECTION_INOUT);
            _knowledge.addInformation(text, space);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {

    }
}
