package net.sharksystem.android.protocols.nfc;

import android.nfc.Tag;

import net.sharkfw.protocols.RequestHandler;

import java.util.Arrays;

/**
 * Created by mn-io on 25.01.2016.
 */
public class NfcMessageReceivedHandler implements OnMessageReceived {
    private RequestHandler handler;
    private NfcMessageStub nfcMessageStub;
    private byte[] byteBuffer;

    public NfcMessageReceivedHandler(NfcMessageStub nfcMessageStub) {
        this.nfcMessageStub = nfcMessageStub;
    }

    @Override
    public void onMessage(byte[] message) {
        if (byteBuffer == null) {
            byteBuffer = message;
        } else {
            byteBuffer = concat(byteBuffer, message);
        }
    }

    public static byte[] concat(byte[] first, byte[] second) {
        final int newLength = first.length + second.length;
        byte[] result = Arrays.copyOf(first, newLength);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @Override
    public void onError(Exception exception) {
    }

    @Override
    public void tagLost() {
        if (byteBuffer != null) {
            handler.handleMessage(byteBuffer, nfcMessageStub);
            byteBuffer = null;
        }
    }

    @Override
    public void newTag(Tag tag) {
    }


    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }


}
