package ru.ifmo.android_2015.onlinenotifications.ui;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import ru.ifmo.android_2015.onlinenotifications.R;
import ru.ifmo.android_2015.onlinenotifications.model.LocalUser;
import ru.ifmo.android_2015.onlinenotifications.model.ValueHolder;
import ru.ifmo.android_2015.onlinenotifications.util.Observable;

public class UserView extends FrameLayout {
    private LocalUser userData;
    private Observable.Observer<LocalUser> observer;

    private TextView userNameView;
    private HttpImageView userImageView;

    public UserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
        observer = new Observable.Observer<LocalUser>() {
            @Override
            public void onChanged(LocalUser value) {
                if (value == userData) {
                    updateView(userData);
                }
            }
        };
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_user, this);

        userNameView = (TextView) findViewById(R.id.userNameView);
        userImageView = (HttpImageView) findViewById(R.id.userImageView);
    }

    public void setUserData(LocalUser userData) {
        if (this.userData != null) {
            this.userData.unregisterListener(observer);
        }

        this.userData = userData;
        updateView(userData);

        userData.registerListener(observer);
    }

    private void updateView(LocalUser userData) {
        boolean online = userData.getOnline().getValue();
        Spanned text;
        if (online) {
            text = Html.fromHtml(userData.getFullName().getValue() + " <font color='gray'><i>" + getContext().getString(R.string.online) + "</i></font>");
        } else {
            text = Html.fromHtml(userData.getFullName().getValue());
        }

        userNameView.setText(text);
        userImageView.setUrl(userData.getAvatarUrl().getValue());
    }
}
