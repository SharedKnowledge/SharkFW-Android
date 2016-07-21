package net.sharksystem.android.protocols.wifidirect;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.asip.engine.ASIPMessage;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.protocols.ConnectionStatusListener;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by micha on 28.01.16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class WifiDirectStreamStub
        implements StreamStub,
        WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.DnsSdTxtRecordListener,
        ConnectionStatusListener{

    private final long PEER_DURABILITY = 1000 * 60 * 1; // 1 Minute
    private final int BROADCASTAMOUNT = 5;
    private PeerSemanticTag _owner;


    private final WifiDirectStreamStub that = this;

    private final AndroidSharkEngine _engine;
    private final WifiDirectManager _wifiDirectManager;
    private final WifiDirectBroadcastManager _wifiDirectBroadcastManager;

    private Context _context;
    private WifiP2pManager _manager;
    private boolean _isStarted = false;
    private HashMap<ASIPKnowledge, ArrayList<WifiDirectPeer>> _knowledgeMap;
    // Lists
    private CopyOnWriteArrayList<WifiDirectPeer> _peers = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<WifiP2pDevice> _knownDevices = new CopyOnWriteArrayList<>();

    // ASIP
    private ASIPKnowledge _currentKnowledge;
    private WifiDirectPeer _currentPeer;
    private boolean _isSender = false;
    private ASIPSpace mSpace;

    public WifiDirectStreamStub(Context context, AndroidSharkEngine engine, ASIPSpace space, String name) {
        _context = context;
        _engine = engine;

        _manager = (WifiP2pManager) _context.getSystemService(Context.WIFI_P2P_SERVICE);

//        STSet topics = InMemoSharkKB.createInMemoSTSet();
//        ASIPSpace space = null;
//        try {
//            topics.createSemanticTag("Beispielinteresse", "www.java.com");
////            topics.createSemanticTag("Android", "www.android.com");
//            space = InMemoSharkKB.createInMemoASIPInterest(topics, null, _engine.getOwner(), null, null, null, null, ASIPSpace.DIRECTION_INOUT);
//        } catch (SharkKBException e) {
//            e.printStackTrace();
//        }

        _wifiDirectManager = new WifiDirectManager(_manager, _context, this, space, name);

        _wifiDirectBroadcastManager = WifiDirectBroadcastManager.getInstance(_context);
        _wifiDirectBroadcastManager.setWifiDirectManager(_wifiDirectManager);
        _wifiDirectBroadcastManager.setEngine(_engine);

        _engine.addConnectionStatusListener(_wifiDirectBroadcastManager);

        _knowledgeMap = new HashMap<>();
    }

    @Override
    public void stop() {
        L.d("isStarted:" + _isStarted, this);
        if (_isStarted) {
            _isStarted = _wifiDirectManager.stop();
        }
    }

    @Override
    public void start() throws IOException {
        if (!_isStarted) _isStarted = _wifiDirectManager.start();
    }

    private void updateDevices(WifiP2pDevice device){
        if(_knownDevices.contains(device)){
            _knownDevices.remove(device);
            _knownDevices.add(device);
        } else {
            _knownDevices.add(device);
        }
    }

    private void addPeer(WifiDirectPeer peer){
        if(_peers.contains(peer)){
            int indexOf = _peers.indexOf(peer);
            WifiDirectPeer temp = _peers.get(indexOf);
            if (temp.getLastUpdated() <= peer.getLastUpdated()) {
                _peers.remove(temp);
                _peers.add(peer);
            }
        } else {
            _peers.add(peer);
        }
    }

    public ArrayList<WifiDirectPeer> getAvailablePeers(){
        ArrayList<WifiDirectPeer> availablePeers = new ArrayList<>();

        Iterator<WifiDirectPeer> iterator = _peers.iterator();
        while (iterator.hasNext()){
            WifiDirectPeer current = iterator.next();
            if(current.status == WifiP2pDevice.AVAILABLE){
                availablePeers.add(current);
            }
        }

        return availablePeers;
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {


        // Add device to knownDevices list.
        updateDevices(srcDevice);
        // Add peer tp list
        if(!txtRecordMap.containsKey("interest") && !txtRecordMap.containsKey("name")){
            return;
        }
        WifiDirectPeer peer = new WifiDirectPeer(srcDevice, txtRecordMap);
        addPeer(peer);
        // Update Lists in WifiDirectBroadcastManager
        _wifiDirectBroadcastManager.setDevices(_knownDevices);
        _wifiDirectBroadcastManager.setPeers(_peers);
        _wifiDirectBroadcastManager.notifyUpdate();

        // Send interest to KP
        ASIPInMessage msg = new ASIPInMessage(_engine, peer.getmInterest(), _engine.getAsipStub());
        msg.setCommand(ASIPMessage.ASIP_EXPOSE);
        msg.setTtl(10);
        msg.setSender(peer.getmTag());

        _engine.getAsipStub().callListener(msg);
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

//    private void initConnection(){
//        WifiDirectPeer peer = pickPeer(_currentKnowledge);
//        if(peer != null && _wifiDirectManager.getStatus() == WifiDirectManager.DISCOVERING){
//            _wifiDirectManager.connect(peer);
//        }
//    }
//
//    private boolean knowledgeReachedLimit(ASIPKnowledge knowledge){
//        ArrayList<WifiDirectPeer> peers = _knowledgeMap.get(knowledge);
//        return peers.size() >= BROADCASTAMOUNT ? true : false;
//    }
//
//    private ASIPKnowledge pickAnotherKnowledge(WifiDirectPeer peer){
//        if(_knowledgeMap.size() >= 2){
//            Set<Map.Entry<ASIPKnowledge, ArrayList<WifiDirectPeer>>> entries = _knowledgeMap.entrySet();
//            Iterator<Map.Entry<ASIPKnowledge, ArrayList<WifiDirectPeer>>> entryIterator = entries.iterator();
//            while (entryIterator.hasNext()) {
//                // Get Knowledge and ArrayList
//                Map.Entry<ASIPKnowledge, ArrayList<WifiDirectPeer>> entry = entryIterator.next();
//                ASIPKnowledge asipKnowledge = entry.getKey();
//                ArrayList<WifiDirectPeer> peerArrayList = entry.getValue();
//
//                if (peerArrayList.size() < BROADCASTAMOUNT && peerArrayList.contains(peer)) {
//                    return asipKnowledge;
//                }
//            }
//        } else {
//            throw new ArrayIndexOutOfBoundsException();
//        }
//        return null;
//    }
//
//    private void addPeerToCurrentKnowledge(WifiDirectPeer peer){
//        ArrayList<WifiDirectPeer> list = _knowledgeMap.get(_currentKnowledge);
//        list.add(peer);
//        _knowledgeMap.put(_currentKnowledge, list);
//    }
//
//    private WifiDirectPeer pickPeer(ASIPKnowledge knowledge){
//        if(_knowledgeMap.containsKey(knowledge)){
//            ArrayList<WifiDirectPeer> peers = _knowledgeMap.get(knowledge);
//            Collections.sort(_peers);
//            Iterator<WifiDirectPeer> knownPeers = _peers.iterator();
//
//            while (knownPeers.hasNext()){
//                WifiDirectPeer peer = knownPeers.next();
//                if(!peers.contains(peer)){
//                    _currentPeer = peer;
//                    return peer;
//                }
//            }
//        } else {
//            L.d("Knowledge not known.");
//        }
//        return null;
//    }

    public void sendBroadcast(ASIPKnowledge knowledge){

        _wifiDirectBroadcastManager.addKnowledge(knowledge);

//        // TODO to be removed
//        _knowledgeMap.put(knowledge, new ArrayList<WifiDirectPeer>());
//        _currentKnowledge = knowledge;
//        if(!_peers.isEmpty()){
//            initConnection();
//        }
//        _handler.post(this);
    }

    public void sendBroadcast(String text) {

        _isSender = true;

        ASIPKnowledge knowledge = InMemoSharkKB.createInMemoKnowledge();
        try {
            STSet types = InMemoSharkKB.createInMemoSTSet();
            types.createSemanticTag("BROADCAST", "www.sharksystem.de/broadcast");
            ASIPSpace space = InMemoSharkKB.createInMemoASIPInterest(null, types, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
            knowledge.addInformation(text, space);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
        this.sendBroadcast(knowledge);
    }

    public void onDisconnected(){
        L.d("Disconnect successful", this);
        _wifiDirectBroadcastManager.onDisconnected();
    }

//    private ASIPOutMessage createASIPOutMessage(WifiDirectPeer peer, String address){
//        PeerSemanticTag tag = peer.getmTag();
////        PeerSemanticTag tcpTag = InMemoSharkKB.createInMemoPeerSemanticTag(tag.getName(), tag.getSI(), "tcp://" + address + ":7071");
//        PeerSemanticTag tcpTag = InMemoSharkKB.createInMemoPeerSemanticTag("Receiver", "www.receiver.de", "tcp://"+address+":7071");
//        L.d(tcpTag.getAddresses().toString(), this);
//        L.d(tcpTag.getAddresses()[0].toString(), this);
//        return _engine.createASIPOutMessage(tcpTag.getAddresses(), tcpTag);
//    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        if(info==null) return;

//        Toast.makeText(_context, "Connection incoming", Toast.LENGTH_SHORT).show();

        _wifiDirectManager.requestGroupInfo(new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {

                if(info.groupFormed){
                    _wifiDirectBroadcastManager.onConnectionEstablished(info, group);
                }
//
//                if(info.groupFormed && info.isGroupOwner){
//                    // Owner
//                    Toast.makeText(_context, "I'm the owner", Toast.LENGTH_SHORT).show();
//                    L.d("I'm the owner", this);
//
//                    // startTCP
//                    try {
//                        _engine.startTCP(7071, _currentKnowledge);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    Toast.makeText(_context, "Waiting for connections...", Toast.LENGTH_LONG).show();
//
//                    // Now wait for incoming Connection
//                    // ...
//
//                } else if(info.groupFormed){
//                    // Client
//                    L.d("I'm the client", this);
//                    Toast.makeText(_context, "I'm the client", Toast.LENGTH_LONG).show();
//
//                    // hey I'm the client..
//                    // go get me the IP Address of the Host
//                    final String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
//                    L.d("groupOwnerAddress:"+groupOwnerAddress, this);
//                    // perfect, now go get me the WifiP2PDevice
//
//                    final WifiDirectPeer owner = new WifiDirectPeer(group.getOwner(), null);
//                    // Now check if I have knowledge I can send
//                    ASIPKnowledge knowledge = null;
//                    if(!_knowledgeMap.isEmpty()){
//                        L.d("I have knowledge to send", this);
//                        // hey, I have knowledge
//                        // new go get me the knowledgeMap
//                        Set<Map.Entry<ASIPKnowledge, ArrayList<WifiDirectPeer>>> entries = _knowledgeMap.entrySet();
//                        Iterator<Map.Entry<ASIPKnowledge, ArrayList<WifiDirectPeer>>> entryIterator = entries.iterator();
//                        // perfect. now iterate through each entry and pick a knowledge
//                        while (entryIterator.hasNext()){
//                            Map.Entry<ASIPKnowledge, ArrayList<WifiDirectPeer>> next = entryIterator.next();
//                            // get the deviceList to check, if I've already sent the knowledge to the device
//                            ArrayList<WifiDirectPeer> deviceList = next.getValue();
//                            // check if owner is NOT in the list
//                            if(!deviceList.contains(owner) && deviceList.size() < BROADCASTAMOUNT){
//                                // Hey, this is a knowledge I haven't sent to the owner.
//                                knowledge = next.getKey();
//                                break;
//                            }
//                        }
//
//                        L.d("Knowledges had been searched", this);
//
//                        final ASIPKnowledge finalKnowledge = knowledge;
//
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                ASIPOutMessage  msg = createASIPOutMessage(owner, groupOwnerAddress);
//                                // open a thread and send the knowledge
//                                msg.insert(finalKnowledge);
//                                L.d("K sent", this);
//                                // Now wait until the stream ends
//                                //....
//                            }
//                        }).start();
//                    } else {
//                        L.d("no k2", this);
//                        // no knowledge - create just a simple TCPConnection
//
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                ASIPOutMessage  msg = createASIPOutMessage(owner, groupOwnerAddress);
//                                // open a thread and send the knowledge
//                                msg.insert(null);
//                                L.d("K sent", this);
//                                // Now wait until the stream ends
//                                //....
//                            }
//                        }).start();
//                    }
//                }




            }
        });

    }

    /**
     * Triggered when TCPConnection is closed.
     */
    @Override
    public void connectionClosed() {
        L.d("TCPConnection got closed", this);
        _wifiDirectManager.disconnect();
//        L.d("Disconnect triggered.",this);
    }
}
