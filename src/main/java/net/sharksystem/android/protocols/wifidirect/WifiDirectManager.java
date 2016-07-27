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

    HashMap<String, String> mRecordMap = new HashMap<>();

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

            String serializedTopic = null;
            String serializedType = null;
            String serializedSender = null;
            String serializedApprovers = null;
            String serializedReceiver = null;
            String serializedLocation = null;
            String serializedTime = null;
            int direction = -1;
            String name;

            try {

                serializedTopic = ASIPSerializer.serializeSTSet(space.getTopics()).toString();
                serializedType = ASIPSerializer.serializeSTSet(space.getTypes()).toString();
                serializedSender = ASIPSerializer.serializeTag(space.getSender()).toString();
                serializedApprovers = ASIPSerializer.serializeSTSet(space.getApprovers()).toString();
                serializedReceiver = ASIPSerializer.serializeSTSet(space.getReceivers()).toString();
                serializedLocation = ASIPSerializer.serializeSTSet(space.getLocations()).toString();
                serializedTime = ASIPSerializer.serializeSTSet(space.getTimes()).toString();
                direction = space.getDirection();

            } catch (SharkKBException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            name = space.getSender().getName();
            if(name.isEmpty()) {
                name = "A";
            }

            mRecordMap.put(NAME_RECORD, name);
            mRecordMap.put(TOPIC_RECORD, serializedTopic);
            mRecordMap.put(TYPE_RECORD, serializedType);
            mRecordMap.put(SENDER_RECORD, serializedSender);
            mRecordMap.put(APPROVERS_RECORD, serializedApprovers);
            mRecordMap.put(RECEIVER_RECORD, serializedReceiver);
            mRecordMap.put(LOCATION_RECORD, serializedLocation);
            mRecordMap.put(TIME_RECORD, serializedTime);
            mRecordMap.put(DIRECTION_RECORD, String.valueOf(direction));

            mServiceInfo =
                    WifiP2pDnsSdServiceInfo.newInstance("_sbc", "_presence._tcp", mRecordMap);

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

        if(!txtRecordMap.containsKey(WifiDirectManager.NAME_RECORD) ||
                !txtRecordMap.containsKey(WifiDirectManager.SENDER_RECORD)){
            return;
        }

        ASIPInterest interest = new InMemoInterest();
        String addr = "WIFI://" + srcDevice.deviceAddress;

        STSet topics = null;
        STSet types = null;
        PeerSemanticTag sender = null;
        PeerSTSet approver = null;
        PeerSTSet receiver = null;
        SpatialSTSet locations = null;
        TimeSTSet times = null;
        int direction = -1;

        try {
            topics = ASIPSerializer.deserializeAnySTSet(null,
                    String.valueOf(txtRecordMap.get(WifiDirectManager.TOPIC_RECORD)));
            types = ASIPSerializer.deserializeAnySTSet(null,
                    String.valueOf(txtRecordMap.get(WifiDirectManager.TYPE_RECORD)));
            sender = ASIPSerializer.deserializePeerTag(
                    String.valueOf(txtRecordMap.get(WifiDirectManager.SENDER_RECORD)));
            approver = ASIPSerializer.deserializePeerSTSet(null,
                    String.valueOf(txtRecordMap.get(WifiDirectManager.APPROVERS_RECORD)));
            receiver = ASIPSerializer.deserializePeerSTSet(null,
                    String.valueOf(txtRecordMap.get(WifiDirectManager.RECEIVER_RECORD)));
            locations = ASIPSerializer.deserializeSpatialSTSet(null,
                    String.valueOf(txtRecordMap.get(WifiDirectManager.LOCATION_RECORD)));
            times = ASIPSerializer.deserializeTimeSTSet(null,
                    String.valueOf(txtRecordMap.get(WifiDirectManager.TIME_RECORD)));
            direction = Integer.getInteger(txtRecordMap.get(WifiDirectManager.DIRECTION_RECORD));
        } catch (SharkKBException e) {
            e.printStackTrace();
        }

        // Set Wifi-Address to sender

        sender.addAddress(addr);

        interest.setTopics(topics);
        interest.setTypes(types);
        interest.setSender(sender);
        interest.setApprovers(approver);
        interest.setReceivers(receiver);
        interest.setLocations(locations);
        interest.setTimes(times);
        interest.setDirection(direction);

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
