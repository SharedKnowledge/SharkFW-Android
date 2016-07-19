package net.sharksystem.android.peer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.SharkStub;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPMessage;
import net.sharkfw.asip.engine.ASIPOutMessage;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.Interest;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.SpatialSemanticTag;
import net.sharkfw.knowledgeBase.TimeSemanticTag;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.peer.J2SEAndroidSharkEngine;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.Stub;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.nfc.NfcMessageStub;
import net.sharksystem.android.protocols.routing.db.MessageDTO;
import net.sharksystem.android.protocols.wifidirect.WifiDirectStreamStub;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidSharkEngine extends J2SEAndroidSharkEngine implements KPNotifier {
    Context mContext;
    WeakReference<Activity> activityRef;
    Stub currentStub;
    private ASIPSpace mSpace;
    private String mName;

    private HashMap<PeerSemanticTag, Long> mNearbyPeers = new HashMap<>();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AndroidSharkEngine(Context context) {
        super();
        this.activateASIP();
        this.mContext = context;

//        final ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//
//        NetworkRequest.Builder builder = new NetworkRequest.Builder();
//
//        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
//        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
//
//        NetworkRequest networkRequest = builder.build();
//        ConnectivityManager.NetworkCallback networkCallback =
//                new ConnectivityManager.NetworkCallback() {
//                    @Override
//                    public void onAvailable(Network network) {
//                        super.onAvailable(network);
//                        LinkProperties prop = connectivityManager.getLinkProperties(network);
//                        InetAddress addr = prop.getLinkAddresses().get(0).getAddress();
//                        L.d("OnAvailable: " + addr.getHostAddress(), this);
//                    }
//                };
//
//        connectivityManager.requestNetwork(networkRequest, networkCallback);
//        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public AndroidSharkEngine(Context context, String ownerName, String topic) {
        this(context);

        offerInterest(topic, ownerName);
    }

    private String[] getDeviceIPAddresses() {
        ArrayList<String> addressesList = new ArrayList<>();

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (isIPv4) {
                            String ipaddress = sAddr;
                            addressesList.add(ipaddress);
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions

//
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
//                NetworkInterface intf = (NetworkInterface) en.nextElement();
//                for (Enumeration<InetAddress> enumIpAddr = intf
//                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress()) {
//                        String ipaddress = inetAddress.getHostAddress();
//                        addressesList.add(ipaddress);
//                    }
//                }
//            }
//        } catch (SocketException ex) {
//            L.e("Exception in Get IP Address: " + ex.toString(), this);
//        }

        if (addressesList.size() <= 0) return null;
        else {
            String[] addresses = new String[addressesList.size()];
            addressesList.toArray(addresses);
            return addresses;
        }
    }

    public AndroidSharkEngine(Activity activity) {
        super();
        this.activateASIP();
        this.mContext = activity.getApplicationContext();
        this.activityRef = new WeakReference<>(activity);
    }

    /*
     * Wifi Direct methods
     * @see net.sharkfw.peer.SharkEngine#createWifiDirectStreamStub(net.sharkfw.kep.KEPStub)
     */
    protected Stub createWifiDirectStreamStub(SharkStub kepStub) throws SharkProtocolNotSupportedException {
        if (currentStub == null) {
            currentStub = new WifiDirectStreamStub(mContext, this, mSpace, mName);
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
            currentStub = new NfcMessageStub(mContext, activityRef);
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

    public void offerInterest(String topic, String name) {

        if (topic.isEmpty())
            topic = "Dummy Interesse";

        STSet set = InMemoSharkKB.createInMemoSTSet();
        ASIPSpace space;
        try {
            set.createSemanticTag(topic, "www." + topic + ".sharksystem.net");
            space = InMemoSharkKB.createInMemoASIPInterest(set, null, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
            mSpace = space;
            mName = name;
//            currentStub.offer(space);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }

        String[] addresses = getDeviceIPAddresses();

//        L.d("Do i have addresses?" + addresses.length, this);
//
        String[] tcpAddresses = new String[addresses.length];

        int i = 0;
        for (String address : addresses) {
            tcpAddresses[i++] = "tcp://" + address + ":7071";
        }

        String si = mName + topic + System.currentTimeMillis();

        PeerSemanticTag owner = InMemoSharkKB.createInMemoPeerSemanticTag(mName, new String[]{si}, tcpAddresses);

        L.d(owner.getAddresses()[0], this);

        setEngineOwnerPeer(owner);

    }

    /**
     * Send a Broadcast using the addresses of the nearby Peers.
     * An ASIPOutMessage will be created an depending on the Content(Knowledge || Interest) an expose or an insert
     * will be triggered.
     *
     * @param topic
     * @param receiver  the actual receiver of the message
     * @param location
     * @param time
     * @param knowledge
     */
    public void sendBroadcast(final SemanticTag topic, final PeerSemanticTag receiver, final SpatialSemanticTag location, final TimeSemanticTag time, final ASIPKnowledge knowledge) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ASIPOutMessage message = createASIPOutMessage(topic, receiver, location, time);
                message.insert(knowledge);
            }
        }).start();
    }

    /**
     * @param topic
     * @param receiver
     * @param location
     * @param time
     * @param interest
     */
    public void sendBroadcast(final SemanticTag topic, final PeerSemanticTag receiver, final SpatialSemanticTag location, final TimeSemanticTag time, final ASIPInterest interest) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ASIPOutMessage message = createASIPOutMessage(topic, receiver, location, time);
                message.expose(interest);
            }
        }).start();
    }

    public ASIPOutMessage createASIPOutMessage(SemanticTag topic, PeerSemanticTag receiver, SpatialSemanticTag location, TimeSemanticTag time) {
        String[] addresses = getNearbyPeerTCPAddresses();
        if (addresses.length <= 0) return null;

        return createASIPOutMessage(addresses, this.getOwner(), receiver, location, time, topic, 10);
    }

    public void sendBroadcast(String text) {
        ((WifiDirectStreamStub) currentStub).sendBroadcast(text);
    }

    public void sendBroadcast(ASIPKnowledge knowledge) {
        ((WifiDirectStreamStub) currentStub).sendBroadcast(knowledge);
    }

    public void sendMessage(final MessageDTO message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ASIPOutMessage asipMessage = createASIPOutMessage(message.getReceiverPeer().getAddresses(), getOwner(), message.getReceiverPeer(), message.getReceiverSpatial(), message.getReceiverTime(), message.getTopic(), message.getTtl());
                try {
                    if (message.getCommand() == ASIPMessage.ASIP_INSERT) {
                        ASIPKnowledge knowledge = ASIPSerializer.deserializeASIPKnowledge(message.getContent());
                        asipMessage.insert(knowledge);
                    } else  if (message.getCommand() == ASIPMessage.ASIP_EXPOSE) {
                        ASIPInterest interest = ASIPSerializer.deserializeASIPInterest(message.getContent());
                        asipMessage.expose(interest);
                    } else {
                        //TODO is this right?
                        asipMessage.raw(message.getContent().getBytes());
                    }
                } catch (SharkKBException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void notifyInterestReceived(ASIPInterest asipInterest, ASIPConnection asipConnection) {
        L.d("Hey we received an Intererest", this);

        PeerSemanticTag sender = asipInterest.getSender();
        if (sender != null) {
            this.mNearbyPeers.put(sender, System.currentTimeMillis());
        }

    }

    @Override
    public void notifyKnowledgeReceived(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {
    }

    public List<PeerSemanticTag> getNearbyPeers(long millis) {
        long currentTime = System.currentTimeMillis();

        ArrayList<PeerSemanticTag> tags = new ArrayList<>();

        for (Map.Entry<PeerSemanticTag, Long> entry : this.mNearbyPeers.entrySet()) {
            if (currentTime - entry.getValue() <= millis) {
                tags.add(entry.getKey());
            }
        }

        return tags;
    }

    public List<PeerSemanticTag> getNearbyPeers() {
        return getNearbyPeers(1000 * 60);
    }

    public String[] getNearbyPeerTCPAddresses(long millis) {
        ArrayList<PeerSemanticTag> peers = (ArrayList) getNearbyPeers();

        ArrayList<String> addressList = new ArrayList<>();


        for (PeerSemanticTag tag : peers) {
            String[] peerAddresses = tag.getAddresses();
            for (String address : peerAddresses) {
                if (address.startsWith("tcp://")) {
                    addressList.add(address);
                }
            }
        }

        String[] addresses = new String[addressList.size()];
        addressList.toArray(addresses);
        return addresses;
    }

    public String[] getNearbyPeerTCPAddresses() {
        return getNearbyPeerTCPAddresses(1000 * 60);
    }
}
