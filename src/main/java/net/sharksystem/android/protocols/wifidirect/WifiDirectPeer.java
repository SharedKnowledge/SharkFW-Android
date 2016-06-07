package net.sharksystem.android.protocols.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;

import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
public class WifiDirectPeer extends WifiP2pDevice implements Comparable<WifiDirectPeer> {

    public final String WIFI_PROTOCOL = "wifi://";

    private ASIPInterest _interest = null;

    private PeerSemanticTag _tag;

    private long _lastUpdated;

    public WifiDirectPeer(WifiP2pDevice device, Map<String, String> txtRecordsMap) {
        super(device);
        _lastUpdated = System.currentTimeMillis();

        try {
            _tag = InMemoSharkKB.createInMemoPeerSemanticTag(deviceName, "www.sharksystem.de/" + deviceName, WIFI_PROTOCOL + deviceAddress);
            if(txtRecordsMap != null){
                _interest = ASIPSerializer.deserializeASIPInterest(txtRecordsMap.get("interest"));
                _interest.setSender(_tag);
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    public WifiDirectPeer(PeerSemanticTag tag, ASIPInterest interest){
        _tag = tag;
        _interest = interest;
        if(_tag.getName().isEmpty()){
            deviceName = _interest.getSender().getName();
        } else {
            deviceName = _tag.getName();
        }
        String[] addresses = _tag.getAddresses();
        if(addresses[0].startsWith(WIFI_PROTOCOL)){
            deviceAddress = addresses[0].substring(WIFI_PROTOCOL.length());
        } else {
            deviceAddress = "0.0.0.0";
        }
    }

    public ASIPInterest getInterest(){
        return _interest;
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
}
