package net.sharksystem.android.protocols.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.nfc.NfcAdapter;
import android.os.Build;

import net.sharksystem.android.protocols.nfc.androidService.SmartCardEmulationService;
import net.sharksystem.android.protocols.nfc.readerWriterMode.NfcReaderCallback;


/**
 * Created by mn-io on 23.01.2016.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NfcAdapterHelper {

    public static final int NFC_FLAGS = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

    /*
     * NFC is waiting for other NFC device to connect to.
     * Technically this device is actively trying to detect devices by electromagnetic induction,
     * which means it is "sending" energy in order to activate passive devices.
     */
    public static void actAsNfcReaderWriter(String smartCardIdentifier, Activity activity, OnMessageSend src, OnMessageReceived dst) {
        if (activity.isDestroyed()) {
            return;
        }

        final NfcReaderCallback nfcReaderCallback = new NfcReaderCallback(smartCardIdentifier, src, dst);
        getAdapter(activity).enableReaderMode(activity, nfcReaderCallback, NFC_FLAGS, null);
    }

    /*
     * NFC acts as a passive SmartCard, which contains data to send.
     * Technically this device is waiting to receive energy by electromagnetic induction.
     */
    public static void actAsSmartCard(String smartCardIdentifier, Activity activity, OnMessageSend src, OnMessageReceived dst) {
        if (activity.isDestroyed()) {
            return;
        }
        SmartCardEmulationService.setInitialHandshakeResponse(smartCardIdentifier);
        SmartCardEmulationService.setSource(src);
        SmartCardEmulationService.setSink(dst);
        getAdapter(activity).disableReaderMode(activity);
    }

    public static NfcAdapter getAdapter(Activity activity) {
        return NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
    }
}
