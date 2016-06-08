package net.sharksystem.android.protocols.wifidirect;

import android.os.Build;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.peer.SharkEngine;
import net.sharkfw.system.L;

/**
 * Created by j4rvis on 31.05.16.
 */
public class RadarKP extends net.sharkfw.kp.FilterKP {

    private final WifiDirectBroadcastManager _wifiDirectBroadcastManager;

    public RadarKP(SharkEngine se, ASIPInterest filter, KPNotifier notifier) {
        super(se, filter, notifier);

        _wifiDirectBroadcastManager = WifiDirectBroadcastManager.getInstance(null);
    }

    @Override
    protected void handleInsert(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {
        super.handleInsert(asipKnowledge, asipConnection);
        L.d("We received an insert.", this);
        try {
            if(asipKnowledge!=null){
                _wifiDirectBroadcastManager.addKnowledge(asipKnowledge, asipConnection.getSender());
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleExpose(ASIPInterest interest, ASIPConnection asipConnection) throws SharkKBException {
        super.handleExpose(interest, asipConnection);
    }
}
