package net.sharksystem.android.protocols.wifidirect;

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
    public RadarKP(SharkEngine se, ASIPInterest filter, KPNotifier notifier) {
        super(se, filter, notifier);
    }

    @Override
    protected void handleInsert(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {
        super.handleInsert(asipKnowledge, asipConnection);
        L.d("We received an insert.", this);
    }

    @Override
    protected void handleExpose(ASIPInterest interest, ASIPConnection asipConnection) throws SharkKBException {
        super.handleExpose(interest, asipConnection);
    }
}
