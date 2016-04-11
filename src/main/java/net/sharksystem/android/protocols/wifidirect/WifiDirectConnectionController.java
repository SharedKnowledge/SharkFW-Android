package net.sharksystem.android.protocols.wifidirect;

/**
 * Created by micha on 08.02.16.
 */
public interface WifiDirectConnectionController {
    public void onConnect(WifiDirectPeer peer);
    public void onDisconnect(WifiDirectPeer peer);
}
