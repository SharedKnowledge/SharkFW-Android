package net.sharksystem.android.peer;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.SharkStub;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPOutMessage;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSTSet;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.SpatialSTSet;
import net.sharkfw.knowledgeBase.SpatialSemanticTag;
import net.sharkfw.knowledgeBase.SystemPropertyHolder;
import net.sharkfw.knowledgeBase.TimeSTSet;
import net.sharkfw.knowledgeBase.TimeSemanticTag;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.peer.J2SEAndroidSharkEngine;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.Stub;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkNotSupportedException;
import net.sharksystem.android.protocols.nfc.NfcMessageStub;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;
import net.sharksystem.android.protocols.wifidirect.WifiDirectStreamStub;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidSharkEngine extends J2SEAndroidSharkEngine implements KPNotifier {
    Context context;
    WeakReference<Activity> activityRef;
    Stub currentStub;
    private ASIPSpace mSpace;
    private String mName;

    private HashMap<PeerSemanticTag, Long> mNearbyPeers = new HashMap<>();

    public AndroidSharkEngine(Context context) {
        super();
        this.activateASIP();
        this.context = context;
    }

    public AndroidSharkEngine(Activity activity) {
        super();
        this.activateASIP();
        this.context = activity.getApplicationContext();
        this.activityRef = new WeakReference<>(activity);
    }

    /*
     * Wifi Direct methods
     * @see net.sharkfw.peer.SharkEngine#createWifiDirectStreamStub(net.sharkfw.kep.KEPStub)
     */
    protected Stub createWifiDirectStreamStub(SharkStub kepStub) throws SharkProtocolNotSupportedException {
        if (currentStub == null) {
            currentStub = new WifiDirectStreamStub(context, this, mSpace, mName);
            currentStub.setHandler((RequestHandler) kepStub);
        }
        return currentStub;
    }

    @Override
    public void startWifiDirect() throws SharkProtocolNotSupportedException, IOException {
//        this.createWifiDirectStreamStub(this.getAsipStub()).start();
        this.createWifiDirectStreamStub(this.getAsipStub()).start();
//        offerInterest("TestTopic");
    }

    public void stopWifiDirect() throws SharkProtocolNotSupportedException {
        currentStub.stop();
    }

    @Override
    protected Stub createNfcStreamStub(SharkStub stub) throws SharkProtocolNotSupportedException {
        if (currentStub == null) {
            currentStub = new NfcMessageStub(context, activityRef);
            currentStub.setHandler((RequestHandler) stub);
        }
        return currentStub;
    }

    @Override
    public void startNfc() throws SharkProtocolNotSupportedException, IOException {
        this.createNfcStreamStub(this.getAsipStub()).start();
    }

    @Override
    public void stopNfc() throws SharkProtocolNotSupportedException {
        this.createNfcStreamStub(this.getAsipStub()).stop();
    }

    @Override
    protected Stub createBluetoothStreamStub(SharkStub kepStub) throws SharkProtocolNotSupportedException {
        throw new SharkProtocolNotSupportedException();
    }

    @Override
    public void startBluetooth() throws SharkProtocolNotSupportedException, IOException {
        throw new SharkProtocolNotSupportedException();
    }

    @Override
    public void stopBluetooth() throws SharkProtocolNotSupportedException {
        throw new SharkProtocolNotSupportedException();
    }

    @Override
    public Stub getProtocolStub(int type) throws SharkProtocolNotSupportedException {
        return super.getProtocolStub(type);
        //TODO this function is called by the parent but the parent function itself look likes a big mess
        // and it does not look like it is designed to work with start/stop methods.
//        return currentStub;
    }

    public void offerInterest(String topic, String name){

        if(TextUtils.isEmpty(topic))
           topic = "Dummy Interesse";

        STSet set = InMemoSharkKB.createInMemoSTSet();
        ASIPSpace space;
        try {
            set.createSemanticTag(topic, "www."+topic+".sharksystem.net");
            space =  InMemoSharkKB.createInMemoASIPInterest(set, null, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
            mSpace = space;
            mName = name;
//            currentStub.offer(space);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }

    }

    /**
     * Send a Broadcast using the addresses of the nearby Peers.
     * An ASIPOutMessage will be created an depending on the Content(Knowledge || Interest) an expose or an insert
     * will be triggered.
     *
     * @param topic
     * @param receiver the actual receiver of the message
     * @param location
     * @param time
     * @param knowledge
     */
    public void sendBroadcast(final SemanticTag topic, final PeerSemanticTag receiver, final SpatialSemanticTag location, final TimeSemanticTag time, final ASIPKnowledge knowledge){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ASIPOutMessage message = createASIPOutMessage(topic, receiver, location, time);
                message.insert(knowledge);
            }
        }).start();
    }

    /**
     *
     * @param topic
     * @param receiver
     * @param location
     * @param time
     * @param interest
     */
    public void sendBroadcast(final SemanticTag topic, final PeerSemanticTag receiver, final SpatialSemanticTag location, final TimeSemanticTag time, final ASIPInterest interest){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ASIPOutMessage message = createASIPOutMessage(topic, receiver, location, time);
                message.expose(interest);
            }
        }).start();
    }

    public ASIPOutMessage createASIPOutMessage(SemanticTag topic, PeerSemanticTag receiver, SpatialSemanticTag location, TimeSemanticTag time){
        String[] addresses = getNearbyPeerTCPAddresses();
        if (addresses.length <= 0) return null;

        return createASIPOutMessage(addresses, this.getOwner(), receiver, location, time, topic, 10);
    }

    public void sendBroadcast(String text) {
        ((WifiDirectStreamStub) currentStub).sendBroadcast(text);
    }

    public void sendBroadcast(ASIPKnowledge knowledge){
        ((WifiDirectStreamStub) currentStub).sendBroadcast(knowledge);
    }

    @Override
    public void notifyInterestReceived(ASIPInterest asipInterest, ASIPConnection asipConnection) {
        L.d("Hey we received an Intererest", this);

        PeerSemanticTag sender = asipInterest.getSender();
        if(sender != null){
            this.mNearbyPeers.put(sender, System.currentTimeMillis());
        }

    }

    @Override
    public void notifyKnowledgeReceived(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {
    }

    public List<PeerSemanticTag> getNearbyPeers(long millis){
        long currentTime = System.currentTimeMillis();

        ArrayList<PeerSemanticTag> tags = new ArrayList<>();

        for(Map.Entry<PeerSemanticTag, Long> entry : this.mNearbyPeers.entrySet()){
            if(currentTime - entry.getValue() <= millis){
                tags.add(entry.getKey());
            }
        }

        return tags;
    }

    public List<PeerSemanticTag> getNearbyPeers(){
        return getNearbyPeers(1000 * 60);
    }

    public String[] getNearbyPeerTCPAddresses(long millis){
        ArrayList<PeerSemanticTag> peers = (ArrayList) getNearbyPeers();

        ArrayList<String> addressList = new ArrayList<>();


        for(PeerSemanticTag tag : peers){
            String[] peerAddresses = tag.getAddresses();
            for (String address : peerAddresses){
                if (address.startsWith("tcp://")){
                    addressList.add(address);
                }
            }
        }

        String[] addresses = new String[addressList.size()];
        addressList.toArray(addresses);
        return addresses;
    }

    public String[] getNearbyPeerTCPAddresses(){
        return getNearbyPeerTCPAddresses(1000 * 60);
    }
}
