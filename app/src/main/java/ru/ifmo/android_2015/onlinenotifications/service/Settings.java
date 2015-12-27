package ru.ifmo.android_2015.onlinenotifications.service;

import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public class Settings {
    public static final String KEY_UPDATE_PERIOD = "update_period";
    private long updatePeriodMillis = 60_000L;

    public void save(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_UPDATE_PERIOD, updatePeriodMillis);
        editor.apply();
    }

    public void load(SharedPreferences preferences) {
        updatePeriodMillis = preferences.getLong(KEY_UPDATE_PERIOD, updatePeriodMillis);
    }

    public long getUpdatePeriod(TimeUnit timeUnit) {
        return timeUnit.convert(updatePeriodMillis, TimeUnit.MILLISECONDS);
    }

    public void setUpdatePeriod(long period, TimeUnit timeUnit) {
        updatePeriodMillis = TimeUnit.MILLISECONDS.convert(period, timeUnit);
    }

    public Settings copy() {
        Settings settings = new Settings();
        settings.updatePeriodMillis = updatePeriodMillis;
        return settings;
    }
}
