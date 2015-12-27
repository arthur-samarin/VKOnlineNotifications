package ru.ifmo.android_2015.onlinenotifications.model;

public class DatabaseException extends RuntimeException {
    public DatabaseException() {
    }

    public DatabaseException(String detailMessage) {
        super(detailMessage);
    }

    public DatabaseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public DatabaseException(Throwable throwable) {
        super(throwable);
    }
}
