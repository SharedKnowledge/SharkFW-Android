package net.sharksystem.android.protocols.routing.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.asip.engine.ASIPMessage;
import net.sharkfw.asip.engine.ASIPSerializer;
import net.sharkfw.knowledgeBase.SharkKBException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MessageContentProvider {

    public static final long MAX_CHECKS = 2;

    private MySQLiteHelper dbHelper;
    private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
            MySQLiteHelper.COLUMN_VERSION,
            MySQLiteHelper.COLUMN_FORMAT,
            MySQLiteHelper.COLUMN_ENCRYPTED,
            MySQLiteHelper.COLUMN_ENCRYPTED_SESSION_KEY,
            MySQLiteHelper.COLUMN_SIGNED,
            //MySQLiteHelper.COLUMN_SIGNATURE,
            MySQLiteHelper.COLUMN_TTL,
            MySQLiteHelper.COLUMN_COMMAND,
            MySQLiteHelper.COLUMN_SENDER,
            MySQLiteHelper.COLUMN_RECEIVERS,
            MySQLiteHelper.COLUMN_RECEIVERPEER,
            MySQLiteHelper.COLUMN_RECEIVERLOCATION,
            MySQLiteHelper.COLUMN_RECEIVERTIME,
            MySQLiteHelper.COLUMN_CONTENT};

    public MessageContentProvider(Context context) {
        dbHelper = MySQLiteHelper.getInstance(context);
    }

    // TODO Signature
    public void persist(ASIPInMessage msg) throws JSONException, SharkKBException, IOException {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        String content;
        if (msg.getCommand() == ASIPMessage.ASIP_INSERT) {
            content = ASIPSerializer.serializeKnowledge(msg.getKnowledge()).toString();
        } else  if (msg.getCommand() == ASIPMessage.ASIP_EXPOSE) {
            content = ASIPSerializer.serializeInterest(msg.getInterest()).toString();
        } else {
            InputStream is = msg.getInputStream();
            content = IOUtils.toString(is);
            is.close();
        }

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_VERSION, msg.getVersion());
        values.put(MySQLiteHelper.COLUMN_FORMAT, msg.getFormat());
        values.put(MySQLiteHelper.COLUMN_ENCRYPTED, msg.isEncrypted());
        values.put(MySQLiteHelper.COLUMN_ENCRYPTED_SESSION_KEY, msg.getEncryptedSessionKey());
        values.put(MySQLiteHelper.COLUMN_SIGNED, msg.isSigned());
        //values.put(MySQLiteHelper.SIGNATURE, msg.getSignature());
        values.put(MySQLiteHelper.COLUMN_TTL, msg.getTtl());
        values.put(MySQLiteHelper.COLUMN_COMMAND, msg.getCommand());
        values.put(MySQLiteHelper.COLUMN_SENDER, msg.getSender() != null ? ASIPSerializer.serializeTag(msg.getSender()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERS, msg.getReceivers() != null ? ASIPSerializer.serializeSTSet(msg.getReceivers()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERPEER, msg.getReceiverPeer() != null ? ASIPSerializer.serializeTag(msg.getReceiverPeer()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERLOCATION, msg.getReceiverSpatial() != null ? ASIPSerializer.serializeTag(msg.getReceiverSpatial()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERTIME, msg.getReceiverTime() != null ? ASIPSerializer.serializeTag(msg.getReceiverTime()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_CONTENT, content);

        database.insert(MySQLiteHelper.TABLE_MESSAGES, null, values);

        dbHelper.close();
    }

    // TODO Signature
    public void update(MessageDTO msg) throws JSONException, SharkKBException {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_VERSION, msg.getVersion());
        values.put(MySQLiteHelper.COLUMN_FORMAT, msg.getFormat());
        values.put(MySQLiteHelper.COLUMN_ENCRYPTED, msg.isEncrypted());
        values.put(MySQLiteHelper.COLUMN_ENCRYPTED_SESSION_KEY, msg.getEncryptedSessionKey());
        values.put(MySQLiteHelper.COLUMN_SIGNED, msg.isSigned());
        //values.put(MySQLiteHelper.SIGNATURE, msg.getSignature());
        values.put(MySQLiteHelper.COLUMN_TTL, msg.getTtl());
        values.put(MySQLiteHelper.COLUMN_COMMAND, msg.getCommand());
        values.put(MySQLiteHelper.COLUMN_SENDER, msg.getSender() != null ? ASIPSerializer.serializeTag(msg.getSender()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERS, msg.getReceivers() != null ? ASIPSerializer.serializeSTSet(msg.getReceivers()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERPEER, msg.getReceiverPeer() != null ? ASIPSerializer.serializeTag(msg.getReceiverPeer()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERLOCATION, msg.getReceiverSpatial() != null ? ASIPSerializer.serializeTag(msg.getReceiverSpatial()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_RECEIVERTIME, msg.getReceiverTime() != null ? ASIPSerializer.serializeTag(msg.getReceiverTime()).toString() : "");
        values.put(MySQLiteHelper.COLUMN_CONTENT, msg.getContent());

        database.update(MySQLiteHelper.TABLE_MESSAGES, values, "_id="+msg.getId(), null);

        dbHelper.close();
    }

    public void delete(MessageDTO messageDTO) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        long id = messageDTO.getId();
        System.out.println("Message deleted with id: " + id);
        database.delete(MySQLiteHelper.TABLE_MESSAGES, MySQLiteHelper.COLUMN_ID + " = " + id, null);
        dbHelper.close();
    }

    public List<MessageDTO> getAllMessages() throws SharkKBException {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        List<MessageDTO> messageDTOs = new ArrayList<>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,  allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            MessageDTO messageDTO = cursorToMessage(cursor);
            messageDTOs.add(messageDTO);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        dbHelper.close();
        return messageDTOs;
    }

    private MessageDTO cursorToMessage(Cursor cursor) throws SharkKBException {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(cursor.getLong(0));
        messageDTO.setVersion(cursor.getString(1));
        messageDTO.setFormat(cursor.getString(2));
        messageDTO.setEncrypted(cursor.getInt(3) > 0);
        messageDTO.setEncryptedSessionKey(cursor.getString(4));
        messageDTO.setSigned(cursor.getInt(5) > 0);
        //messageDTO.setSignature(cursor.getString(6));
        messageDTO.setTtl(cursor.getLong(6));
        messageDTO.setCommand(cursor.getInt(7));
        messageDTO.setSender(ASIPSerializer.deserializePeerTag(cursor.getString(8)));
        //messageDTO.setReceivers(ASIPSerializer.deserializeSTSet(cursor.getString(9)));
        messageDTO.setReceiverPeer(ASIPSerializer.deserializePeerTag(cursor.getString(10)));
        messageDTO.setReceiverSpatial(ASIPSerializer.deserializeSpatialTag(cursor.getString(11)));
        messageDTO.setReceiverTime(ASIPSerializer.deserializeTimeTag(cursor.getString(12)));
        messageDTO.setContent(cursor.getString(13));

        //TODO content is just a string right now..can be interest or knowledge or raw though
        return messageDTO;
    }

}
