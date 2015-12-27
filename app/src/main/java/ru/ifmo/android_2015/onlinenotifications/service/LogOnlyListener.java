package ru.ifmo.android_2015.onlinenotifications.service;

import android.util.Log;

public class LogOnlyListener<T> implements TaskListener<T> {
    private String tag;
    private String message;

    public LogOnlyListener(String tag, String message) {
        this.tag = tag;
        this.message = message;
    }

    @Override
    public void onSuccess(Object result) {
        Log.d(tag, message);
    }

    @Override
    public void onError(ServiceException exception) {
        Log.w(tag, exception);
    }
}
