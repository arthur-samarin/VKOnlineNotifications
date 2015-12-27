package ru.ifmo.android_2015.onlinenotifications;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.ifmo.android_2015.onlinenotifications.model.LocalUser;

public class DatabaseManager extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "users";

    public DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LocalUser.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
