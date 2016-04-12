package net.sharksystem.android.protocols.nfc;

/**
 * Created by mn-io on 22.01.16.
 */
public interface OnMessageSend {

    byte[] getNextMessage();

    void onDeactivated(int reason);

    void setMaxSize(int size);
}
