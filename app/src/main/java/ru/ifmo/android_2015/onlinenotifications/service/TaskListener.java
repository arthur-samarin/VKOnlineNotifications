package ru.ifmo.android_2015.onlinenotifications.service;

public interface TaskListener<R> {
    void onSuccess(R result);
    void onError(ServiceException exception);
}
