package net.sharksystem.android.protocols.nfc.ux;

import net.sharkfw.system.L;
import net.sharksystem.android.protocols.nfc.NfcMessageStub;

/**
 * Created by Mario Neises (mn-io) on 22.01.16.
 */
public class NfcUxHandler {

    public static final String LOG_NFC_PREPARED_SENDING = "nfc: sending - prepared with total data: %d bytes";
    public static final String LOG_NFC_PREPARED_SENDING_FAIL = "nfc: sending - preparation failed because buffer is not empty";
    public static final String LOG_NFC_SENDING = "nfc: sending %d bytes of %d, left: %d bytes";
    public static final String LOG_NFC_SENDING_INCOMPLETE = "nfc: sending was not done completely";
    public static final String LOG_NFC_TAG_GONE_SENDER = "nfc: sending tag gone";
    public static final String LOG_NFC_RECEIVING = "nfc: receiving %d bytes, total received: %d bytes";
    public static final String LOG_NFC_TAG_GONE_RECEIVER = "nfc: receiving tag gone";
    public static final String LOG_NFC_HANDLE_ERROR_RECEIVING = "nfc: receiving error occurred: ";

    private int totalDataLength;

    private NfcMessageStub nfcStub;

    public void preparedSending(int totalDataLength) {
        this.totalDataLength = totalDataLength;
        L.d(String.format(LOG_NFC_PREPARED_SENDING, totalDataLength), this);
    }

    public void preparedSendingFailed() {
        L.d(LOG_NFC_PREPARED_SENDING_FAIL, this);
    }

    public void sending(int currentDataLength, int leftDataLength) {
        L.d(String.format(LOG_NFC_SENDING, currentDataLength, totalDataLength, leftDataLength), this);
    }

    public void sendingNotDoneCompletely(byte[] buffer) {
        L.d(LOG_NFC_SENDING_INCOMPLETE, this);
    }

    public void tagGoneOnSender() {
        L.d(LOG_NFC_TAG_GONE_SENDER, this);
    }

    public void receiving(int currentDataLength, int newTotalDataLength) {
        L.d(String.format(LOG_NFC_RECEIVING, currentDataLength, newTotalDataLength), this);
    }

    public void tagGoneOnReceiver() {
        L.d(LOG_NFC_TAG_GONE_RECEIVER, this);
    }

    public void handleErrorOnReceiving(Exception exception) {
        L.d(LOG_NFC_HANDLE_ERROR_RECEIVING + exception.getMessage(), this);
    }
}
