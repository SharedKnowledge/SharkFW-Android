package net.sharksystem.android.protocols.wifidirect_obsolete;

import android.net.wifi.p2p.WifiP2pDevice;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;

import java.util.Map;

/**
 * Created by j4rvis on 28.01.16.
 */
public class WifiDirectPeer extends WifiP2pDevice implements Comparable<WifiDirectPeer> {

    public static final String WIFI_PROTOCOL = "wifi://";

    private ASIPInterest mInterest = null;
    private String  mInterestString = "";
    private String mName = "";
    private PeerSemanticTag mTag;

    private long _lastUpdated;

    public WifiDirectPeer(WifiP2pDevice device, Map<String, String> txtRecordsMap) {
        super(device);
        _lastUpdated = System.currentTimeMillis();

        mInterestString = txtRecordsMap.get("interest");
        mName = txtRecordsMap.get("name");
//        if(txtRecordsMap != null){
//            if(txtRecordsMap.containsKey("interest")){
//            } if(txtRecordsMap.containsKey("name")){
//            }
//        }
        try {
            if(!mInterestString.isEmpty() && !mName.isEmpty()){
                mInterest = ASIPSerializer.deserializeASIPInterest(mInterestString);
                mTag = mInterest.getSender();
            }
//            String peerName = "";
//            if(!mName.isEmpty()){
//                peerName = mName;
//            } else if (!deviceName.isEmpty()){
//                peerName = deviceName;
//            } else {
//                peerName = "Anonym";
//            }
//            mTag = InMemoSharkKB.createInMemoPeerSemanticTag(peerName, "www.sharksystem.de/" + peerName, WIFI_PROTOCOL + deviceAddress);
//            String[] addresses = mInterest.getSender().getAddresses();
//            for (String address : addresses){
//                mTag.addAddress(address);
//            }
//            String[] sis = mInterest.getSender().getSI();
//            for (String si : sis){
//                mTag.addSI(si);
//            }
//            mInterest.setSender(mTag);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    public WifiDirectPeer(PeerSemanticTag tag, ASIPInterest interest){

        _lastUpdated = System.currentTimeMillis();
        mTag = tag;
        mInterest = interest;
        if(mTag.getName().isEmpty()){
            mName = mInterest.getSender().getName();
        } else {
            mName = mTag.getName();
        }
        String[] addresses = mTag.getAddresses();
        if(addresses[0].startsWith(WIFI_PROTOCOL)){
            deviceAddress = addresses[0].substring(WIFI_PROTOCOL.length());
        } else {
            deviceAddress = "0.0.0.0";
        }
    }

    public String getName() {
        return mName;
    }

    public ASIPInterest getmInterest(){
        return mInterest;
    }

    public PeerSemanticTag getmTag() {
        return mTag;
    }

    public long getLastUpdated() {
        return _lastUpdated;
    }

    @Override
    public int compareTo(WifiDirectPeer another) {
        return (_lastUpdated < another.getLastUpdated() ? -1 :
                (_lastUpdated == another.getLastUpdated() ? 0 : 1));
    }

    public void resetUpdated(){
        _lastUpdated = 0;
    }


}
