package net.sharksystem.android.protocols.wifidirect;

import net.sharkfw.asip.ASIPKnowledge;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by j4rvis on 01.06.16.
 */
public class BroadcastManager {
    WifiDirectManager _wifiDirectManager;
    ArrayList<WifiDirectPeer> _peers;
    HashMap<ASIPKnowledge, ArrayList<WifiDirectPeer>> _map;

    ASIPKnowledge currentKnowledge;
    WifiDirectPeer currentPeer;

    public BroadcastManager(WifiDirectManager wifiDirectManager, ArrayList<WifiDirectPeer> peers, HashMap<ASIPKnowledge, ArrayList<WifiDirectPeer>> map) {
        _wifiDirectManager = wifiDirectManager;
        _peers = peers;
        _map = map;
    }

    public void initConnection(){

    }

    public void initSending(){

    }


}
