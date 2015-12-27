package ru.ifmo.android_2015.onlinenotifications.util;

public interface Task<T> {
    void apply(T value);
}
