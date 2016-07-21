package net.sharksystem.android.protocols.wifidirect;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import net.sharkfw.asip.ASIPInformation;
import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.system.L;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Created by j4rvis on 25.05.16.
 */
public class WifiDirectKPNotifier implements KPNotifier {

    private final Context _context;
    private LocalBroadcastManager _lbm;

    public final static String NEW_INTEREST_ACTION = "net.sharksystem.android.wifi.p2p.NEW_INTEREST";
    public final static String NEW_KNOWLEDGE_ACTION = "net.sharksystem.android.wifi.p2p.NEW_KNOWLEDGE";
    public final static String NEW_BROADCAST_ACTION = "net.sharksystem.android.wifi.p2p.BROADCAST";


    public WifiDirectKPNotifier(Context context) {
        _context = context;
        _lbm = LocalBroadcastManager.getInstance(_context);
    }

    @Override
    public void notifyInterestReceived(ASIPInterest asipInterest, ASIPConnection asipConnection) {
        Intent intent = new Intent(NEW_INTEREST_ACTION);
        try {
            intent.putExtra("interest", ASIPSerializer.serializeASIPSpace(asipInterest).toString());
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
        _lbm.sendBroadcast(intent);
    }

    @Override
    public void notifyKnowledgeReceived(ASIPKnowledge asipKnowledge, ASIPConnection asipConnection) {

        L.d("We got a knowledge", this);
//        if(asipKnowledge==null){
//            L.d("But knowledge equals null");
//            return;
//        }

        String senderName = "";
        try {
//            senderName = asipConnection.getSender().getName();
            STSet types = InMemoSharkKB.createInMemoSTSet();
            types.createSemanticTag("BROADCAST", "www.sharksystem.de/broadcast");
            ASIPSpace space = InMemoSharkKB.createInMemoASIPInterest(null, types, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
            Iterator<ASIPInformation> messages = asipKnowledge.getInformation(space);
            String message = "";
            if(messages.hasNext()){
                message = messages.next().getContentAsString();
            }
            L.d("Message:"+message, this);
            if(!message.isEmpty()){
                Intent intent = new Intent(NEW_BROADCAST_ACTION);
                intent.putExtra("broadcast_message", message);
//                intent.putExtra("broadcast_sender", senderName);
                _lbm.sendBroadcast(intent);
            }

        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notifyRawReceived(InputStream inputStream, ASIPConnection asipConnection) {

    }
}
