package net.sharksystem.android.protocols.routing.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SharkServiceAutoStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(RoutingService.KEY_SHARK, Context.MODE_PRIVATE);

        if (prefs.getBoolean(RoutingService.KEY_IS_ROUTING_ENABLED, false)) {
            context.startService(new Intent(context, RoutingService.class));
        }
    }
}
