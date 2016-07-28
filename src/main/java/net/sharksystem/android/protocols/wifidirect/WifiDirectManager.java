package net.sharksystem.android.protocols.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSTSet;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.SpatialSTSet;
import net.sharkfw.knowledgeBase.TimeSTSet;
import net.sharkfw.knowledgeBase.inmemory.InMemoInterest;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharksystem.android.Application;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by j4rvis on 22.07.16.
 */
public class WifiDirectManager
        extends BroadcastReceiver
        implements WifiP2pManager.DnsSdTxtRecordListener,
        WifiP2pManager.ConnectionInfoListener, Runnable {

    private boolean mIsReceiverRegistered;
    private boolean mIsDiscovering;
    private WifiP2pDnsSdServiceInfo mServiceInfo;

    private enum WIFI_STATE {
        INIT,
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        NONE
    }

    private WIFI_STATE mState = WIFI_STATE.NONE;

    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private WifiDirectStreamStub mStub = null;

    private Context mContext = null;

    // Interfaces
    //
    //

    interface WifiDirectPeerListener {
        void onNewInterest(ASIPInterest interest);
        void onNewPeer(PeerSemanticTag peers);
    }
    interface WifiDirectNetworkListener {
        void onNetworkCreated(List<PeerSemanticTag> connectedPeers);
        void onNetworkDestroyed();
    }

    // Instance
    //
    //

    private static WifiDirectManager sInstance = null;

    static {
        sInstance = new WifiDirectManager();
    }

    private WifiDirectManager() {
        mContext = Application.getAppContext();
        mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);

        mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);
        mManager.setDnsSdResponseListeners(mChannel, null, this);
    }

    public static WifiDirectManager getInstance() {
        return sInstance;
    }

    // Setter
    //
    //

    private List<WifiDirectPeerListener> mPeerListeners = new ArrayList<>();
    private List<WifiDirectNetworkListener> mNetworkListeners = new ArrayList<>();

    public void setWifiDirectStreamStub(WifiDirectStreamStub stub){
        mStub = stub;
    }

    public void addPeerListener(WifiDirectPeerListener listener){
        if(!mPeerListeners.contains(listener)){
            mPeerListeners.add(listener);
        }
    }

    public void addNetworkListener(WifiDirectNetworkListener listener){
        if(!mNetworkListeners.contains(listener)){
            mNetworkListeners.add(listener);
        }
    }

    // Getter
    //
    //

    public WIFI_STATE getState(){ return mState; }

    // Private methods
    //
    //



    // Public methods
    //
    //

    HashMap<String, String> map = new HashMap<>();

    final static String TOPIC_RECORD = "TO";
    final static String TYPE_RECORD = "TY";
    final static String SENDER_RECORD = "SE";
    final static String APPROVERS_RECORD = "AP";
    final static String RECEIVER_RECORD = "RE";
    final static String LOCATION_RECORD = "LO";
    final static String TIME_RECORD = "TI";
    final static String DIRECTION_RECORD = "DI";

    final static String NAME_RECORD = "NAME";


    public void startAdvertising(ASIPSpace space){

        if(!mIsDiscovering){

            if(mIsReceiverRegistered){
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                mContext.registerReceiver(this, intentFilter);
                mIsReceiverRegistered = true;
            }

            mManager.clearLocalServices(mChannel, new WifiActionListener("Clear LocalServices"));

            HashMap<String, String> map = interest2RecordMap((ASIPInterest) space);

            mServiceInfo =
                    WifiP2pDnsSdServiceInfo.newInstance("_sbc", "_presence._tcp", map);

            mManager.addLocalService(mChannel, mServiceInfo,
                    new WifiActionListener("Add LocalService"));

            mManager.clearServiceRequests(mChannel,
                    new WifiActionListener("Clear ServiceRequests"));

            WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

            mManager.addServiceRequest(mChannel, serviceRequest,
                    new WifiActionListener("Add ServiceRequest"));

            mHandler.post(this);

            mState = WIFI_STATE.DISCOVERING;
            mIsDiscovering = true;

        }

    }

    public void stopAdvertising(){
        if(mIsDiscovering){
            mHandler.removeCallbacks(this);
            mManager.clearServiceRequests(mChannel,
                    new WifiActionListener("Clear ServiceRequests"));
            mManager.removeLocalService(mChannel, mServiceInfo,
                    new WifiActionListener("Remove LocalService"));
            mManager.clearLocalServices(mChannel,
                    new WifiActionListener("Clear LocalServices"));

            if(mIsReceiverRegistered){
                mContext.unregisterReceiver(this);
                mIsReceiverRegistered = false;
            }

            mState = WIFI_STATE.INIT;
            mIsDiscovering = false;
        }

        if(mIsReceiverRegistered){
            mContext.unregisterReceiver(this);
            mIsReceiverRegistered = false;
        }

    }

    public HashMap<String, String> interest2RecordMap(ASIPInterest space){

        HashMap<String, String> map = new HashMap<>();

        String serializedTopic = "";
        String serializedType = "";
        String serializedSender = "";
        String serializedApprovers = "";
        String serializedReceiver = "";
        String serializedLocation = "";
        String serializedTime = "";
        int direction = -1;
        String name = "";

        try {

            if(space.getTopics() != null){
                serializedTopic = ASIPSerializer.serializeSTSet(space.getTopics()).toString();
            }
            if(space.getTypes() == null){
                serializedType = ASIPSerializer.serializeSTSet(space.getTypes()).toString();
            }
            if(space.getSender() == null){
                serializedSender = ASIPSerializer.serializeTag(space.getSender()).toString();
                name = space.getSender().getName();
                if(name.isEmpty()) {
                    name = "A";
                }
            }
            if(space.getApprovers() == null){
                serializedApprovers = ASIPSerializer.serializeSTSet(space.getApprovers()).toString();
            }
            if(space.getReceivers() == null){
                serializedReceiver = ASIPSerializer.serializeSTSet(space.getReceivers()).toString();
            }
            if(space.getLocations() == null){
                serializedLocation = ASIPSerializer.serializeSTSet(space.getLocations()).toString();
            }
            if(space.getTimes() == null){
                serializedTime = ASIPSerializer.serializeSTSet(space.getTimes()).toString();
            }
            if(space.getDirection() < 0){
                direction = space.getDirection();
            }

        } catch (SharkKBException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        map.put(NAME_RECORD, name);
        map.put(TOPIC_RECORD, serializedTopic);
        map.put(TYPE_RECORD, serializedType);
        map.put(SENDER_RECORD, serializedSender);
        map.put(APPROVERS_RECORD, serializedApprovers);
        map.put(RECEIVER_RECORD, serializedReceiver);
        map.put(LOCATION_RECORD, serializedLocation);
        map.put(TIME_RECORD, serializedTime);
        map.put(DIRECTION_RECORD, String.valueOf(direction));

        return map;
    }

    public ASIPInterest recordMap2Interest(Map<String, String> map) throws SharkKBException {

        ASIPInterest interest = InMemoSharkKB.createInMemoASIPInterest();

        interest.setTopics(InMemoSharkKB.createInMemoSTSet());
        interest.setTypes(InMemoSharkKB.createInMemoSTSet());
        interest.setApprovers(InMemoSharkKB.createInMemoPeerSTSet());
        interest.setReceivers(InMemoSharkKB.createInMemoPeerSTSet());
        interest.setLocations(InMemoSharkKB.createInMemoSpatialSTSet());
        interest.setTimes(InMemoSharkKB.createInMemoTimeSTSet());

        if(map.containsKey(WifiDirectManager.TOPIC_RECORD)){
            String record = map.get(WifiDirectManager.TOPIC_RECORD);
            interest.getTopics().merge(ASIPSerializer.deserializeSTSet(record));
        }
        if(map.containsKey(WifiDirectManager.TYPE_RECORD)){
            String record = map.get(WifiDirectManager.TYPE_RECORD);
            interest.getTypes().merge(ASIPSerializer.deserializeSTSet(record));
        }
        if(map.containsKey(WifiDirectManager.SENDER_RECORD)){
            String record = map.get(WifiDirectManager.SENDER_RECORD);
            interest.setSender(ASIPSerializer.deserializePeerTag(record));
        }
        if(map.containsKey(WifiDirectManager.APPROVERS_RECORD)){
            String record = map.get(WifiDirectManager.APPROVERS_RECORD);
            interest.getApprovers().merge(ASIPSerializer.deserializePeerSTSet(null, record));
        }
        if(map.containsKey(WifiDirectManager.RECEIVER_RECORD)){
            String record = map.get(WifiDirectManager.RECEIVER_RECORD);
            interest.getReceivers().merge(ASIPSerializer.deserializePeerSTSet(null, record));
        }
        if(map.containsKey(WifiDirectManager.LOCATION_RECORD)){
            String record = map.get(WifiDirectManager.LOCATION_RECORD);
            interest.getLocations().merge(ASIPSerializer.deserializeSpatialSTSet(null, record));
        }
        if(map.containsKey(WifiDirectManager.TIME_RECORD)){
            String record = map.get(WifiDirectManager.TIME_RECORD);
            interest.getTimes().merge(ASIPSerializer.deserializeTimeSTSet(null, record));
        }
        if(map.containsKey(WifiDirectManager.DIRECTION_RECORD)){
            int record = Integer.getInteger(map.get(WifiDirectManager.DIRECTION_RECORD));
            interest.setDirection(record);
        }
        return interest;
    }

    public boolean isValidRecordMap(Map<String, String> map){
        if(map.containsKey(WifiDirectManager.NAME_RECORD)
                && map.containsKey(WifiDirectManager.TOPIC_RECORD)){
            return true;
        } else {
            return false;
        }
    }

    public void connect(List<PeerSemanticTag> peers){

    }

    public void connect(PeerSemanticTag peer){
        final WifiP2pConfig config = new WifiP2pConfig();
        for(String addr : peer.getAddresses()){
            if(addr.startsWith("WIFI://")){
                config.deviceAddress = addr;
            }
        }
        config.wps.setup = WpsInfo.PBC;
        mManager.connect(mChannel, config, new WifiActionListener("Init Connection"));
    }


    // Implemented methods
    //
    //

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName,
                                          Map<String, String> txtRecordMap,
                                          WifiP2pDevice srcDevice) {

        if(srcDevice == null || txtRecordMap.isEmpty()) return;

        if(isValidRecordMap(txtRecordMap)){
            return;
        }

        String addr = "WIFI://" + srcDevice.deviceAddress;

        ASIPInterest interest = null;
        try {
            interest = recordMap2Interest(txtRecordMap);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }

        interest.getSender().addAddress(addr);

        PeerSemanticTag sender = interest.getSender();

        // Inform Listener

        for(WifiDirectPeerListener listener : mPeerListeners){
            listener.onNewPeer(sender);
            listener.onNewInterest(interest);
        }

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(mManager == null)
            return;

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            } else {
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()){
                mState = WIFI_STATE.CONNECTED;
                mManager.requestConnectionInfo(mChannel, this);
            } else {
                mState = WIFI_STATE.DISCOVERING;
            }
        }
    }

    private Handler mHandler = new Handler();

    @Override
    public void run() {

    }

}
