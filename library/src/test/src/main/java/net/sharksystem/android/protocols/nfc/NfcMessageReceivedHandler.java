package net.sharksystem.android.protocols.nfc;

import android.nfc.Tag;

import net.sharkfw.protocols.RequestHandler;
import net.sharksystem.android.protocols.nfc.ux.NfcUxHandler;

import java.util.Arrays;

/**
 * Created by Mario Neises (mn-io) on 25.01.2016.
 */
public class NfcMessageReceivedHandler implements OnMessageReceived {

    public static final String EXCEPTION_STUB_NULL = "Stub must not be null";

    private RequestHandler handler;
    private final NfcMessageStub nfcMessageStub;
    private NfcUxHandler uxHandler;

    private byte[] byteBuffer;

    public NfcMessageReceivedHandler(NfcMessageStub nfcMessageStub) {
        if (nfcMessageStub == null) {
            throw new IllegalArgumentException(EXCEPTION_STUB_NULL);
        }

        this.nfcMessageStub = nfcMessageStub;
    }

    @Override
    public void handleMessageReceived(byte[] msg) {
        if (byteBuffer == null) {
            byteBuffer = concat(new byte[0], msg);
        } else {
            byteBuffer = concat(byteBuffer, msg);
        }

        getUxHandler().receiving(msg.length, byteBuffer.length);
    }

    public static byte[] concat(byte[] first, byte[] second) {
        final int newLength = first.length + second.length;
        byte[] result = Arrays.copyOf(first, newLength);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @Override
    public void handleError(Exception exception) {
        getUxHandler().handleErrorOnReceiving(exception);
    }

    @Override
    public void handleTagLost() {
        if (byteBuffer != null && byteBuffer.length > 0) {
            try {
                handler.handleMessage(byteBuffer, nfcMessageStub);
            } catch (Exception e) {
                getUxHandler().handleErrorOnReceiving(e);
            }
            byteBuffer = null;
        }
        getUxHandler().tagGoneOnReceiver();
    }

    @Override
    public void handleNewTag(Tag tag) {
    }

    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }

    private NfcUxHandler getUxHandler() {
        if (this.uxHandler != null) {
            return uxHandler;
        } else {
            uxHandler = new NfcUxHandler();
            return uxHandler;
        }
    }

    public void setUxHandler(NfcUxHandler uxHandler) {
        this.uxHandler = uxHandler;
    }
}
