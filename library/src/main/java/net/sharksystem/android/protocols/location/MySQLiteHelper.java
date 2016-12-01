package net.sharksystem.android.protocols.location;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

    private static MySQLiteHelper _instance = null;

    public static final String COLUMN_ID = "_id";

    public static final String TABLE_COORDINATES = "coordinates";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_INSERTION_DATE = "insertion_date";

    private static final String DATABASE_NAME = "movingrouter.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE_COORDINATES =
            "create table "
            + TABLE_COORDINATES + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_LATITUDE + " float, "
            + COLUMN_LONGITUDE + " float,"
            + COLUMN_INSERTION_DATE + " signed bigint);";


    public static MySQLiteHelper getInstance(Context context) {
        if (_instance == null) {
            _instance = new MySQLiteHelper(context.getApplicationContext());
        }

        return _instance;
    }

    private MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_COORDINATES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COORDINATES);
        onCreate(db);
    }
}
