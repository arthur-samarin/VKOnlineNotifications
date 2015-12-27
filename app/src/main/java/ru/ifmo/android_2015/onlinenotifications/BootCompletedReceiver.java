package ru.ifmo.android_2015.onlinenotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.ifmo.android_2015.onlinenotifications.service.MyService;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Nothing
        // Service is started in MyApplication
    }
}
