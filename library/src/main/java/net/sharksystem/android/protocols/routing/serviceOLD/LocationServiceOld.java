package net.sharksystem.android.protocols.routing.serviceOLD;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Calendar;

public class LocationServiceOld extends Service {

    private AlarmManager _alarmMananger;
    public PendingIntent _pendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        _alarmMananger = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intentToFire = new Intent(this, LocationReceiverOld.class);
        _pendingIntent = PendingIntent.getBroadcast(this, 0, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            long interval = 60 * 1000;
            int alarmType = AlarmManager.RTC_WAKEUP;
            long timetoRefresh = Calendar.getInstance().getTimeInMillis();
            _alarmMananger.setInexactRepeating(alarmType, timetoRefresh, interval, _pendingIntent);
            Toast.makeText(this, "Service started.", Toast.LENGTH_SHORT).show();
            return Service.START_NOT_STICKY;
        } catch (Exception e) {
            Toast.makeText(this, "error running service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return Service.START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        this._alarmMananger.cancel(_pendingIntent);
        LocationReceiverOld.stopLocationListener();

        Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show();
    }
}
