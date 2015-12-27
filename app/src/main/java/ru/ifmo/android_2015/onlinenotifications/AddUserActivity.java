package ru.ifmo.android_2015.onlinenotifications;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.ifmo.android_2015.onlinenotifications.model.LocalUser;
import ru.ifmo.android_2015.onlinenotifications.ui.OnElementClickListener;
import ru.ifmo.android_2015.onlinenotifications.ui.UserListRecyclerViewAdapter;
import ru.ifmo.android_2015.onlinenotifications.util.Task;
import ru.ifmo.android_2015.onlinenotifications.util.SaveContainer;

public class AddUserActivity extends AppCompatActivity {
    public static int RESULT_CODE_OK = 1;
    public static int RESULT_CODE_ERROR = 2;
    public static String EXTRA_VK_ID = "vk_id";

    private static String KEY_LIST_ADAPTER = "list_adapter";
    private static String KEY_PROGRESS_BAR_VISIBILITY = "progress_bar_visivility";

    private ProgressBar progressBar;
    private RecyclerView userListView;
    private UserListRecyclerViewAdapter userListAdapter;

    private SaveContainer<AddUserActivity> saveContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        userListView = (RecyclerView) findViewById(R.id.userList);

        userListView.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            saveContainer = (SaveContainer<AddUserActivity>) getLastCustomNonConfigurationInstance();
            saveContainer.attachActivity(this);
            userListAdapter = saveContainer.get(KEY_LIST_ADAPTER);
            userListView.setAdapter(userListAdapter);
        } else {
            userListAdapter = new UserListRecyclerViewAdapter(Collections.<LocalUser>emptyList());
            userListAdapter.setClickListener(new OnElementClickListener<LocalUser>() {
                @Override
                public void onElementClick(View view, LocalUser element) {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_VK_ID, element.getVkId());
                    setResult(RESULT_CODE_OK, intent);
                    finish();
                }
            });
            userListView.setAdapter(userListAdapter);

            saveContainer = new SaveContainer<>();
            saveContainer.attachActivity(this);
            if (!VKSdk.isLoggedIn()) {
                VKSdk.login(this);
            } else {
                Helper.loadFriendsList(this);
            }
        }
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
    protected void onDestroy() {
        super.onDestroy();
        saveContainer.put(KEY_LIST_ADAPTER, userListAdapter);
        saveContainer.detachActivity();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return saveContainer;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Helper.onActivityResult(this, requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Используется для избежания утечки экземпляра Activity в анонимных классах
    private static class Helper {
        private static boolean onActivityResult(final AddUserActivity activity, int requestCode, int resultCode, Intent data) {
            final SaveContainer<AddUserActivity> saveContainer = activity.saveContainer;

            return (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
                @Override
                public void onResult(VKAccessToken res) {
                    saveContainer.addTask(new Task<AddUserActivity>() {
                        @Override
                        public void apply(AddUserActivity addUserActivity) {
                            loadFriendsList(activity);
                        }
                    });
                }
                @Override
                public void onError(VKError error) {
                    saveContainer.addTask(new Task<AddUserActivity>() {
                        @Override
                        public void apply(AddUserActivity addUserActivity) {
                            Toast.makeText(addUserActivity, R.string.authorization_error, Toast.LENGTH_LONG).show();
                            addUserActivity.setResult(RESULT_CODE_ERROR);
                            addUserActivity.finish();
                        }
                    });
                }
            }));
        }

        private static void loadFriendsList(AddUserActivity activity) {
            final SaveContainer<AddUserActivity> saveContainer = activity.saveContainer;
            activity.startProgress();

            VKRequest req = VKApi.friends().get(VKParameters.from("fields", "online,first_name,last_name,photo_200"));
            req.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(final VKResponse response) {
                    saveContainer.addTask(new Task<AddUserActivity>() {
                        @Override
                        public void apply(AddUserActivity addUserActivity) {
                            List<VKApiUserFull> vkUsersList = (List<VKApiUserFull>) response.parsedModel;
                            List<LocalUser> list = new ArrayList<LocalUser>();
                            for (VKApiUserFull vkUser : vkUsersList) {
                                list.add(new LocalUser(vkUser));
                            }
                            addUserActivity.userListAdapter.setUserList(list);
                            addUserActivity.stopProgress();
                        }
                    });
                }

                @Override
                public void onError(final VKError error) {
                    saveContainer.addTask(new Task<AddUserActivity>() {
                        @Override
                        public void apply(AddUserActivity addUserActivity) {
                            Toast.makeText(addUserActivity, error.errorMessage, Toast.LENGTH_LONG).show();
                            addUserActivity.setResult(RESULT_CODE_ERROR);
                            addUserActivity.finish();
                        }
                    });
                }
            });
        }
    }

    private void startProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void stopProgress() {
        progressBar.setVisibility(View.GONE);
    }
}
