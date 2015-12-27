package ru.ifmo.android_2015.onlinenotifications;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import ru.ifmo.android_2015.onlinenotifications.model.LocalUser;
import ru.ifmo.android_2015.onlinenotifications.service.MyService;
import ru.ifmo.android_2015.onlinenotifications.service.ServiceException;
import ru.ifmo.android_2015.onlinenotifications.service.TaskListener;
import ru.ifmo.android_2015.onlinenotifications.ui.UserListRecyclerViewAdapter;
import ru.ifmo.android_2015.onlinenotifications.util.SaveContainer;
import ru.ifmo.android_2015.onlinenotifications.util.Task;

public class MainActivity extends AppCompatActivity {
    public static final int ADD_USER_REQUEST_CODE = 1;
    private static String KEY_PROGRESS_BAR_VISIBILITY = "progress_bar_visivility";
    private static String KEY_USER_LIST = "user_list";

    private ProgressBar progressBar;
    private RecyclerView userListView;
    private UserListRecyclerViewAdapter userListAdapter;

    private SaveContainer<MainActivity> saveContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        userListView = (RecyclerView) findViewById(R.id.userList);

        userListAdapter = new UserListRecyclerViewAdapter(Collections.<LocalUser>emptyList());
        userListView.setAdapter(userListAdapter);
        userListView.setLayoutManager(new LinearLayoutManager(this));

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                Helper.removeElement(MainActivity.this, viewHolder);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(userListView);

        if (savedInstanceState != null) {
            // Restore state
            saveContainer = (SaveContainer<MainActivity>) getLastCustomNonConfigurationInstance();
            saveContainer.attachActivity(this);
            userListAdapter.setUserList(saveContainer.<List<LocalUser>>get(KEY_USER_LIST));
        } else {
            saveContainer = new SaveContainer<>();
            saveContainer.attachActivity(this);
            Helper.loadUserListWhenServiceAvailable(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveContainer.put(KEY_USER_LIST, userListAdapter.getUserList());
        saveContainer.detachActivity();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PROGRESS_BAR_VISIBILITY, progressBar.getVisibility());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //noinspection ResourceType
        progressBar.setVisibility(savedInstanceState.getInt(KEY_PROGRESS_BAR_VISIBILITY));
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return saveContainer;
    }

    // Используется для избежания утечки экземпляра Activity в анонимных классах
    private static class Helper {
        public static void removeElement(MainActivity activity, RecyclerView.ViewHolder viewHolder) {
            final SaveContainer<MainActivity> saveContainer = activity.saveContainer;
            final LocalUser user = activity.userListAdapter.getDisplayedUser(viewHolder);

            activity.startProgress();
            MyService.runWhenServiceAvailable(new Runnable() {
                @Override
                public void run() {
                    MyService.getInstance().deleteUser(user, new TaskListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            saveContainer.addTask(new Task<MainActivity>() {
                                @Override
                                public void apply(MainActivity mainActivity) {
                                    Toast.makeText(mainActivity, R.string.user_removed_toast, Toast.LENGTH_SHORT).show();
                                    mainActivity.stopProgress();
                                    Helper.loadUserList(mainActivity);
                                }
                            });
                        }

                        @Override
                        public void onError(final ServiceException exception) {
                            saveContainer.addTask(new Task<MainActivity>() {
                                @Override
                                public void apply(MainActivity mainActivity) {
                                    Log.w(TAG, "Error deleting user", exception);
                                    Toast.makeText(mainActivity, exception.getMessage(), Toast.LENGTH_LONG).show();
                                    mainActivity.stopProgress();
                                }
                            });
                        }
                    });
                }
            });
        }

        public static void reloadDatabase(MainActivity activity) {
            final SaveContainer<MainActivity> saveContainer = activity.saveContainer;
            activity.startProgress();
            MyService.runWhenServiceAvailable(new Runnable() {
                @Override
                public void run() {
                    MyService.getInstance().reloadDatabase(new TaskListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            saveContainer.addTask(new Task<MainActivity>() {
                                @Override
                                public void apply(MainActivity mainActivity) {
                                    mainActivity.stopProgress();
                                    Toast.makeText(mainActivity, R.string.database_reloaded_toast, Toast.LENGTH_SHORT).show();
                                    Helper.loadUserList(mainActivity);
                                }
                            });
                        }

                        @Override
                        public void onError(final ServiceException exception) {
                            saveContainer.addTask(new Task<MainActivity>() {
                                @Override
                                public void apply(MainActivity mainActivity) {
                                    Toast.makeText(mainActivity, exception.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            });
        }

        private static void addUser(MainActivity activity, final int vkId) {
            final SaveContainer<MainActivity> saveContainer = activity.saveContainer;

            activity.startProgress();
            MyService.runWhenServiceAvailable(new Runnable() {
                @Override
                public void run() {
                    MyService.getInstance().addUser(vkId, new TaskListener<LocalUser>() {
                        @Override
                        public void onSuccess(LocalUser result) {
                            saveContainer.addTask(new Task<MainActivity>() {
                                @Override
                                public void apply(MainActivity mainActivity) {
                                    mainActivity.stopProgress();
                                    Helper.loadUserList(mainActivity);
                                }
                            });
                        }

                        @Override
                        public void onError(final ServiceException exception) {
                            saveContainer.addTask(new Task<MainActivity>() {
                                @Override
                                public void apply(MainActivity mainActivity) {
                                    Log.w(TAG, "Error adding user", exception);
                                    Toast.makeText(mainActivity, exception.getMessage(), Toast.LENGTH_LONG).show();
                                    mainActivity.stopProgress();
                                }
                            });
                        }
                    });
                }
            });
        }

        private static void loadUserList(MainActivity activity) {
            final SaveContainer<MainActivity> saveContainer = activity.saveContainer;
            activity.startProgress();

            MyService.getInstance().getObservedUsersList(new TaskListener<List<LocalUser>>() {
                @Override
                public void onSuccess(final List<LocalUser> result) {
                    saveContainer.addTask(new Task<MainActivity>() {
                        @Override
                        public void apply(MainActivity mainActivity) {
                            mainActivity.userListAdapter.setUserList(result);

                            Toast.makeText(mainActivity, R.string.user_list_loaded_toast, Toast.LENGTH_SHORT).show();
                            mainActivity.stopProgress();
                        }
                    });
                }

                @Override
                public void onError(final ServiceException exception) {
                    saveContainer.addTask(new Task<MainActivity>() {
                        @Override
                        public void apply(MainActivity mainActivity) {
                            Toast.makeText(mainActivity, exception.getMessage(), Toast.LENGTH_LONG).show();
                            mainActivity.stopProgress();
                        }
                    });
                }
            });
        }

        private static void loadUserListWhenServiceAvailable(MainActivity activity) {
            final SaveContainer<MainActivity> container = activity.saveContainer;
            MyService.runWhenServiceAvailable(new Runnable() {
                @Override
                public void run() {
                    container.addTask(new Task<MainActivity>() {
                        @Override
                        public void apply(MainActivity mainActivity) {
                            loadUserList(mainActivity);
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_add_user) {
            Intent intent = new Intent(this, AddUserActivity.class);
            startActivityForResult(intent, ADD_USER_REQUEST_CODE);
            return true;
        }
        if (id == R.id.action_reload_database) {
            Helper.reloadDatabase(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_USER_REQUEST_CODE) {
            if (resultCode == AddUserActivity.RESULT_CODE_OK) {
                final int vkId = data.getIntExtra(AddUserActivity.EXTRA_VK_ID, -1);

                Helper.addUser(this, vkId);
            }
        }
    }

    private void startProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void stopProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private static final String TAG = "MainActivity";
}
