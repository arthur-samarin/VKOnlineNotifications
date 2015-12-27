package ru.ifmo.android_2015.onlinenotifications.service;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

public abstract class ServiceTask<R> {
    private final TaskListener<R> listener;

    public ServiceTask(@NonNull TaskListener<R> listener) {
        this.listener = listener;
    }

    public void runAndCallListeners() {
        try {
            final R result = run();
            runInMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess(result);
                }
            });
        } catch (final ServiceException e) {
            runInMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onError(e);
                }
            });
        } catch (Throwable throwable) {
            Log.w(TAG, throwable);
        }
    }

    protected abstract R run() throws ServiceException;

    private static void runInMainThread(Runnable runnable) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(runnable);
    }

    public static final String TAG = "ServiceTask";
}
