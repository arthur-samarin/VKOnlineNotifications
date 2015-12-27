package ru.ifmo.android_2015.onlinenotifications.service;

public class ServiceException extends Exception {
    public ServiceException() {
    }

    public ServiceException(String detailMessage) {
        super(detailMessage);
    }

    public ServiceException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ServiceException(Throwable throwable) {
        super(throwable);
    }
}
