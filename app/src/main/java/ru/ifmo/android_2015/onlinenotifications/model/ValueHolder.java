package ru.ifmo.android_2015.onlinenotifications.model;

import java.util.Objects;

public class ValueHolder<T> {
    private T value;
    private boolean modified;

    public ValueHolder(T defaultValue) {
        this.value = defaultValue;
    }

    public boolean isModified() {
        return modified;
    }

    public T getValue() {
        return value;
    }

    public void resetModifiedFlag() {
        modified = false;
    }

    public boolean setValue(T value) {
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            modified = true;
            return true;
        }
        return false;
    }
}
