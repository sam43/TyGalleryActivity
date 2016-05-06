package com.android.gallery3d.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taoxj on 16-4-27.
 */
public class PictureDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = PictureDatabaseHelper.class.getSimpleName();
    static final int DB_VERSION = 1;

    private static final String SQL_CREATE_TABLE = "CREATE TABLE ";

    private static final String[][] CREATE_PHOTO = {
            { Pictures._ID, "INTEGER PRIMARY KEY AUTOINCREMENT" },
            { Pictures.PICTUREID, "TEXT DEFAULT ''" },
            { Pictures.URL, "TEXT" },
            { Pictures.USERPHOTOURL, "TEXT" },
            { Pictures.TAKEPICTURETIME, "TEXT NOT NULL" },
            { Pictures.ISDELETED, "INTEGER" },
            { Pictures.URI, "TEXT" },
            { Pictures.PATH, "TEXT NOT NULL" },
            { Pictures.TYPE, "TEXT NOT NULL" },
    };
    public PictureDatabaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public PictureDatabaseHelper(Context context, String name) {
        super(context, name, null,DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("koala", "database create");
        createTable(db, Pictures.TABLE, getPictureTableDefinition());
    }
    protected List<String[]> getPictureTableDefinition() {
        return tableCreationStrings(CREATE_PHOTO);
    }

    protected static void createTable(SQLiteDatabase db, String table, List<String[]> columns) {
        StringBuilder create = new StringBuilder(SQL_CREATE_TABLE);
        create.append(table).append('(');
        boolean first = true;
        for (String[] column : columns) {
            if (!first) {
                create.append(',');
            }
            first = false;
            for (String val: column) {
                create.append(val).append(' ');
            }
        }
        create.append(')');
        db.beginTransaction();
        try {
            db.execSQL(create.toString());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    protected static List<String[]> tableCreationStrings(String[][] createTable) {
        ArrayList<String[]> create = new ArrayList<String[]>(createTable.length);
        for (String[] line: createTable) {
            create.add(line);
        }
        return create;
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db);
    }

    private void recreate(SQLiteDatabase db) {
        dropTable(db, Pictures.TABLE);
        onCreate(db);
    }

    protected static void dropTable(SQLiteDatabase db, String table) {
        db.beginTransaction();
        try {
            db.execSQL("drop table if exists " + table);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
