package net.sharksystem.android.protocols.nfc;

import net.sharkfw.system.L;

/**
 * Created by mn-io on 22.01.16.
 */
public class NfcUXHandler {

    private int totalDataLength;

    public void preparedSending(int totalDataLength) {
        this.totalDataLength = totalDataLength;
        L.d(String.format("nfc: sending - prepared with total data: %d", totalDataLength));
    }

    public void preparedSendingFailed() {
        L.d("nfc: sending - preparation failed because buffer is not empty");

    }

    public void sending(int currentDataLength, int leftDataLength) {
        L.d(String.format("nfc: sending %d bytes of %d, left: %d", currentDataLength, totalDataLength, leftDataLength));
    }

    public void sendingNotDoneCompletely() {
        L.d("nfc: sending was not done completely");
    }

    public void tagGoneOnSender() {
        L.d("nfc: sending tag gone");
    }


    public void receiving(int currentDataLength, int newTotalDataLength) {
        L.d(String.format("nfc: receiving %d bytes, total received: %d", currentDataLength, newTotalDataLength));
    }

    public void tagGoneOnReceiver() {
        L.d("nfc: receiving tag gone");
    }

    public void handleErrorOnReceiving(Exception exception) {
        L.d("nfc: receiving error occured: " + exception.getMessage());
    }
}
