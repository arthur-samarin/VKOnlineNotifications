package ru.ifmo.android_2015.onlinenotifications;

import android.app.Application;
import android.content.Intent;

import com.vk.sdk.VKSdk;

import ru.ifmo.android_2015.onlinenotifications.service.MyService;

public class MyApplication extends Application {
    private static volatile DatabaseManager databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        databaseManager = new DatabaseManager(this);
        startService(new Intent(this, MyService.class));
    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
