package net.sharksystem.android.protocols.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by micha on 28.01.16.
 */
public class WifiDirectPeer implements Parcelable {

    private WifiP2pDevice _device;
    private Map<String, String> _txtRecordsMap;
    private long _lastUpdated;

    public WifiDirectPeer(Parcel parcel) {
        _device = parcel.readParcelable(WifiP2pDevice.class.getClassLoader());
        _txtRecordsMap = (HashMap<String, String>) parcel.readSerializable();
        _lastUpdated = parcel.readLong();
    }

    public WifiDirectPeer(WifiP2pDevice device, Map<String, String> txtRecordsMap) {
        _device = device;
        _txtRecordsMap = txtRecordsMap;
        _lastUpdated = System.currentTimeMillis();
    }

    public WifiP2pDevice getDevice() {
        return _device;
    }

    public Map<String, String> getTxtRecords() {
        return _txtRecordsMap;
    }

    public String getName() {
        return _device.deviceName;
    }

    public String getAddress() {
        return _device.deviceAddress;
    }

    public long getLastUpdated() {
        return _lastUpdated;
    }

    public int getStatus() {
        return _device.status;
    }

    public boolean update(WifiDirectPeer otherPeer) {
        if (this.equals(otherPeer)) {
            if (_lastUpdated < otherPeer.getLastUpdated()) {
                _txtRecordsMap = otherPeer.getTxtRecords();
                _lastUpdated = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return this._device.equals(((WifiDirectPeer) o).getDevice());
    }

    public static final Parcelable.Creator<WifiDirectPeer> CREATOR =
            new Parcelable.Creator<WifiDirectPeer>() {

                @Override
                public WifiDirectPeer createFromParcel(Parcel source) {
                    return new WifiDirectPeer(source);
                }

                @Override
                public WifiDirectPeer[] newArray(int size) {
                    return new WifiDirectPeer[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(_device, 0);
        Bundle bundle = new Bundle();
        bundle.putSerializable("HashMap", (HashMap) _txtRecordsMap);
        dest.writeBundle(bundle);
        dest.writeLong(_lastUpdated);
    }

    //TODO Structure of map as Interest or Set of Interests
}
