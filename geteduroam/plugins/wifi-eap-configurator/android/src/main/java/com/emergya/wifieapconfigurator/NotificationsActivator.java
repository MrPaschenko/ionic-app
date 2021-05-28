package com.emergya.wifieapconfigurator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Its the class responsable of reactive the alarm when the device is rebooted
 */
public class NotificationsActivator extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            StartNotifications.enqueueWorkStart(context, new Intent());
        }
    }
}