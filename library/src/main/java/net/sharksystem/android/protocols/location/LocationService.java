package net.sharksystem.android.protocols.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class LocationService extends Service {

    private AlarmManager mAlarmManager;
    private PendingIntent mLocationIntent;

    public static final long DEFAULT_COORDINATE_TTL = 24 * 60 * 60 * 1000; // 24 days
    public static final long DEFAULT_LOCATION_CHECK_INTERVAL = 2 * 60 * 1000; // 2 minutes

    @Override
    public void onCreate() {
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intentToFire = new Intent(LocationReceiver.ACTION_REFRESH_SCHEDULE_ALARM);
        intentToFire.putExtra(LocationReceiver.TAG_COORDINATE_TTL, DEFAULT_COORDINATE_TTL);
        mLocationIntent = PendingIntent.getBroadcast(this, 0, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), DEFAULT_LOCATION_CHECK_INTERVAL, mLocationIntent);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mAlarmManager.cancel(mLocationIntent);
        LocationReceiver.removeLocationListeners();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
