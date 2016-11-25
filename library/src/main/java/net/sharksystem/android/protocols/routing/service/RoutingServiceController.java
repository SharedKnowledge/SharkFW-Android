package net.sharksystem.android.protocols.routing.service;

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
    private Intent mRoutingIntent = null;
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
        mRoutingIntent = new Intent(mContext, RoutingService.class);
    }

    public void enableRouting() {
        if (!isSharkRunning()) {
            Log.e("CONTROLLER", "Service not running, starting it");
            mContext.startService(mRoutingIntent);
        }
        if(!mIsBound){
            mIsBound = mContext.bindService(mRoutingIntent, this, Context.BIND_AUTO_CREATE);
        }
    }

    public void disableRouting() {
        mContext.stopService(mRoutingIntent);
        if (mIsBound) {
            mContext.unbindService(this);
            mIsBound = false;
            mRoutingService = null;
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
        mRoutingService.startRouting();
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
