package net.sharksystem.android.protocols.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
public class WifiDirectPeer {

    private WifiP2pDevice device;
    private Map<String, String> txtRecordsMap;
    private long lastUpdated;

    public WifiDirectPeer(WifiP2pDevice device, Map<String, String> txtRecordsMap) {
        this.device = device;
        this.txtRecordsMap = txtRecordsMap;
        this.lastUpdated = System.currentTimeMillis();
    }

    public WifiP2pDevice getDevice(){
        return this.device;
    }

    public Map<String, String> getTxtRecords(){
        return this.txtRecordsMap;
    }

    public String getName(){
        return this.device.deviceName;
    }

    public String getAddress(){
        return this.device.deviceAddress;
    }

    public long getLastUpdated(){
        return this.lastUpdated;
    }

    public int getStatus(){
        return this.device.status;
    }

    public boolean update(WifiDirectPeer otherPeer){
        if(this.equals(otherPeer)){
            if(this.lastUpdated < otherPeer.getLastUpdated()){
                this.txtRecordsMap = otherPeer.getTxtRecords();
                this.lastUpdated = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return this.device.equals( ((WifiDirectPeer) o).getDevice());
    }

    //TODO Structure of map as Interest or Set of Interests
}
