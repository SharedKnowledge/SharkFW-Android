package net.sharksystem.android.protocols.routing.db;

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

    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_FORMAT = "format";
    public static final String COLUMN_ENCRYPTED = "encrypted";
    public static final String COLUMN_ENCRYPTED_SESSION_KEY = "encrypted_session_key";
    public static final String COLUMN_SIGNED = "signed";
    public static final String COLUMN_TTL = "ttl";
    public static final String COLUMN_COPIES = "checks";
    public static final String COLUMN_COMMAND = "command";
    public static final String COLUMN_TOPIC = "topic";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_RECEIVERS = "receivers";
    public static final String COLUMN_SIGNATURE = "signature";
    public static final String COLUMN_RECEIVERPEER = "receiver_peer";
    public static final String COLUMN_RECEIVERLOCATION = "receiver_location";
    public static final String COLUMN_RECEIVERTIME = "receiver_time";
    public static final String COLUMN_CONTENT = "content";

    public static final String TABLE_SENT_MESSAGES = "sent_messages";
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_PEER_ADDRESS = "peer_address";

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

    private static final String DATABASE_CREATE_MESSAGES =
            "create table "
            + TABLE_MESSAGES + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_VERSION + " text, "
            + COLUMN_FORMAT + " text, "
            + COLUMN_ENCRYPTED + " bool, "
            + COLUMN_ENCRYPTED_SESSION_KEY + " text, "
            + COLUMN_SIGNED + " bool, "
            + COLUMN_TTL + " signed bigint, "
            + COLUMN_COPIES + " signed bigint, "
            + COLUMN_COMMAND + " integer, "
            + COLUMN_TOPIC + " text, "
            + COLUMN_TYPE + " text, "
            + COLUMN_SENDER + " text, "
            + COLUMN_RECEIVERS + " text, "
            // + COLUMN_SIGNATURE + " text, "
            + COLUMN_RECEIVERPEER + " text, "
            + COLUMN_RECEIVERLOCATION + " text, "
            + COLUMN_RECEIVERTIME +  " text, "
            + COLUMN_CONTENT + " text, "
            + COLUMN_INSERTION_DATE + " signed bigint);";

    private static final String DATABASE_CREATE_SENT_MESSAGES =
            "create table "
            + TABLE_SENT_MESSAGES + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_MESSAGE_ID + " integer, "
            + COLUMN_PEER_ADDRESS + " text);";

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
        database.execSQL(DATABASE_CREATE_MESSAGES);
        database.execSQL(DATABASE_CREATE_SENT_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COORDINATES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENT_MESSAGES);
        onCreate(db);
    }
}
