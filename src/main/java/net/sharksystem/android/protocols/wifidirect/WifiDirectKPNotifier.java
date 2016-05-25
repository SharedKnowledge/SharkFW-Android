package net.sharksystem.android.protocols.wifidirect;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.kp.KPNotifier;

/**
 * Created by j4rvis on 25.05.16.
 */
public class WifiDirectKPNotifier implements KPNotifier {

    private final Context _context;
    private LocalBroadcastManager _lbm;

    public final static String NEW_INTEREST_ACTION = "net.sharksystem.android.wifi.p2p.NEW_INTEREST";
    public final static String NEW_KNOWLEDGE_ACTION = "net.sharksystem.android.wifi.p2p.NEW_KNOWLEDGE";


    public WifiDirectKPNotifier(Context context) {
        _context = context;
        _lbm = LocalBroadcastManager.getInstance(_context);
    }

    @Override
    public void notifyInterestReceived(ASIPInterest asipInterest) {
        Intent intent = new Intent(NEW_INTEREST_ACTION);
        try {
            intent.putExtra("interest", ASIPSerializer.serializeASIPSpace(asipInterest).toString());
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
        _lbm.sendBroadcast(intent);
    }

    @Override
    public void notifyKnowledgeReceived(ASIPKnowledge asipKnowledge) {

    }

    @Override
    public void notifyPeerReceived(PeerSemanticTag peerSemanticTag) {

    }


}
