package net.sharksystem.android.protocols.wifidirect;

import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.StreamConnection;
import net.sharkfw.protocols.StreamStub;
import net.sharkfw.system.SharkNotSupportedException;

import java.io.IOException;
import java.util.List;

/**
 * Created by j4rvis on 22.07.16.
 */
public class WifiDirectStreamStub implements StreamStub{
    @Override
    public StreamConnection createStreamConnection(String s) throws IOException {
        return null;
    }

    @Override
    public String getLocalAddress() {
        return null;
    }

    @Override
    public void setHandler(RequestHandler requestHandler) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void start() throws IOException {

    }

    @Override
    public boolean started() {
        return false;
    }

    @Override
    public void offer(ASIPSpace asipSpace) throws SharkNotSupportedException {

    }

    @Override
    public void offer(Knowledge knowledge) throws SharkNotSupportedException {

    }

    // Peer lists

    private List<PeerSemanticTag> mNeighbours;
    private List<PeerSemanticTag> mAdhocNetworkNeighbours;

    public List<PeerSemanticTag> getNeighbours(){
        return mNeighbours;
    }

    public List<PeerSemanticTag> getAdhocNetworkNeighbours(){
        return mAdhocNetworkNeighbours;
    }
}
