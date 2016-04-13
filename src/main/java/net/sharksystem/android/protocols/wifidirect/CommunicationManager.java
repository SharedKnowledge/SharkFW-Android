package net.sharksystem.android.protocols.wifidirect;

import android.content.Context;

import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharksystem.android.peer.AndroidSharkEngine;

import java.io.IOException;

/**
 * Created by j4rvis on 13.04.16.
 */
public class CommunicationManager {
    private static CommunicationManager _instance = null;
    private AndroidSharkEngine _engine = null;
    private AndroidKP _kp = null;
    SharkWifiDirectManager _manager = null;

    private CommunicationManager(Context context) {
        _engine = new AndroidSharkEngine(context);
        _kp = new AndroidKP(_engine);
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
            _engine.startWifiDirect();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        try {
            _engine.startWifiDirect();
        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
