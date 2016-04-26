package net.sharksystem.android.protocols.wifidirect;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.knowledgeBase.SharkCS;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.peer.KEPConnection;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.peer.SharkEngine;
import net.sharkfw.system.L;

import java.io.InputStream;

/**
 * Created by msc on 12.04.16.
 */
public class AndroidKP extends KnowledgePort {

    public AndroidKP(SharkEngine se) {
        super(se);
    }

    @Override
    protected void handleInsert(Knowledge knowledge, KEPConnection kepConnection) {

    }

    @Override
    protected void handleExpose(SharkCS sharkCS, KEPConnection kepConnection) {

    }

    @Override
    protected void handleInsert(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {
        super.handleInsert(asipKnowledge, asipConnection);
    }

    @Override
    protected void handleExpose(ASIPInterest interest, ASIPConnection asipConnection) throws SharkKBException {
        super.handleExpose(interest, asipConnection);
    }

    @Override
    protected void handleRaw(InputStream is, ASIPConnection asipConnection) {
        L.d("RAAAAAAWWWR!", this);
        super.handleRaw(is, asipConnection);
    }

    @Override
    protected void doProcess(ASIPInMessage msg, ASIPConnection con) {
        L.d("Message received");
        super.doProcess(msg, con);
    }
}
