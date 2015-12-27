package ru.ifmo.android_2015.onlinenotifications.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ru.ifmo.android_2015.onlinenotifications.MyApplication;
import ru.ifmo.android_2015.onlinenotifications.R;
import ru.ifmo.android_2015.onlinenotifications.model.LocalUser;
import ru.ifmo.android_2015.onlinenotifications.util.Images;

public class MyService extends Service {
    private static MyService instance;
    private static List<Runnable> onServiceStart = new ArrayList<>();

    public static MyService getInstance() {
        return instance;
    }

    public static void runWhenServiceAvailable(Runnable runnable) {
        if (instance != null) {
            runnable.run();
        } else {
            onServiceStart.add(runnable);
        }
    }

    private static final String PREFERENCES_NAME = "service_preferences";
    private Settings settings = new Settings();
    private Timer timer = new Timer("MyService Timer");
    private ExecutorService tasksExecutor = Executors.newSingleThreadExecutor();
    private List<LocalUser> observedUsers;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        reloadDatabase(new LogOnlyListener<Void>(TAG, "Loading database"));
        reloadSettings(new LogOnlyListener<Void>(TAG, "Loading settings"));

        for (Runnable handler : onServiceStart) {
            try {
                handler.run();
            } catch (Exception ex) {
                Log.w(TAG, "Exception in onServiceStart handler", ex);
            }
        }
    }

    public Settings getSettings() {
        return settings.copy();
    }

    public void reloadSettings(final TaskListener<Void> listener) {
        addNewTask(new ServiceTask<Void>(listener) {
            @Override
            protected Void run() throws ServiceException {
                settings.load(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE));
                initializeTimer();
                return null;
            }
        });
    }

    public void updateSettings(final Settings newSettings, final TaskListener<Void> listener) {
        addNewTask(new ServiceTask<Void>(listener) {
            @Override
            protected Void run() throws ServiceException {
                settings = newSettings.copy();
                initializeTimer();

                settings.save(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE));
                return null;
            }
        });
    }

    private void initializeTimer() {
        timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateUsers(true, new LogOnlyListener<Void>(TAG, "Updating users and showing notifications"));
            }
        }, 0L, settings.getUpdatePeriod(TimeUnit.MILLISECONDS));
    }

    public void updateUsers(final boolean showOnlineNotifications, final TaskListener<Void> listener) {
        addNewTask(new ServiceTask<Void>(listener) {
            @Override
            protected Void run() throws ServiceException {
                if (observedUsers == null) {
                    throw new ServiceException("User list not ready");
                }

                SparseArray<LocalUser> observedUsersMap = new SparseArray<>();
                for (LocalUser user : observedUsers) {
                    observedUsersMap.append(user.getVkId(), user);
                }

                List<LocalUser> newUsersData = loadUsersData(makeStringFromUserIds(observedUsers));
                for (LocalUser userData : newUsersData) {
                    LocalUser observedUser = observedUsersMap.get(userData.getVkId());
                    observedUser.updateData(userData.getFullName().getValue(), userData.getAvatarUrl().getValue(), userData.getOnline().getValue());
                }

                if (showOnlineNotifications) {
                    for (LocalUser user : observedUsers) {
                        if (user.getOnline().isModified()) {
                            Notification.Builder builder = new Notification.Builder(MyService.this);
                            builder.setSmallIcon(R.drawable.ic_ab_done);

                            Bitmap userImage = Images.downloadSyncOrNull(user.getAvatarUrl().getValue());
                            if (userImage != null) {
                                builder.setLargeIcon(userImage);
                            }

                            builder.setContentTitle(user.getOnline().getValue() ? "User is online" : "User is offline");
                            builder.setContentText(user.getFullName().getValue());
                            Notification notification = builder.build();

                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(TAG, user.getVkId(), notification);
                        }
                    }
                }

                storeChanges(observedUsers);
                return null;
            }
        });
    }

    public void deleteUser(final LocalUser user, TaskListener<Void> listener) {
        addNewTask(new ServiceTask<Void>(listener) {
            @Override
            protected Void run() throws ServiceException {
                observedUsers.remove(user);
                user.delete();
                storeChanges(Collections.singletonList(user));
                return null;
            }
        });
    }

    public void addUser(final int vkId, TaskListener<LocalUser> listener) {
        addNewTask(new ServiceTask<LocalUser>(listener) {
            @Override
            protected LocalUser run() throws ServiceException {
                if (observedUsers == null) {
                    throw new ServiceException("User list not ready");
                }

                VKRequest req = VKApi.users().get(VKParameters.from("user_ids", vkId, "fields", "online,first_name,last_name,photo_200"));

                final AtomicReference<LocalUser> userRef = new AtomicReference<>();
                final AtomicReference<VKError> errorRef = new AtomicReference<>();

                req.executeSyncWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        List<VKApiUserFull> users = (List<VKApiUserFull>) response.parsedModel;
                        if (!users.isEmpty()) {
                            VKApiUserFull vkUser = users.get(0);
                            LocalUser user = new LocalUser(vkUser.getId(), vkUser.first_name + " " + vkUser.last_name, vkUser.photo_200, vkUser.online);
                            userRef.set(user);
                        }
                    }

                    @Override
                    public void onError(VKError error) {
                        errorRef.set(error);
                    }
                });

                LocalUser user = userRef.get();
                VKError error = errorRef.get();

                if (error == null) {
                    if (user != null) {
                        storeChanges(Collections.singletonList(user));
                        observedUsers.add(user);
                        return user;
                    } else {
                        throw new ServiceException("No such user");
                    }
                } else {
                    throw new ServiceException(error.errorMessage);
                }
            }
        });
    }

    private void storeChanges(List<LocalUser> list) throws ServiceException {
        try {
            SQLiteDatabase database = MyApplication.getDatabaseManager().getWritableDatabase();
            database.beginTransaction();
            try {
                for (LocalUser user : list) {
                    user.storeChanges(database);
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        } catch (Exception ex) {
            throw new ServiceException("Database error", ex);
        }
    }

    private List<LocalUser> loadUsersData(String ids) throws ServiceException {
        VKRequest req = VKApi.users().get(VKParameters.from("user_ids", ids, "fields", "online,first_name,last_name,photo_200"));

        final AtomicReference<List<LocalUser>> userRef = new AtomicReference<>();
        final AtomicReference<VKError> errorRef = new AtomicReference<>();

        req.executeSyncWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                List<VKApiUserFull> vkUsers = (List<VKApiUserFull>) response.parsedModel;
                List<LocalUser> users = new ArrayList<LocalUser>();
                for (VKApiUserFull user : vkUsers) {
                    users.add(new LocalUser(user));
                }
                userRef.set(users);
            }

            @Override
            public void onError(VKError error) {
                errorRef.set(error);
            }
        });

        List<LocalUser> users = userRef.get();
        VKError error = errorRef.get();

        if (error != null) {
            throw new ServiceException(error.errorMessage);
        } else {
            return users;
        }
    }

    public void reloadDatabase(TaskListener<Void> listener) {
        addNewTask(new ServiceTask<Void>(listener) {
            @Override
            protected Void run() throws ServiceException {
                try {
                    SQLiteDatabase database = MyApplication.getDatabaseManager().getReadableDatabase();
                    observedUsers = LocalUser.loadAll(database);
                } catch (Exception e) {
                    throw new ServiceException("Error loading user list", e);
                }
                return null;
            }
        });
    }

    public void getObservedUsersList(TaskListener<List<LocalUser>> listener) {
        addNewTask(new ServiceTask<List<LocalUser>>(listener) {
            @Override
            protected List<LocalUser> run() throws ServiceException {
                if (observedUsers != null) {
                    return observedUsers;
                } else {
                    throw new ServiceException("Error loading user list");
                }
            }
        });
    }

    private void addNewTask(@NonNull final ServiceTask<?> task) {
        tasksExecutor.submit(new Runnable() {
            @Override
            public void run() {
                task.runAndCallListeners();
            }
        });
    }

    private String makeStringFromUserIds(List<LocalUser> list) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size() - 1; i++) {
            builder.append(list.get(i).getVkId());
            builder.append(",");
        }
        builder.append(list.get(list.size() - 1).getVkId());
        return builder.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final String TAG = "MyService";
}
