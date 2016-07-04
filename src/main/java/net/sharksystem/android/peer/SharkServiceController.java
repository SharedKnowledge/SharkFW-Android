package net.sharksystem.android.peer;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by j4rvis on 26.04.16.
 */
public class SharkServiceController implements ServiceConnection, KPListener {

    private CopyOnWriteArrayList<WifiDirectPeer> mPeers = null;

    private boolean mIsBound;
    private Intent mSharkIntent = null;
    private Context mContext = null;
    private static SharkServiceController mInstance;
    private SharkService mSharkService;
    private String mName = "Name";
    private String mInterest = "Interesse";


    public static synchronized SharkServiceController getInstance(Context context) {
        if (mInstance == null)
            mInstance = new SharkServiceController(context);
        return mInstance;
    }

    private SharkServiceController(Context context) {
        mContext = context.getApplicationContext();
        mPeers = new CopyOnWriteArrayList<>();

        mSharkIntent = new Intent(mContext, SharkService.class);
        mSharkIntent.putExtra("name", mName);
        mSharkIntent.putExtra("interest", mInterest);
    }

    public void startShark(){
        if (!isSharkRunning()) {
            Log.e("CONTROLLER", "Service not running, starting it");
            mContext.startService(mSharkIntent);
        }
        if(!mIsBound){
            mIsBound = mContext.bindService(mSharkIntent, this, Context.BIND_AUTO_CREATE);
        }
    }

    public void stopShark(){
        mContext.stopService(mSharkIntent);
        if (mIsBound) {
            mContext.unbindService(this);
            mIsBound = false;
            mSharkService = null;
        }
    }

    public void startRouting() {
        if (mSharkService != null) {
            mSharkService.startRouting();
        } else {
            // ?
        }
    }

    public void stopRouting() {
        if (mSharkService != null) {
            mSharkService.stopRouting();
        } else {
            // ?
        }
    }

    public void setOffer(String name, String interest){
        mName = name;
        mInterest = interest;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e("CONTROLLER", "Connected to service");
        mIsBound = true;
        SharkService.LocalBinder localBinder = (SharkService.LocalBinder) service;
        mSharkService = localBinder.getInstance();

        mSharkService.addKPListener(this);

        mSharkService.setInterestToOffer(mInterest);
        mSharkService.setNameToOffer(mName);

        // Set engine and kp if wanted
        mSharkService.startEngine();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mIsBound = false;
        mSharkService = null;

        mSharkService.removeKPListener(this);
    }

    @Override
    public void onNewInterest(ASIPInterest interest) {
        WifiDirectPeer newPeer = new WifiDirectPeer(interest.getSender(), interest);

        if (mPeers.contains(newPeer)) {
            WifiDirectPeer peer = mPeers.get(mPeers.indexOf(newPeer));
            if (peer.getLastUpdated() < newPeer.getLastUpdated()) {
                mPeers.remove(peer);
                mPeers.add(newPeer);
            }
        } else {
            mPeers.add(newPeer);
        }
    }

    @Override
    public void onNewKnowledge(ASIPKnowledge knowledge) {

    }

    public CopyOnWriteArrayList<WifiDirectPeer> getPeers(){
        return mPeers;
    }

    public void resetPeers(){
        mPeers = new CopyOnWriteArrayList<>();
    }

    public void sendBroadcast(String text){
        mSharkService.sendBroadcast(text);
    }

    private boolean isSharkRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SharkService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
