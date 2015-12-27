package ru.ifmo.android_2015.onlinenotifications.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import ru.ifmo.android_2015.onlinenotifications.model.LocalUser;

public class UserListRecyclerViewAdapter extends RecyclerView.Adapter<UserListRecyclerViewAdapter.LocalUserViewHolder> {
    private List<LocalUser> userList;
    private OnElementClickListener<LocalUser> clickListener;

    public UserListRecyclerViewAdapter(List<LocalUser> userList) {
        this.userList = userList;
    }

    public void setUserList(List<LocalUser> userList) {
        this.userList = userList;
        notifyDataSetChanged();
    }

    @Override
    public LocalUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final UserView view = new UserView(parent.getContext(), null);
        final LocalUserViewHolder holder = new LocalUserViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    LocalUser user = holder.getDisplayedUser();
                    clickListener.onElementClick(view, user);
                }
            }
        });
        return holder;
    }

    public void setClickListener(OnElementClickListener<LocalUser> clickListener) {
        this.clickListener = clickListener;
    }

    public LocalUser getDisplayedUser(RecyclerView.ViewHolder holder) {
        LocalUserViewHolder h = (LocalUserViewHolder) holder;
        return h.getDisplayedUser();
    }

    @Override
    public void onBindViewHolder(LocalUserViewHolder holder, int position) {
        LocalUser user = userList.get(position);
        holder.display(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public List<LocalUser> getUserList() {
        return userList;
    }

    public static class LocalUserViewHolder extends RecyclerView.ViewHolder {
        private UserView userView;
        private LocalUser user;

        public LocalUserViewHolder(UserView itemView) {
            super(itemView);
            this.userView = itemView;
        }

        public void display(LocalUser user) {
            this.user = user;
            updateView();
        }

        private void updateView() {
            userView.setUserData(user);
        }

        public LocalUser getDisplayedUser() {
            return user;
        }
    }
}
