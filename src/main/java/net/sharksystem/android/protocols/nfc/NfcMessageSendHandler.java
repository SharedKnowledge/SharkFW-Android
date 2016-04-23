package net.sharksystem.android.protocols.nfc;

import java.util.Arrays;

/**
 * Created by mn-io on 25.01.2016.
 */
public class NfcMessageSendHandler implements OnMessageSend {
    public byte[] byteBuffer = null;
    private int size;
    private final Object lock = new Object();
    private NfcUXHandler uxHandler;

    @Override
    public byte[] getNextMessage() {
        final byte[] data = getBytesFromBuffer(size);
        return data;
    }

    @Override
    public void onDeactivated(int reason) {
        synchronized (lock) {
            if (byteBuffer != null && byteBuffer.length > 0) {
                getUxHandler().sendingNotDoneCompletely();
                return;
            }
            getUxHandler().tagGoneOnSender();
        }
    }

    @Override
    public void setMaxSize(int size) {
        this.size = size;
    }

    public void setData(byte[] data) {
        synchronized (lock) {
            this.byteBuffer = data;
            getUxHandler().preparedSending(data.length);
        }
    }

    public byte[] getBytesFromBuffer(int maxLength) {
        synchronized (lock) {
            if (byteBuffer == null || 0 == byteBuffer.length) {
                getUxHandler().sending(0, 0);
                byteBuffer = null;
                return null;
            }

            int length = Math.min(byteBuffer.length, maxLength);
            final byte[] currentBuffer = Arrays.copyOfRange(byteBuffer, 0, length);

            byteBuffer = Arrays.copyOfRange(byteBuffer, length, byteBuffer.length);
            getUxHandler().sending(currentBuffer.length, byteBuffer.length);
            return currentBuffer;
        }
    }

    private NfcUXHandler getUxHandler() {
        if (this.uxHandler != null) {
            return uxHandler;
        } else {
            uxHandler = new NfcUXHandler();
            return uxHandler;
        }
    }

    public void setUxHandler(NfcUXHandler uxHandler) {
        this.uxHandler = uxHandler;
    }
}
