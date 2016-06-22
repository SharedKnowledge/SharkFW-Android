package net.sharksystem.android.protocols.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.system.L;

import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
public class WifiDirectPeer extends WifiP2pDevice implements Comparable<WifiDirectPeer> {

    public static final String WIFI_PROTOCOL = "wifi://";

    private ASIPInterest mInterest = null;
    private String  mInterestString = "";
    private String mName = "";
    private PeerSemanticTag _tag;

    private long _lastUpdated;

    public WifiDirectPeer(WifiP2pDevice device, Map<String, String> txtRecordsMap) {
        super(device);
        _lastUpdated = System.currentTimeMillis();

        if(txtRecordsMap != null){
            if(txtRecordsMap.containsKey("interest")){
                mInterestString = txtRecordsMap.get("interest");
            } if(txtRecordsMap.containsKey("name")){
                mName = txtRecordsMap.get("name");
            }
        }

        try {
            String peerName = "";
            if(!mName.isEmpty()){
                peerName = mName;
            } else if (!deviceName.isEmpty()){
                peerName = deviceName;
            } else {
                peerName = "Anonym";
            }
            _tag = InMemoSharkKB.createInMemoPeerSemanticTag(peerName, "www.sharksystem.de/" + peerName, WIFI_PROTOCOL + deviceAddress);
            mInterest = ASIPSerializer.deserializeASIPInterest(mInterestString);
            mInterest.setSender(_tag);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    public WifiDirectPeer(PeerSemanticTag tag, ASIPInterest interest){

        _lastUpdated = System.currentTimeMillis();
        _tag = tag;
        mInterest = interest;
        if(_tag.getName().isEmpty()){
            mName = mInterest.getSender().getName();
        } else {
            mName = _tag.getName();
        }
        String[] addresses = _tag.getAddresses();
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

    public PeerSemanticTag getTag() {
        return _tag;
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
