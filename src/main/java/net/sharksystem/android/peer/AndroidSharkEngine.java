package net.sharksystem.android.peer;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import net.sharkfw.asip.ASIPKnowledge;
import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.asip.SharkStub;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.J2SEAndroidSharkEngine;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.Stub;
import net.sharkfw.system.SharkNotSupportedException;
import net.sharksystem.android.protocols.nfc.NfcMessageStub;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;
import net.sharksystem.android.protocols.wifidirect.WifiDirectStreamStub;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AndroidSharkEngine extends J2SEAndroidSharkEngine {
    Context context;
    WeakReference<Activity> activityRef;
    Stub currentStub;
    private ASIPSpace mSpace;
    private String mName;

    public AndroidSharkEngine(Context context) {
        super();
        this.activateASIP();
        this.context = context;
    }

    public AndroidSharkEngine(Activity activity) {
        super();
        this.activateASIP();
        this.context = activity.getApplicationContext();
        this.activityRef = new WeakReference<>(activity);
    }

    /*
     * Wifi Direct methods
     * @see net.sharkfw.peer.SharkEngine#createWifiDirectStreamStub(net.sharkfw.kep.KEPStub)
     */
    protected Stub createWifiDirectStreamStub(SharkStub kepStub) throws SharkProtocolNotSupportedException {
        if (currentStub == null) {
            currentStub = new WifiDirectStreamStub(context, this, mSpace, mName);
            currentStub.setHandler((RequestHandler) kepStub);
        }
        return currentStub;
    }

    @Override
    public void startWifiDirect() throws SharkProtocolNotSupportedException, IOException {
//        this.createWifiDirectStreamStub(this.getAsipStub()).start();
        this.createWifiDirectStreamStub(this.getAsipStub()).start();
//        offerInterest("TestTopic");
    }

    public void stopWifiDirect() throws SharkProtocolNotSupportedException {
        currentStub.stop();
    }

    @Override
    protected Stub createNfcStreamStub(SharkStub stub) throws SharkProtocolNotSupportedException {
        if (currentStub == null) {
            currentStub = new NfcMessageStub(context, activityRef);
            currentStub.setHandler((RequestHandler) stub);
        }
        return currentStub;
    }

    @Override
    public void startNfc() throws SharkProtocolNotSupportedException, IOException {
        this.createNfcStreamStub(this.getAsipStub()).start();
    }

    @Override
    public void stopNfc() throws SharkProtocolNotSupportedException {
        this.createNfcStreamStub(this.getAsipStub()).stop();
    }

    @Override
    protected Stub createBluetoothStreamStub(SharkStub kepStub) throws SharkProtocolNotSupportedException {
        throw new SharkProtocolNotSupportedException();
    }

    @Override
    public void startBluetooth() throws SharkProtocolNotSupportedException, IOException {
        throw new SharkProtocolNotSupportedException();
    }

    @Override
    public void stopBluetooth() throws SharkProtocolNotSupportedException {
        throw new SharkProtocolNotSupportedException();
    }

    @Override
    public Stub getProtocolStub(int type) throws SharkProtocolNotSupportedException {
        return super.getProtocolStub(type);
        //TODO this function is called by the parent but the parent function itself look likes a big mess
        // and it does not look like it is designed to work with start/stop methods.
//        return currentStub;
    }

    public void offerInterest(String topic, String name){

        if(TextUtils.isEmpty(topic))
           topic = "Dummy Interesse";

        STSet set = InMemoSharkKB.createInMemoSTSet();
        ASIPSpace space;
        try {
            set.createSemanticTag(topic, "www."+topic+".sharksystem.net");
            space =  InMemoSharkKB.createInMemoASIPInterest(set, null, (PeerSemanticTag) null, null, null, null, null, ASIPSpace.DIRECTION_INOUT);
            mSpace = space;
            mName = name;
//            currentStub.offer(space);
        } catch (SharkKBException e) {
            e.printStackTrace();
        }

    }

    public List<PeerSemanticTag> getNearbyPeers() {
        return new ArrayList<PeerSemanticTag>();
    }


    public void sendBroadcast(String text) {
        ((WifiDirectStreamStub) currentStub).sendBroadcast(text);
    }

    public void sendBroadcast(ASIPKnowledge knowledge){
        ((WifiDirectStreamStub) currentStub).sendBroadcast(knowledge);
    }
}
