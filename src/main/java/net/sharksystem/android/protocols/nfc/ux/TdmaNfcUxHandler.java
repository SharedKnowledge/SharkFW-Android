package net.sharksystem.android.protocols.nfc.ux;

import android.os.Handler;
import android.os.Looper;

import net.sharkfw.system.L;
import net.sharksystem.android.protocols.nfc.NfcMessageStub;

/**
 * Created by m on 4/23/16.
 */
public class TdmaNfcUxHandler extends NfcUxHandler {

    public static final String LOG_NFC_TDMA_AS_START_NEGOTIATION = "nfc: tdma - start negotiation";
    public static final String LOG_NFC_TDMA_AS_STOP_NEGOTIATION = "nfc: tdma - stop negotiation";
    public static final String LOG_NFC_TDMA_AS_READER = "nfc: tdma - calling start to act as NFC Reader for %d ms";
    public static final String LOG_NFC_TDMA_AS_SMARTCARD = "nfc: tdma - calling stop to act as NFC SmartCard for %d ms";

    public static final int[] TIMEOUTS = {1000, 2000, 500, 1500};

    private NfcMessageStub nfcStub;
    private Handler handler = new Handler(Looper.getMainLooper());

    boolean isNegotiating = false;

    private Runnable toggleNfcMode = new Runnable() {
        @Override
        public void run() {
            final int randomTimeout = getRandomTimeout();

            if (!nfcStub.started()) {
                L.d(String.format(LOG_NFC_TDMA_AS_READER, randomTimeout), this);
                nfcStub.start();
            } else {
                L.d(String.format(LOG_NFC_TDMA_AS_SMARTCARD, randomTimeout), this);
                nfcStub.stop();
            }

            handler.postDelayed(toggleNfcMode, randomTimeout);
        }
    };

    public void setNfcStub(NfcMessageStub nfcStub) {
        this.nfcStub = nfcStub;
    }

    public void startReaderModeNegotiation() {
        L.d(LOG_NFC_TDMA_AS_START_NEGOTIATION, this);
        L.d(String.format(LOG_NFC_TDMA_AS_SMARTCARD, TIMEOUTS[0]), this);
        isNegotiating = true;
        nfcStub.stop();
        handler.postDelayed(toggleNfcMode, TIMEOUTS[0]);
    }

    int getRandomTimeout() {
        final int randomIndex = (int) (Math.random() * 1000);
        final int randomTimeout = TIMEOUTS[randomIndex % TIMEOUTS.length];
        return randomTimeout;
    }

    public void stopNegotiation() {
        if (!isNegotiating) {
            return;
        }

        isNegotiating = false;
        L.d(LOG_NFC_TDMA_AS_STOP_NEGOTIATION, this);
        handler.removeCallbacks(toggleNfcMode);
    }

    //    @Override
//    public void preparedSending(int totalDataLength) {
//        super.preparedSending(totalDataLength);
//    }
//
//    @Override
//    public void preparedSendingFailed() {
//        super.preparedSendingFailed();
//    }
//
    @Override
    public void sending(int currentDataLength, int leftDataLength) {
        stopNegotiation();
        super.sending(currentDataLength, leftDataLength);
    }

    //
//    @Override
//    public void sendingNotDoneCompletely(byte[] buffer) {
//        super.sendingNotDoneCompletely(buffer);
//    }
//
//    @Override
//    public void tagGoneOnSender() {
//        super.tagGoneOnSender();
//    }
//
    @Override
    public void receiving(int currentDataLength, int newTotalDataLength) {
        stopNegotiation();
        super.receiving(currentDataLength, newTotalDataLength);
    }
//
//    @Override
//    public void tagGoneOnReceiver() {
//        super.tagGoneOnReceiver();
//    }
//
//    @Override
//    public void handleErrorOnReceiving(Exception exception) {
//        super.handleErrorOnReceiving(exception);
//    }
}
