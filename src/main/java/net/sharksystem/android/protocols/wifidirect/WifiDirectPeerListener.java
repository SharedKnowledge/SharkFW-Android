package net.sharksystem.android.protocols.wifidirect;

import java.util.List;

/**
 * Created by micha on 03.02.16.
 */
public interface WifiDirectPeerListener {
    public void onNewPeer(List<WifiDirectPeer> peers);
}
