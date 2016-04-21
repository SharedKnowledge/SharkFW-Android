package net.sharksystem.android.peer;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;

import net.sharkfw.asip.SharkStub;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.peer.J2SEAndroidSharkEngine;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.protocols.Stub;
import net.sharkfw.system.L;
import net.sharksystem.android.protocols.nfc.NfcMessageStub;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;
import net.sharksystem.android.protocols.wifidirect.WifiDirectStreamStub;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class AndroidSharkEngine extends J2SEAndroidSharkEngine {
    Context context;
    WeakReference<Activity> activityRef;
    Stub currentStub;

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
            currentStub = new WifiDirectStreamStub(context, activityRef);
            currentStub.setHandler((RequestHandler) kepStub);
        }
        return currentStub;
    }

    @Override
    public void startWifiDirect() throws SharkProtocolNotSupportedException, IOException {
        this.createWifiDirectStreamStub(this.getAsipStub()).start();
    }

    public void stopWifiDirect() throws SharkProtocolNotSupportedException {
        currentStub.stop();
    }

    public Stub getWifiStub(){
        return currentStub;
    }

    public void connect(WifiDirectPeer peer){
        ((WifiDirectStreamStub) currentStub).connect(peer);
    }

    public void disconnect(WifiDirectPeer peer){
        ((WifiDirectStreamStub) currentStub).disconnect(peer);
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
        //TODO this function is called by the parent but the parent function itself look likes a big mess
        // and it does not look like it is designed to work with start/stop methods.
        return currentStub;
    }
}
