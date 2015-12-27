package ru.ifmo.android_2015.onlinenotifications.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SaveContainer<T> {
    private T activity;
    private Queue<Task<? super T>> pendingTasks = new LinkedList<>();
    private Map<String, Object> values = new HashMap<>();

    public void attachActivity(T activity) {
        this.activity = activity;

        while (!pendingTasks.isEmpty()) {
            Task<? super T> task = pendingTasks.poll();
            task.apply(activity);
        }
    }

    public void detachActivity() {
        this.activity = null;
    }

    public void addTask(Task<? super T> task) {
        if (activity != null) {
            task.apply(activity);
        } else {
            pendingTasks.add(task);
        }
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public <V> V get(String key) {
        return (V) values.get(key);
    }
}
