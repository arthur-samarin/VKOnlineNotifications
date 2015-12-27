package ru.ifmo.android_2015.onlinenotifications.ui;

import android.view.View;

public interface OnElementClickListener<T> {
    void onElementClick(View view, T element);
}
