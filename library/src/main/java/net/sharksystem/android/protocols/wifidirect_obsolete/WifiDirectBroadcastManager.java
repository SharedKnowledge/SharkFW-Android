package net.sharksystem.android.protocols.wifidirect_obsolete;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import net.sharkfw.asip.ASIPInformation;
import net.sharkfw.asip.ASIPInformationSpace;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPOutMessage;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkAlgebra;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.protocols.ConnectionStatusListener;
import net.sharkfw.system.L;
import net.sharksystem.android.peer.AndroidSharkEngine;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by j4rvis on 01.06.16.
 */
public class WifiDirectBroadcastManager implements Runnable, ConnectionStatusListener {

    private static WifiDirectBroadcastManager _instance = null;
    private Context _context;

    // Thread Messages
    public final static int NEWKNOWLEDGEMSG = 0;
    public final static int NEWPEERMSG = 1;
    public final static int PREPARECONNECTIONMSG = 2;
    public final static int ONCONNECTEDMSG = 3;
    public final static int ONDISCONNECTEDMSG = 4;
    private boolean _isDisconnected;
    private boolean _hasNewPeers;
    private boolean _hasNewKnowledge;
    private String _groupOwnerAddress;
    private WifiP2pInfo _connectionInfo;
    private WifiP2pGroup _connectedGroup;

    // Status
    private enum WifiState {
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        TCPCLOSED,
        DISCONNECTED
    }
    private WifiState _wifiState = WifiState.DISCOVERING;

    private enum ThreadState {
        BLOCKED,
        RUNNING,
        SENDING,
        PAUSED,
        INITIALIZED
    }
    private ThreadState _threadState = ThreadState.INITIALIZED;

    private enum BroadcastState {
        ISOWNER,
        ISSENDERANDOWNER,
        ISCLIENT,
        ISSENDERANDCLIENT,
        NOTINITIALIZED
    }
    private BroadcastState _broadcastState = BroadcastState.NOTINITIALIZED;

    private class LooperThread extends Thread {
        public Handler mHandler;

        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what){
                        case WifiDirectBroadcastManager.NEWKNOWLEDGEMSG:
                            doKnowledge();
                            break;
                        case WifiDirectBroadcastManager.NEWPEERMSG:
                            doPeers();
                            break;
                        case WifiDirectBroadcastManager.PREPARECONNECTIONMSG:
                            doPrepare();
                            break;
                        case WifiDirectBroadcastManager.ONCONNECTEDMSG:
                            L.d("ONCONNECTEDMSG");
                            break;
                        case WifiDirectBroadcastManager.ONDISCONNECTEDMSG:
                            L.d("ONDISCONNECTEDMSG queued");
                            break;
                    }
                }
            };
            Looper.loop();
        }
    }

    LooperThread mLooperThread;

    // Runnables

    private void doKnowledge(){
        L.d("NEWKNOWLEDGEMSG queued", this);
    }
    private void doPeers(){
        L.d("NEWPEERSMSG queued", this);
        try {
            L.d("Sleep 10 seconds", this);
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queueThread(WifiDirectBroadcastManager.PREPARECONNECTIONMSG);
    }

    private void doPrepare(){
        L.d("PREPARECONNECTIONMSG queued", this);
        try {
            L.d("Sleep 15 seconds", this);
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queueThread(WifiDirectBroadcastManager.ONCONNECTEDMSG);
    }

    // Lists and maps
    private CopyOnWriteArrayList<WifiP2pDevice> _devices = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<WifiDirectPeer> _peers = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<ASIPKnowledge, CopyOnWriteArrayList<PeerSemanticTag>> _mapWithPeers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ASIPKnowledge, Integer> _mapWithCounts = new ConcurrentHashMap<>();
    // Temp Lists and Maps
    private ArrayList<WifiP2pDevice> _tempDevices = new ArrayList<>();
    private ArrayList<WifiDirectPeer> _tempPeers = new ArrayList<>();
    private HashMap<ASIPKnowledge, ArrayList<PeerSemanticTag>> _tempMapWithPeers = new HashMap<>();
    private HashMap<ASIPKnowledge, Integer> _tempMapWithCounts = new HashMap<>();

    private boolean _isSender = false;
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
    private boolean _messageSent;
    private int _connectingLimit = 3;
    private int _connectingCounter = 0;

    public static synchronized WifiDirectBroadcastManager getInstance(Context context){
        if(_instance == null){
            _instance = new WifiDirectBroadcastManager(context);
        }
        return _instance;
    }

    public WifiDirectBroadcastManager(Context context) {
        _context = context;

        mLooperThread = new LooperThread();
        mLooperThread.start();

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

    // Thread methods

    public void onDestroy(){
        mLooperThread.mHandler.getLooper().quit();
    }

    private void queueThread(int msgId){
        if (mLooperThread.mHandler != null) {
            Message msg = mLooperThread.mHandler.obtainMessage(msgId);
            mLooperThread.mHandler.sendMessage(msg);
        }
    }

    // Setter and Adder

    public void setEngine(AndroidSharkEngine engine){
        _engine = engine;
    }

    public void setWifiDirectManager(WifiDirectManager manager){
        _manager = manager;
    }

    // Acts also as Listener

    public synchronized void setDevices(CopyOnWriteArrayList<WifiP2pDevice> devices){
        _devices = devices;
    }

    public synchronized void addKnowledge(ASIPKnowledge knowledge, PeerSemanticTag sender){

        if(_mapWithPeers.containsKey(knowledge)){
            return;
        } else {
            if(sender!=null){
                L.d("Add a received knowledge to the pool", this);
                try {
                    knowledge.addInformation(ASIPSerializer.serializeTag(sender).toString(), _peerSpace);
                } catch (JSONException | SharkKBException e) {
                    e.printStackTrace();
                }
            }
            CopyOnWriteArrayList knowledgePeers = getKnowledgePeers(knowledge);
            _mapWithPeers.put(knowledge, knowledgePeers);
            _mapWithCounts.put(knowledge, 0);
            L.d("Add a received knowledge to the pool", this);
        }

        _lastAddedKnowledge = knowledge;

//        queueThread(WifiDirectBroadcastManager.NEWKNOWLEDGEMSG);

//        if(!_wifiState.equals(WifiState.CONNECTING) ||
//                !_wifiState.equals(WifiState.CONNECTED) ||
//                !_threadState.equals(ThreadState.RUNNING)){
//            _handler.post(this);
//        }
        startThread();
    }

    public synchronized void addKnowledge(ASIPKnowledge knowledge){
        addKnowledge(knowledge, null);
    }

    public synchronized void setPeers(CopyOnWriteArrayList<WifiDirectPeer> peers){
        _peers = peers;
//        queueThread(WifiDirectBroadcastManager.NEWPEERMSG);
    }

    // Update Lists and Maps with temp. Lists and Maps

    private void updateLists(){

    }

    private boolean startThread(){
//        L.d("ThreadState:" + _threadState, this);
        if(_threadState == ThreadState.PAUSED || _threadState == ThreadState.INITIALIZED){
            _handler.post(this);
            return true;
        }
        return false;
    }

    // Getter and Picker

    public CopyOnWriteArrayList<WifiDirectPeer> getAvailablePeers(){
        CopyOnWriteArrayList<WifiDirectPeer> availablePeers = new CopyOnWriteArrayList<>();

        Iterator<WifiDirectPeer> iterator = _peers.iterator();
        while (iterator.hasNext()){
            WifiDirectPeer current = iterator.next();
            if(current.status == WifiP2pDevice.AVAILABLE && current.getLastUpdated() > 0){
                availablePeers.add(current);
            }
        }

        return availablePeers;
    }

    private CopyOnWriteArrayList<PeerSemanticTag> getKnowledgePeers(ASIPKnowledge knowledge){
        CopyOnWriteArrayList<PeerSemanticTag> temp = new CopyOnWriteArrayList<>();
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

    private WifiDirectPeer pickPeer(ASIPKnowledge knowledge){
        if(_mapWithPeers.containsKey(knowledge)){
//            L.d("Knowledge found in map.", this);
            CopyOnWriteArrayList<PeerSemanticTag> peers = _mapWithPeers.get(knowledge);

            CopyOnWriteArrayList availablePeers = getAvailablePeers();
//            L.d("Size of availablePeers: " + availablePeers.size(), this);
//            Collections.sort(availablePeers);
            Iterator<WifiDirectPeer> knownPeers = availablePeers.iterator();
            while (knownPeers.hasNext()){
                WifiDirectPeer peer = knownPeers.next();

//                L.d("Peers size: " + peers.size(), this);

                if(peers.isEmpty()){
//                    L.d("No peers to check against. return first peer", this);
                    return peer;
                }

                PeerSemanticTag tag = peer.getmTag();
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

    // Thread Itself

    @Override
    public void run() {

        _threadState = ThreadState.RUNNING;

//        int threadRuns = 0;

//        _isConnected = false;
//        _isConnecting = false;
//        _isDisconnected = false;
//        _isRunning = false;
//        _isOwner = false;
//        _isSender = false;

//        _hasNewPeers = false;
//        _hasNewKnowledge = false;
//        L.d("WifiState: " + _wifiState, this);

        while (true){
//            L.d("loop", this);
            if(_wifiState == WifiState.CONNECTING) {
//                // TODO
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                threadRuns++;
                return;
            }
            if(_wifiState == WifiState.DISCOVERING){
                // We are not connected
//                L.d("We are not connected", this);
//                if(_hasNewKnowledge || _hasNewPeers){
//                    updateLists();
//                    // Check what's new and set booleans to false;
//                }
                // Do we have knowldege?
                if(_mapWithPeers.isEmpty()){
                    // We have no knowledge to send
                    break;
                }
                // Do we have peers?
                if(getAvailablePeers().isEmpty()){
                    //We have no Peers to send knowledge to.
                    break;
                }
                // We have k and peers
                // Go pick a knowledge and check if we can send it to one of our known peers
                WifiDirectPeer peer = null;

                Iterator<ASIPKnowledge> iterator = _mapWithPeers.keySet().iterator();
                while (iterator.hasNext() && peer == null) {
                    ASIPKnowledge knowledge = iterator.next();
                    if (_mapWithCounts.get(knowledge) >= 5) continue;
                    peer = pickPeer(knowledge);
                    if (peer != null) {
                        _knowledgeToSend = knowledge;
                    }
                }
                // Did we found a knowledge?
                if (_knowledgeToSend == null) {
                    L.d("No Knowledge to send", this);
                    break;
                }

                // And did we found a fitting peer?
                if (peer == null) {
                    L.d("Did not find any suitable peer", this);
                    break;
                }

                // Okay we have a peer and knowledge
                // Now check if we can update our lists
//                if(_hasNewKnowledge || _hasNewPeers){
//                    updateLists();
//                    // Check what's new and set booleans to false;
//                }

                // Now check if the Peer is really still AVAILABLE
                if(_devices.contains(peer)){
                    int i = _devices.indexOf(peer);
                    if(!(_devices.get(i).status == WifiP2pDevice.AVAILABLE)){
                        // Peer is not available anymore
                        break;
                    }
                } else {
                    // Peer is not in the list of devices.
                    break;
                }

                // Okay now really start sending this shi...

                // Init the connection and set us to CONNECTING
                _manager.connect(peer);
                _wifiState = WifiState.CONNECTING;

                _lastConnectedPeer = peer;
                // Remove the lastUpdated from the currentPeer
                _peers.remove(_lastConnectedPeer);
                _lastConnectedPeer.resetUpdated();
                _peers.add(_lastConnectedPeer);

                break;
                // Do not return... let it run until the state changes

            }
            else if(_wifiState == WifiState.CONNECTED){
                // We are connected!!!!
                L.d("We are connected", this);
                if(_broadcastState == BroadcastState.ISOWNER || _broadcastState == BroadcastState.ISSENDERANDOWNER){
                    try {
                        _engine.startTCP(7071, _knowledgeToSend);
                        _threadState = ThreadState.BLOCKED;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ASIPKnowledge knowledge = null;
                    if (_broadcastState == BroadcastState.ISSENDERANDCLIENT) {
                        knowledge = _knowledgeToSend;
                    }

                    final ASIPKnowledge finalKnowledge = knowledge;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            _threadState = ThreadState.SENDING;
                            PeerSemanticTag tcpTag = InMemoSharkKB.createInMemoPeerSemanticTag("Receiver", "www.receiver.de", "tcp://" + _groupOwnerAddress + ":7071");
                            ASIPOutMessage msg = _engine.createASIPOutMessage(tcpTag.getAddresses(), tcpTag);
                            msg.insert(finalKnowledge);
                        }
                    }).start();

                    // We reached the end so we probably sent the msg
                    // leave the Thread
                    return;
                }
                return;
            }
            else if(_wifiState == WifiState.TCPCLOSED){

                if(_broadcastState == BroadcastState.ISOWNER || _broadcastState == BroadcastState.ISSENDERANDOWNER){
                    L.d("I'm the OWNER so shutdown the server.", this);
                    _engine.stopTCP();
                }
                _manager.disconnect();
                _threadState = ThreadState.PAUSED;
                break;
            }
            else if(_wifiState == WifiState.DISCONNECTED){
                // Add peer to sent knowledge
                L.d("We are disconnected", this);
                if(_knowledgeToSend!=null){
                    CopyOnWriteArrayList<PeerSemanticTag> peerSemanticTags = _mapWithPeers.get(_knowledgeToSend);
                    peerSemanticTags.add(_lastConnectedPeer.getmTag());
                    L.d("Peer size:" + peerSemanticTags.size(), this);
                    _mapWithPeers.put(_knowledgeToSend, peerSemanticTags);
                    int count = _mapWithCounts.get(_knowledgeToSend);
                    L.d("Count before:" + count, this);
                    _mapWithCounts.put(_knowledgeToSend, ++count);
                    L.d("Count after: " + _mapWithCounts.get(_knowledgeToSend), this);
                    _knowledgeToSend=null;
                }
                _connectedGroup = null;
                _connectionInfo = null;
                _groupOwnerAddress = "";

                _wifiState = WifiState.DISCOVERING;
                // Restart the Thread
                _threadState = ThreadState.PAUSED;
                startThread();
                return;
            }
        }
//        L.d("blob", this);
        _threadState = ThreadState.PAUSED;


//        while (true) {
//            if (_threadState.equals(ThreadState.RUNNING)) return;
//            _threadState = ThreadState.RUNNING;
//
//            if (_wifiState.equals(WifiState.CONNECTED)) {
//                //            L.d("Currently connected to a device", this);
//                //            _handler.postDelayed(this, THREAD_TIMEOUT*3);
//                _threadState = ThreadState.BLOCKED;
//                return;
//            } else if (_wifiState.equals(WifiState.CONNECTING) && _threadState.equals(ThreadState.RUNNING)) {
//                if (++_connectingCounter > _connectingLimit) {
//                    _wifiState = WifiState.DISCOVERING;
//                    _connectingCounter = 0;
//                }
//                _threadState = ThreadState.PAUSED;
//                return;
//            } else {
//                if (_mapWithPeers.isEmpty()) {
//                    //                L.d("Map is empty", this);
//                    _threadState = ThreadState.PAUSED;
//                    return;
//                } else {
//                    if (getAvailablePeers().isEmpty()) {
//                        L.d("No available Peers", this);
//                        _threadState = ThreadState.PAUSED;
//                        return;
//                    } else {
//
//                        //                    L.d("okay try to send the Knowledge", this);
//                        //                    ArrayList<ASIPKnowledge> checkedKnowledges = new ArrayList<>();
//                        //                    ArrayList<WifiDirectPeer> checkedPeers = new ArrayList<>();
//                        WifiDirectPeer peer = null;
//
//                        Iterator<ASIPKnowledge> iterator = _mapWithPeers.keySet().iterator();
//                        while (iterator.hasNext() && peer == null) {
//                            ASIPKnowledge knowledge = iterator.next();
//                            if (_mapWithCounts.get(knowledge) >= 5) continue;
//                            peer = pickPeer(knowledge);
//                            if (peer != null) {
//                                _knowledgeToSend = knowledge;
//                            }
//                        }
//
//                        if (_knowledgeToSend == null) {
//                            L.d("No Knowledge to send", this);
//                            _threadState = ThreadState.PAUSED;
//                            return;
//                        }
//
//                        //                    L.d("Peer picked!", this);
//                        if (peer == null) {
//                            L.d("Did not find any suitable peer", this);
//                            _threadState = ThreadState.PAUSED;
//                            return;
//                        }
//                        //                    else if(peer.equals(_lastConnectedPeer)){
//                        //                        L.d("Do not try to connect to last Peer");
//                        //                        return;
//                        //                    }
//
//                        L.d("Connecting to Peer", this);
//
//                        initConnection(peer);
//                        _lastConnectedPeer = peer;
//                        //                    _isSender = true;
//
//                        if (_lastConnectedPeer != null) {
//                            _peers.remove(_lastConnectedPeer);
//                            //                        int i = _peers.indexOf(_lastConnectedPeer);
//                            _lastConnectedPeer.resetUpdated();
//                            _peers.add(_lastConnectedPeer);
//                        }
//
//                        _handler.postDelayed(this, THREAD_TIMEOUT * 3);
//                        _threadState = ThreadState.PAUSED;
//                    }
//                }
//            }
//            _threadState = ThreadState.PAUSED;
//        }
    }

    // Listener

    public void onConnectionEstablished(WifiP2pInfo info, final WifiP2pGroup group){

        // Oh yeah we're connected
        // Check if we have an info and a group
        // Otherwise try to disconnect and reset the States
        if(info==null || group == null){
            _manager.disconnect();
            // TODO Resetting the States..
        }

        _connectionInfo = info;
        _connectedGroup = group;

        _wifiState = WifiState.CONNECTED;

        // SET BROADCAST STATE
        // SET the groupOwnerAddress
        // And run the thread again

        _groupOwnerAddress = info.groupOwnerAddress.getHostAddress();

        if(info.isGroupOwner) {
            _broadcastState = BroadcastState.ISOWNER;
            if(_knowledgeToSend!=null){
                _broadcastState = BroadcastState.ISSENDERANDOWNER;
            }
        } else {
            _broadcastState = BroadcastState.ISCLIENT;
            if(_knowledgeToSend!=null){
                _broadcastState = BroadcastState.ISSENDERANDCLIENT;
            }
        }
        _threadState = ThreadState.PAUSED;
        startThread();
//        _threadState = ThreadState.BLOCKED;
//
//        final String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
//        final WifiDirectPeer owner = new WifiDirectPeer(group.getOwner(), null);
//
//
//        if(_broadcastState.equals(BroadcastState.ISOWNER) || _broadcastState.equals(BroadcastState.ISSENDERANDOWNER)){
//            try {
//                _engine.startTCP(7071, _knowledgeToSend);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else {
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            ASIPKnowledge knowledge = null;
//            if(_broadcastState.equals(BroadcastState.ISSENDERANDCLIENT)){
//               knowledge = _knowledgeToSend;
//            }
//
//            final ASIPKnowledge finalKnowledge = knowledge;
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    PeerSemanticTag tcpTag = InMemoSharkKB.createInMemoPeerSemanticTag("Receiver", "www.receiver.de", "tcp://"+groupOwnerAddress+":7071");
//                    ASIPOutMessage msg = _engine.createASIPOutMessage(tcpTag.getAddresses(), tcpTag);
//                    msg.insert(finalKnowledge);
////                    _threadState = ThreadState.PAUSED;
////                    _messageSent = true;
//                }
//            }).start();
//        }
    }

    public void onDisconnected(){
        _wifiState = WifiState.DISCONNECTED;
        _broadcastState = BroadcastState.NOTINITIALIZED;

        // Run the THREAD
        startThread();

//        _threadState = ThreadState.PAUSED;
//        L.d("Wifi network closed.", this);
//        L.d(_threadState.toString(), this);
//        L.d(_wifiState.toString(), this);
//        L.d(_broadcastState.toString(), this);

        // Add peer to send knowledge
//        if(_knowledgeToSend!=null){
//            ArrayList<PeerSemanticTag> peerSemanticTags = _mapWithPeers.get(_knowledgeToSend);
//            peerSemanticTags.add(_lastConnectedPeer.getmTag());
//            L.d("Peer size:" + peerSemanticTags.size(), this);
//            _mapWithPeers.put(_knowledgeToSend, peerSemanticTags);
//            int count = _mapWithCounts.get(_knowledgeToSend);
//            L.d("Count before:" + count, this);
//            _mapWithCounts.put(_knowledgeToSend, ++count);
//            L.d("Count after: " + _mapWithCounts.get(_knowledgeToSend), this);
////            _messageSent=false;
//            _knowledgeToSend=null;
//        }

    }

    @Override
    public void connectionClosed() {
        _threadState = ThreadState.PAUSED;
        L.d("TCP Connection closed", this);
        _wifiState = WifiState.TCPCLOSED;
        // run the THREAD

        startThread();

//        if(_broadcastState == BroadcastState.ISOWNER || _broadcastState == BroadcastState.ISSENDERANDOWNER){
//            L.d("I'm the OWNER so shutdown the server.", this);
//            _engine.stopTCP();
//        }
//        _manager.disconnect();
    }

    public void notifyUpdate(){
        boolean started = startThread();
//        L.d("Update + StartThread:" + started, this);
//        L.d(_threadState.toString(), this);
//        L.d(_wifiState.toString(), this);
//        L.d(_broadcastState.toString(), this);
//        if(!_wifiState.equals(WifiState.CONNECTED) ||
//                !_wifiState.equals(WifiState.CONNECTING) ||
//                !_threadState.equals(ThreadState.BLOCKED) ||
//                !_threadState.equals(ThreadState.RUNNING)){
//            _handler.post(this);
//        }
    }
}
