package net.sharksystem.android.peer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharksystem.android.protocols.wifidirect.WifiDirectKPNotifier;
import net.sharksystem.android.protocols.wifidirect.WifiDirectPeer;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by j4rvis on 26.04.16.
 */
public class SharkServiceController
        extends BroadcastReceiver
        implements ServiceConnection {

    private CopyOnWriteArrayList<WifiDirectPeer> mPeers = null;

    private boolean mIsBound;
    private Intent mIntent = null;
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

    public SharkServiceController(Context context) {
        mContext = context.getApplicationContext();
        mPeers = new CopyOnWriteArrayList<>();

        mIntent = new Intent(mContext, SharkService.class);
        mIntent.putExtra("name", mName);
        mIntent.putExtra("interest", mInterest);
    }

    public void stopService(){
        mContext.stopService(mIntent);
    }

    public void bindToService(){
        registerReceiver();
//        Toast.makeText(mContext, "Binding...", Toast.LENGTH_SHORT).show();
        if(!mIsBound){
            mIsBound = mContext.bindService(mIntent, this, mContext.BIND_AUTO_CREATE);
        }
    }

    public void setOffer(String name, String interest){
        mName = name;
        mInterest = interest;
    }

    public void unbindFromService(){
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this);
        if(mIsBound){
            mContext.unbindService(this);
            mIsBound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mIsBound = true;
        SharkService.LocalBinder localBinder = (SharkService.LocalBinder) service;
        mSharkService = localBinder.getInstance();

        mSharkService.setInterestToOffer(mInterest);
        mSharkService.setNameToOffer(mName);

        // Set engine and kp if wanted
        mSharkService.startEngine();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mIsBound = false;
        mSharkService = null;
        stopService();
    }

    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiDirectKPNotifier.NEW_INTEREST_ACTION);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiDirectKPNotifier.NEW_INTEREST_ACTION.equals(action)) {

            String stringInterest = intent.getStringExtra("interest");
            ASIPInterest interest = null;
            try {
                interest = ASIPSerializer.deserializeASIPInterest(stringInterest);
            } catch (SharkKBException e) {
                e.printStackTrace();
            }

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
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SharkService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

}
