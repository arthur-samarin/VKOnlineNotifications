package ru.ifmo.android_2015.onlinenotifications.util;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class Observable<T> {
    private List<Observer<? super T>> listeners = new ArrayList<>();

    public void registerListener(Observer<? super T> listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Observer<? super T> listener) {
        listeners.remove(listener);
    }

    protected void fireChange(final T self) {
        ArrayList<Observer<? super T>> safeCopy = new ArrayList<>(listeners);
        for (final Observer<? super T> listener : safeCopy) {
            Handler uiHandler = new Handler(Looper.getMainLooper());
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onChanged(self);
                }
            });
        }
    }

    public interface Observer<T> {
        void onChanged(T value);
    }
}
