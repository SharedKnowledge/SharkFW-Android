package net.sharksystem.android.protocols.wifidirect;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;

import net.sharkfw.asip.ASIPInformation;
import net.sharkfw.asip.ASIPInformationSpace;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPOutMessage;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkAlgebra;
import net.sharkfw.knowledgeBase.SharkCS;
import net.sharkfw.knowledgeBase.SharkCSAlgebra;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.system.L;
import net.sharksystem.android.peer.AndroidSharkEngine;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by j4rvis on 01.06.16.
 */
public class WifiDirectBroadcastManager implements Runnable{

    private static WifiDirectBroadcastManager _instance = null;
    private Context _context;

    // Lists and maps
    private ArrayList<WifiP2pDevice> _devices = new ArrayList<>();
    private ArrayList<WifiDirectPeer> _peers = new ArrayList<>();
    private HashMap<ASIPKnowledge, ArrayList<PeerSemanticTag>> _map = new HashMap<>();
    private boolean _isTryingToSend = false;
    private WifiDirectManager _manager;

    // Thread
    private Handler _handler = new Handler();
    private final long THREAD_TIMEOUT = 5000;
    public boolean _isRunning = false;

    // defined Shark things
    private ASIPSpace _broadcastSpace;
    private ASIPSpace _peerSpace;
    private ASIPKnowledge _lastAddedKnowledge = null;
    private boolean _isConnected;
    private ASIPKnowledge _knowledgeToSend;
    private AndroidSharkEngine _engine;
    private boolean _isOwner = false;
    private boolean _isConnecting = false;
    private WifiDirectPeer _lastConnectedPeer = null;

    public static synchronized WifiDirectBroadcastManager getInstance(Context context){
        if(_instance == null){
            _instance = new WifiDirectBroadcastManager(context);
        }
        return _instance;
    }

    public WifiDirectBroadcastManager(Context context) {
        _context = context;

        STSet peerTypes = InMemoSharkKB.createInMemoSTSet();
        STSet broadcastTypes = InMemoSharkKB.createInMemoSTSet();
        try {
            peerTypes.createSemanticTag("PEERS", "www.sharksystem.de/peers");
            broadcastTypes.createSemanticTag("BROADCAST", "www.sharksystem.de/broadcast");
            _peerSpace = InMemoSharkKB.createInMemoASIPInterest(null, peerTypes, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
            _broadcastSpace = InMemoSharkKB.createInMemoASIPInterest(null, broadcastTypes, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    public void setEngine(AndroidSharkEngine engine){
        _engine = engine;
    }

    public void setWifiDirectManager(WifiDirectManager manager){
        _manager = manager;
    }

    public void setDevices(ArrayList<WifiP2pDevice> devices){
        _devices = devices;
    }

    public void addKnowledge(ASIPKnowledge knowledge, PeerSemanticTag sender){

        if(_map.containsKey(knowledge)){
            return;
        } else {
            try {
                knowledge.addInformation(ASIPSerializer.serializeTag(sender).toString(), _peerSpace);
            } catch (JSONException | SharkKBException e) {
                e.printStackTrace();
            }
            ArrayList knowledgePeers = getKnowledgePeers(knowledge);
            _map.put(knowledge, knowledgePeers);
        }

        _lastAddedKnowledge = knowledge;

        if(!_isConnecting) _handler.post(this);
    }

    public void addKnowledge(ASIPKnowledge knowledge){

        if(_map.containsKey(knowledge)){
            L.d("Knowledge already known", this);
            return;
        } else {
            ArrayList knowledgePeers = getKnowledgePeers(knowledge);
            _map.put(knowledge, knowledgePeers);
            L.d("Knowledge added to map", this);
        }

        _lastAddedKnowledge = knowledge;

        if(!_isConnecting) _handler.post(this);
    }

    private ArrayList<PeerSemanticTag> getKnowledgePeers(ASIPKnowledge knowledge){
        ArrayList<PeerSemanticTag> temp = new ArrayList<>();
        try {
            Iterator<ASIPInformationSpace> asipInformationSpaceIterator = knowledge.informationSpaces();
            while (asipInformationSpaceIterator.hasNext()){
                ASIPInformationSpace informationSpace = asipInformationSpaceIterator.next();
                if(SharkAlgebra.identical(informationSpace.getASIPSpace(), _peerSpace)){
                    Iterator<ASIPInformation> informations = informationSpace.informations();
                    while (informations.hasNext()){
                        ASIPInformation information = informations.next();
                        String informationContentAsString = information.getContentAsString();
                        temp.add(ASIPSerializer.deserializePeerTag(informationContentAsString));
                    }
                }
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
        return temp;
    }

    public void setPeers(ArrayList<WifiDirectPeer> peers){
        _peers = peers;
    }

    public ArrayList<WifiDirectPeer> getAvailablePeers(){
        ArrayList<WifiDirectPeer> availablePeers = new ArrayList<>();

        Iterator<WifiDirectPeer> iterator = _peers.iterator();
        while (iterator.hasNext()){
            WifiDirectPeer current = iterator.next();
            if(current.status == WifiP2pDevice.AVAILABLE && current.getLastUpdated() > 0){
                availablePeers.add(current);
            }
        }

        return availablePeers;
    }

    @Override
    public void run() {
        _isRunning = true;
        // Keep running
        // Do we have a running Connection?
        // yes? return
        // no? go on
        // Do we have knowledge to send?
        // no? return
        // yes? try to connect to a peer
            // do we have available peers?
            // no? return
            // yes? go on
            // iterate the peers
                // did we already sent the knowledge to the peer
                // no? next
                // yes?
                // pick the peer
            // do we have a peer?
            // no? return
            // yes? try to connect to the peer

        if(_isConnected || _isTryingToSend){
            L.d("Busy", this);
            _handler.postDelayed(this, THREAD_TIMEOUT);
            return;
        } else if(_isConnecting) {
            _isConnecting = false;
            _handler.postDelayed(this, THREAD_TIMEOUT);
        } else {
            if(_map.isEmpty()){
//                L.d("Map is empty", this);
                return;
            } else {
                if(getAvailablePeers().isEmpty()){
                    L.d("No available Peers", this);
                    return;
                } else {
//                    L.d("okay try to send the Knowledge", this);
//                    ArrayList<ASIPKnowledge> checkedKnowledges = new ArrayList<>();
//                    ArrayList<WifiDirectPeer> checkedPeers = new ArrayList<>();
                    WifiDirectPeer peer = null;

                    Iterator<ASIPKnowledge> iterator = _map.keySet().iterator();
                    while (iterator.hasNext() && peer==null){
                        ASIPKnowledge knowledge = iterator.next();
                        peer = pickPeer(knowledge);
                        if(peer!=null){
                            _knowledgeToSend = knowledge;
                        }
                    }

                    if(_knowledgeToSend==null){
                        L.d("To be sent Knowledge is null", this);
                        return;
                    }

//                    L.d("Peer picked!", this);
                    if(peer==null){
                        L.d("Did not find any suitable peer", this);
                        return;
                    }
//                    else if(peer.equals(_lastConnectedPeer)){
//                        L.d("Do not try to connect to last Peer");
//                        return;
//                    }

                    L.d("Connecting to Peer", this);

                    _isConnecting = true;
                    initConnection(peer);
                    _lastConnectedPeer = peer;
                    _isTryingToSend = true;

                    if(_lastConnectedPeer!=null){
                        int i = _peers.indexOf(_lastConnectedPeer);
                        _lastConnectedPeer.resetUpdated();
                        _peers.add(i, _lastConnectedPeer);
                    }

                    _handler.postDelayed(this, THREAD_TIMEOUT*3);
                }
            }
        }

        _isRunning = false;
    }



    private WifiDirectPeer pickPeer(ASIPKnowledge knowledge){
        if(_map.containsKey(knowledge)){
//            L.d("Knowledge found in map.", this);
            ArrayList<PeerSemanticTag> peers = _map.get(knowledge);

            ArrayList availablePeers = getAvailablePeers();
//            L.d("Size of availablePeers: " + availablePeers.size(), this);
            Collections.sort(availablePeers);
            Iterator<WifiDirectPeer> knownPeers = availablePeers.iterator();
            while (knownPeers.hasNext()){
                WifiDirectPeer peer = knownPeers.next();

                if(peers.isEmpty()){
//                    L.d("No peers to check against. return first peer", this);
                    return peer;
                }

                PeerSemanticTag tag = peer.getTag();
                String wifiAddress = "";
                // Get wifiAddress of the peer
                String[] peerAddresses = tag.getAddresses();
                for(String s : peerAddresses){
                    if(s.startsWith(WifiDirectPeer.WIFI_PROTOCOL)){
                        wifiAddress = s;
                    }
                }
                // Go to next peer if no wifiAddress
                if(wifiAddress.isEmpty()) continue;
                else {
                    // Check if peer already has the knowledge
                    Iterator<PeerSemanticTag> peersIterator = peers.iterator();
                    while (peersIterator.hasNext()){
                        PeerSemanticTag tag1 = peersIterator.next();
                        for (String s: tag1.getAddresses()){
                            if(s.startsWith(WifiDirectPeer.WIFI_PROTOCOL)){
                                if (s.equals(wifiAddress)) continue;
                                else {
                                    return peer;
                                }
                            }
                        }

                    }
                }

            }
        } else {
            L.d("Knowledge not known.", this);
        }
        return null;
    }

    private void initConnection(WifiDirectPeer peer){
        _manager.connect(peer);
    }

    public void onConnectionEstablished(WifiP2pInfo info, final WifiP2pGroup group){

        _isConnected = true;
        _isConnecting = false;
        _isOwner = info.isGroupOwner;

        final String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
        final WifiDirectPeer owner = new WifiDirectPeer(group.getOwner(), null);

        if(_isOwner){
            if(_knowledgeToSend==null){
                L.d("OWNER!!!");
            } else {
                L.d("OWNER WITH KNOWLEDGE!!!");
            }
            try {
                _engine.startTCP(7071, _knowledgeToSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if(_knowledgeToSend==null){
                L.d("CLIENT!!!");
            } else {
                L.d("CLIENT WITH KNOWLEDGE!!!");
            }
            try {
                Thread.sleep(THREAD_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ASIPKnowledge knowledge = null;
            if(_isTryingToSend){
                L.d("Active sender!", this);
               knowledge = _knowledgeToSend;
            }

            final ASIPKnowledge finalKnowledge = knowledge;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    PeerSemanticTag tcpTag = InMemoSharkKB.createInMemoPeerSemanticTag("Receiver", "www.receiver.de", "tcp://"+groupOwnerAddress+":7071");
                    ASIPOutMessage msg = _engine.createASIPOutMessage(tcpTag.getAddresses(), tcpTag);
                    msg.insert(finalKnowledge);
                }
            }).start();
        }
    }

    public void onDisconnected(){
        _isConnected = false;
        _isOwner = false;
        _isTryingToSend = false;
    }

    public void notifyUpdate(){
        L.d("Update", this);
        if(!_isConnecting) _handler.post(this);
    }
}
