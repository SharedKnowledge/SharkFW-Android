package net.sharksystem.android.protocols.wifidirect;

import android.content.Context;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharksystem.android.peer.AndroidSharkEngine;

import java.io.IOException;
import java.util.List;

/**
 * Created by j4rvis on 13.04.16.
 */
public class CommunicationManager extends AndroidSharkEngine implements SharkWifiDirectManager.PeerListener {
    private static CommunicationManager _instance = null;
//    private AndroidSharkEngine _engine = null;
    private AndroidKP _kp = null;
    private SharkWifiDirectManager _manager = null;

    private CommunicationManager(Context context) {
        super(context);
//        _engine = new AndroidSharkEngine(context);
        _kp = new AndroidKP(this);
        _manager = SharkWifiDirectManager.getInstance(context);
    }

    public static CommunicationManager getInstance(Context context){
        if(CommunicationManager._instance == null){
            CommunicationManager._instance = new CommunicationManager(context);
        }
        return CommunicationManager._instance;
    }

    public void start(){
        try {
            this.startWifiDirect();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        try {
            this.startWifiDirect();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void offer(ASIPInterest interest){

    }

    @Override
    public void onNewPeer(List<WifiDirectPeer> peers) {

    }
}
