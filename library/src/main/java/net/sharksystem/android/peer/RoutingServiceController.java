package net.sharksystem.android.peer;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import net.sharkfw.knowledgeBase.STSet;

public class RoutingServiceController implements ServiceConnection {
    private boolean mIsBound;
    private Intent mSharkIntent = null;
    private Context mContext = null;
    private static RoutingServiceController mInstance;
    private RoutingService mRoutingService;

    public static synchronized RoutingServiceController getInstance(Context context) {
        if (mInstance == null)
            mInstance = new RoutingServiceController(context);
        return mInstance;
    }

    private RoutingServiceController(Context context) {
        mContext = context.getApplicationContext();
        mSharkIntent = new Intent(mContext, RoutingService.class);
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
            mRoutingService = null;
        }
    }

    public void startRouting() {
        if (mRoutingService != null) {
            mRoutingService.startRouting();
        } else {
            // ?
        }
    }

    public void stopRouting() {
        if (mRoutingService != null) {
            mRoutingService.stopRouting();
        } else {
            // ?
        }
    }

    public void setTopicsToRoute(STSet topics) {
        if (mRoutingService != null) {
            mRoutingService.setTopicsToRoute(topics);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e("CONTROLLER", "Connected to service");
        mIsBound = true;
        RoutingService.LocalBinder localBinder = (RoutingService.LocalBinder) service;
        mRoutingService = localBinder.getInstance();

        // Set engine and kp if wanted
        mRoutingService.startEngine();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mIsBound = false;
        mRoutingService = null;
    }

    private boolean isSharkRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RoutingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
