package net.sharksystem.android.protocols.nfc;

import android.nfc.Tag;

/**
 * Created by mn-io on 22.01.16.
 */
public interface OnMessageReceived {

    void handleMessageReceived(byte[] msg);

    void handleError(Exception exception);

    void handleTagLost();

    void handleNewTag(Tag tag);
}
