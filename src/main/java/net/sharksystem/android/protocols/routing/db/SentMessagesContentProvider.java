package net.sharksystem.android.protocols.routing.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vividsolutions.jts.geom.Coordinate;

import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.SharkKBException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hirsch on 20.07.2016.
 */
public class SentMessagesContentProvider {

    private MySQLiteHelper dbHelper;

    private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
            MySQLiteHelper.COLUMN_MESSAGE_ID,
            MySQLiteHelper.COLUMN_PEER_ADDRESS};

    public SentMessagesContentProvider(Context context) {
        dbHelper = MySQLiteHelper.getInstance(context);
    }

    public List<String> getReceiversForMessage(MessageDTO message) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        List<String> receiverAddresses = new ArrayList<>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_SENT_MESSAGES, allColumns, MySQLiteHelper.COLUMN_MESSAGE_ID + "=" + message.getId(), null, null, null, null);
        while (!cursor.isAfterLast()) {
            receiverAddresses.add(cursor.getString(2));
            cursor.moveToNext();
        }

        cursor.close();
        dbHelper.close();
        return receiverAddresses;
    }

    public void persist(MessageDTO message, List<String> addressesToSend) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        for (String address : addressesToSend) {
            ContentValues values = new ContentValues();
            values.put(MySQLiteHelper.COLUMN_MESSAGE_ID, message.getId());
            values.put(MySQLiteHelper.COLUMN_PEER_ADDRESS, address);
            database.insert(MySQLiteHelper.TABLE_SENT_MESSAGES, null, values);
        }
        dbHelper.close();
    }
}
