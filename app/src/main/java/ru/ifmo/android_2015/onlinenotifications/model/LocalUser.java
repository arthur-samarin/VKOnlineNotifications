package ru.ifmo.android_2015.onlinenotifications.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.vk.sdk.api.model.VKApiUserFull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2015.onlinenotifications.util.Observable;

public class LocalUser extends Observable<LocalUser> {
    private static final String TABLE_NAME = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_AVATAR_URL = "avatar_url";
    private static final String COLUMN_ONLINE = "online";

    private enum State {
        NEW, NOT_MODIFIED, MODIFIED, DELETED, DESTROYED
    }

    private State state;
    private int vkId;
    private ValueHolder<String> fullName;
    private ValueHolder<String> avatarUrl;
    private ValueHolder<Boolean> online;

    public LocalUser(VKApiUserFull user) {
        this(user.id, user.first_name + " " + user.last_name, user.photo_200, user.online);
    }

    public LocalUser(int vkId, String fullName, String avatarUrl, boolean online) {
        this(State.NEW, vkId, fullName, avatarUrl, online);
    }

    private LocalUser(State state, int vkId, String fullName, String avatarUrl, boolean online) {
        this.state = state;
        this.vkId = vkId;
        this.fullName = new ValueHolder<>(fullName);
        this.avatarUrl = new ValueHolder<>(avatarUrl);
        this.online = new ValueHolder<>(online);
    }

    public int getVkId() {
        return vkId;
    }

    public ValueHolder<String> getFullName() {
        return fullName;
    }

    public ValueHolder<String> getAvatarUrl() {
        return avatarUrl;
    }

    public ValueHolder<Boolean> getOnline() {
        return online;
    }

    public boolean updateData(String fullName, String avatarUrl, boolean online) {
        boolean modified = false;
        modified |= this.fullName.setValue(fullName);
        modified |= this.avatarUrl.setValue(avatarUrl);
        modified |= this.online.setValue(online);

        if (modified && state == State.NOT_MODIFIED) {
            state = State.MODIFIED;
            fireChange(this);
        }
        return modified;
    }

    public void delete() {
        state = State.DELETED;
    }

    /*
     * Database logic
     */

    public static void createTable(SQLiteDatabase database) {
        String statement = "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INT PRIMARY KEY, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_AVATAR_URL + " TEXT, " +
                    COLUMN_ONLINE + " INT" +
                ")";
        database.execSQL(statement);
    }

    public static List<LocalUser> loadAll(SQLiteDatabase database) {
        String statement = "SELECT * FROM " + TABLE_NAME;
        List<LocalUser> users = new ArrayList<>();
        try (Cursor cursor = database.rawQuery(statement, null)) {
            while (cursor.moveToNext()) {
                users.add(fromDatabaseRaw(cursor));
            }
            return users;
        }
    }

    public static LocalUser fromDatabaseRaw(Cursor cursor) {
        int vkId = cursor.getInt(0);
        String fullName = cursor.getString(1);
        String avatarUrl = cursor.getString(2);
        boolean online = cursor.getInt(3) != 0;

        return new LocalUser(State.NOT_MODIFIED, vkId, fullName, avatarUrl, online);
    }

    public void storeChanges(SQLiteDatabase database) {
        if (state == State.MODIFIED) {
            updateColumnIfNotModified(database, fullName, COLUMN_NAME);
            updateColumnIfNotModified(database, avatarUrl, COLUMN_AVATAR_URL);
            updateColumnIfNotModified(database, online, COLUMN_ONLINE);
        } else if (state == State.DELETED) {
            deleteFromDatabase(database);
            state = State.DESTROYED;
        } else if (state == State.NEW) {
            insertIntoDatabase(database);
            state = State.NOT_MODIFIED;
        }
    }

    private void insertIntoDatabase(SQLiteDatabase database) {
        try (SQLiteStatement statement = database.compileStatement("INSERT INTO " + TABLE_NAME + "(" +
                    COLUMN_ID + ", " + COLUMN_NAME + ", " + COLUMN_AVATAR_URL + ", " + COLUMN_ONLINE +
                ") VALUES (?, ?, ?, ?)")) {
            statement.bindLong(1, vkId);
            statement.bindString(2, fullName.getValue());
            statement.bindString(3, avatarUrl.getValue());
            statement.bindLong(4, online.getValue() ? 1 : 0);
            if (statement.executeUpdateDelete() != 1) {
                throw new DatabaseException("Deleting " + vkId + " failed");
            }
        }
    }

    private void deleteFromDatabase(SQLiteDatabase database) {
        try (SQLiteStatement statement = database.compileStatement("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?")) {
            statement.bindLong(1, vkId);
            if (statement.executeUpdateDelete() != 1) {
                throw new DatabaseException("Deleting " + vkId + " failed");
            }
        }
    }

    private <T> void updateColumnIfNotModified(SQLiteDatabase database, ValueHolder<T> holder, String columnName) {
        try (SQLiteStatement statement = database.compileStatement("UPDATE " + TABLE_NAME + " SET " + columnName + " = ? WHERE " + COLUMN_ID + " = ?")) {
            bindToStatement(statement, 1, holder.getValue());
            statement.bindLong(2, vkId);
            if (statement.executeUpdateDelete() != 1) {
                throw new DatabaseException("Updating " + columnName + " of " + vkId + " failed");
            }
            holder.resetModifiedFlag();
        }
    }

    private static void bindToStatement(SQLiteStatement statement, int pos, Object obj) {
        if (obj == null)
            statement.bindNull(pos);
        else if (obj instanceof String)
            statement.bindString(pos, (String) obj);
        else if (obj instanceof Integer)
            statement.bindLong(pos, (Integer) obj);
        else if (obj instanceof byte[])
            statement.bindBlob(pos, (byte[]) obj);
    }
}
