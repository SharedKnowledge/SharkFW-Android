package net.sharksystem.android.ports;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.kp.FilterKP;
import net.sharkfw.peer.SharkEngine;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.wifidirect_obsolete.WifiDirectBroadcastManager;

/**
 * Created by j4rvis on 31.05.16.
 */
public class RadarKP extends FilterKP {

    private final WifiDirectBroadcastManager mWifiDirectBroadcastManager;

    public RadarKP(SharkEngine se, ASIPInterest filter) {
        super(se, filter);

        mWifiDirectBroadcastManager = WifiDirectBroadcastManager.getInstance(null);
    }

    @Override
    protected void handleInsert(ASIPInMessage message, ASIPConnection asipConnection, ASIPKnowledge asipKnowledge) {
        super.handleInsert(message, asipConnection, asipKnowledge);
        L.d("We received an insert.", this);
        try {
            if(asipKnowledge!=null){
                mWifiDirectBroadcastManager.addKnowledge(asipKnowledge, asipConnection.getSender());
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleExpose(ASIPInMessage message, ASIPConnection asipConnection, ASIPInterest interest) throws SharkKBException {
        super.handleExpose(message, asipConnection, interest);
    }
}
