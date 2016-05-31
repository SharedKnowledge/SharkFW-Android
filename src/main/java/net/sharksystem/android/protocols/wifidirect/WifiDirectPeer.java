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
public class WifiDirectPeer extends WifiP2pDevice {

    private ASIPInterest _interest;
    private PeerSemanticTag _tag;

    private Map<String, String> _txtRecordsMap;
    private long _lastUpdated;

    public String WIFI_PROTOCOL = "wifi://";
    private String WIFI_SI = "www.sharksystems.net/wifi";

    public int wpsConfigMethodsSupported;
    public int deviceCapability;
    public int groupCapability;
    public int status;

    public WifiDirectPeer(WifiP2pDevice device, Map<String, String> txtRecordsMap) {
        super(device);
        _txtRecordsMap = txtRecordsMap;
        _lastUpdated = System.currentTimeMillis();

        try {
            _tag = InMemoSharkKB.createInMemoPeerSemanticTag(deviceName, "www.sharksystem.de/" + deviceName, WIFI_PROTOCOL + deviceAddress);
            _interest = ASIPSerializer.deserializeASIPInterest(_txtRecordsMap.get("interest"));
            _interest.setSender(_tag);
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

    public Map<String, String> getTxtRecords() {
        return _txtRecordsMap;
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

//    private void tagToDevice(PeerSemanticTag tag){
//
//    }

//    public PeerSemanticTag getDeviceAsTag(){
//
//        String name = deviceName;
//
//        JSONObject object = new JSONObject();
//        try {
//            object.put("deviceName", deviceName);
//            object.put("deviceAddress", deviceAddress);
//            object.put("primaryDeviceType", primaryDeviceType);
//            object.put("secondaryDeviceType", secondaryDeviceType);
//            object.put("wpsConfigMethodsSupported", wpsConfigMethodsSupported);
//            object.put("deviceCapability", deviceCapability);
//            object.put("groupCapability", groupCapability);
//            object.put("status", status);
////            object.put("wfdInfo", secondaryDeviceType);
//            Class<?> c = this.getClass();
//            Field wfdInfo = c.getDeclaredField("wfdInfo");
//            Class<?> info = "WifiP2pWfdInfo".getClass();
//            wfdInfo.get(this).toString();
//            info.asSubclass(info).getConstructor(info).newInstance(wfdInfo);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//
//        return InMemoSharkKB.createInMemoPeerSemanticTag(name, WIFI_SI, WIFI_PROTOCOL + object.toString());
//    }
}
