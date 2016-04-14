package net.sharksystem.android.protocols.nfc.readerWriterMode;

import android.annotation.TargetApi;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;

import net.sharksystem.android.protocols.nfc.OnMessageReceived;
import net.sharksystem.android.protocols.nfc.OnMessageSend;


/**
 * Created by mn-io on 23.01.2016.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NfcReaderCallback implements NfcAdapter.ReaderCallback {
    private final String smartCardIdentifier;
    private final OnMessageReceived onMessageReceived;
    private final OnMessageSend onMessageSendCallback;

    private IsoDepTransceiver isoDepTransceiver;

    public NfcReaderCallback(String smartCardIdentifier, OnMessageSend onMessageSendCallback, OnMessageReceived onMessageReceived) {
        this.smartCardIdentifier = smartCardIdentifier;
        this.onMessageReceived = onMessageReceived;
        this.onMessageSendCallback = onMessageSendCallback;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            return;
        }

        if (isoDepTransceiver != null) {
            isoDepTransceiver.interruptThread();
        }

        isoDepTransceiver = new IsoDepTransceiver(smartCardIdentifier, tag, isoDep, onMessageReceived, onMessageSendCallback);
    }
}
