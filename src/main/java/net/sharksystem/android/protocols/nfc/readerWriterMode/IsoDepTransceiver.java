package net.sharksystem.android.protocols.nfc.readerWriterMode;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import net.sharksystem.android.protocols.nfc.OnMessageReceived;
import net.sharksystem.android.protocols.nfc.OnMessageSend;
import net.sharksystem.android.protocols.nfc.androidService.SmartCardEmulationService;

import java.io.IOException;
import java.util.Arrays;


/**
 * Created by mn-io on 22.01.16.
 */
public class IsoDepTransceiver implements Runnable {

    public static final byte[] KEEP_CHANNEL_OPEN_SIGNAL_ACTIVE = {(byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFD,};
    public static final byte[] CLA_INS_P1_P2 = {0x00, (byte) 0xA4, 0x04, 0x00};
    public static final byte[] AID_ANDROID = {(byte) 0xF0, 0x01, 0x02, 0x03, 0x06, 0x06, 0x06}; // needs to be equal host-apdu-service > aid-filter
    public static final byte[] AID_APDU = createSelectAidApdu(CLA_INS_P1_P2, AID_ANDROID);
    public static final byte[] EMPTY_MSG = new byte[0];

    private final Thread thread;

    private byte[] initialHandshakeIdentifier;
    private IsoDep isoDep;
    private OnMessageReceived onMessageReceived;
    private OnMessageSend onMessageSend;

    public IsoDepTransceiver(String smartCardIdentifier, Tag tag, IsoDep isoDep, OnMessageReceived onMessageReceived, OnMessageSend onMessageSend) {
        if (smartCardIdentifier != null) {
            this.initialHandshakeIdentifier = smartCardIdentifier.getBytes();
        }

        this.isoDep = isoDep;
        this.onMessageReceived = onMessageReceived;

        if (onMessageSend != null) {
            this.onMessageSend = onMessageSend;
            onMessageSend.setMaxSize(isoDep.getMaxTransceiveLength());
        }

        onMessageReceived.handleNewTag(tag);

        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            isoDep.connect();
            byte[] response = isoDep.transceive(AID_APDU);
            if (!Arrays.equals(response, initialHandshakeIdentifier)) {
                return;
            }

            while (isoDep.isConnected() && !Thread.interrupted()) {
                handleTransceiving();
            }

            isoDep.close();
        } catch (TagLostException ignore) {
            onMessageReceived.handleTagLost();
        } catch (IOException e) {
            onMessageReceived.handleError(e);
        }
    }

    private void handleTransceiving() throws IOException {
        byte[] response;
        byte[] nextMessage;

        if (onMessageSend != null) {
            nextMessage = onMessageSend.getNextMessage();
        } else {
            nextMessage = KEEP_CHANNEL_OPEN_SIGNAL_ACTIVE;
        }

        if (nextMessage == null) {
            nextMessage = KEEP_CHANNEL_OPEN_SIGNAL_ACTIVE;
        }

        // tag lost exception if response is null => therefore always send data to allow bidirectional data flow
        response = isoDep.transceive(nextMessage);

        if (!Arrays.equals(SmartCardEmulationService.KEEP_CHANNEL_OPEN_SIGNAL_PASSIVE, response)) {
            onMessageReceived.handleMessageReceived(response);
        } else {
            onMessageReceived.handleMessageReceived(EMPTY_MSG);
        }
    }

    private static byte[] createSelectAidApdu(byte[] header, byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(header, 0, result, 0, header.length);
        result[4] = (byte) aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }

    public void interruptThread() {
        if (!thread.isInterrupted()) {
            thread.interrupt();
        }
    }
}
