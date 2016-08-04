package net.sharksystem.android.protocols.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Build;

import net.sharkfw.asip.ASIPSpace;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.protocols.MessageStub;
import net.sharkfw.protocols.RequestHandler;
import net.sharkfw.system.SharkNotSupportedException;
import net.sharksystem.android.protocols.nfc.ux.NfcUxHandler;
import net.sharksystem.android.protocols.nfc.ux.TdmaNfcUxHandler;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by Mario Neises (mn-io) on 22.01.16.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NfcMessageStub implements MessageStub {

    public static final String SMART_CARD_IDENTIFIER = "SHARK NFC";

    public static final String EXCEPTION_NFC_NOT_SUPPORTED = "NFC is not supported by device";
    public static final String EXCEPTION_NFC_ANDROID_TOO_OLD = "NFC implementation requires at least android KITKAT API %2$d - Device API is %1$d";
    public static final String EXCEPTION_NFC_NOT_ENABLED = "NFC is not enabled in system settings";
    public static final String EXCEPTION_NFC_NO_ACTIVITY = "NFC needs an activity to bind to";

    private final NfcAdapter nfcAdapter;
    private final WeakReference<Activity> activity;
    private final NfcMessageReceivedHandler receivedRequestHandler;
    private final NfcMessageSendHandler sendRequestHandler;
    private boolean isStarted = false;

    public NfcMessageStub(Context context, WeakReference<Activity> activity) throws SharkProtocolNotSupportedException {
        if (activity == null || activity.get() == null) {
            throw new IllegalStateException(EXCEPTION_NFC_NO_ACTIVITY);
        }

        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            String msg = String.format(EXCEPTION_NFC_ANDROID_TOO_OLD, Build.VERSION.SDK_INT, Build.VERSION_CODES.KITKAT);
            throw new SharkProtocolNotSupportedException(msg);
        }
        if (this.nfcAdapter == null) {
            throw new SharkProtocolNotSupportedException(EXCEPTION_NFC_NOT_SUPPORTED);
        }
        if (!nfcAdapter.isEnabled()) {
            throw new IllegalStateException(EXCEPTION_NFC_NOT_ENABLED);
        }

        receivedRequestHandler = new NfcMessageReceivedHandler(this);
        sendRequestHandler = new NfcMessageSendHandler();
    }

    @Override
    public void setHandler(RequestHandler handler) {
        receivedRequestHandler.setHandler(handler);
    }

    @Override
    public void stop() {
        NfcAdapterHelper.actAsSmartCard(SMART_CARD_IDENTIFIER, activity.get(), sendRequestHandler, receivedRequestHandler);
        isStarted = false;
    }

    @Override
    public void start() {
        NfcAdapterHelper.actAsNfcReaderWriter(SMART_CARD_IDENTIFIER, activity.get(), sendRequestHandler, receivedRequestHandler);
        isStarted = true;
    }

    @Override
    public boolean started() {
        return isStarted;
    }

    @Override
    public void offer(ASIPSpace asipSpace) throws SharkNotSupportedException {
    }

    @Override
    public void offer(Knowledge knowledge) throws SharkNotSupportedException {

    }

    @Override
    public void setReplyAddressString(String addr) {

    }

    @Override
    public void sendMessage(byte[] msg, String recAddress) throws IOException {
        sendRequestHandler.setData(msg);
    }

    @Override
    public String getReplyAddressString() {
        return null;
    }

    public void setUxHandler(NfcUxHandler handler) {
        if (handler instanceof TdmaNfcUxHandler) {
            ((TdmaNfcUxHandler) handler).setNfcStub(this);
        }
        receivedRequestHandler.setUxHandler(handler);
        sendRequestHandler.setUxHandler(handler);
    }
}
