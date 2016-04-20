package net.sharksystem.android.protocols.nfc;

import java.util.Arrays;

/**
 * Created by mn-io on 25.01.2016.
 */
public class NfcMessageSendHandler implements OnMessageSend {
    public static final String EXCEPTION_BUFFER_NOT_EMPTY = "Buffer not empty. Data loss on attempt to overwrite existing data";
    public static final String EXCEPTION_BUFFER_NOT_COMPLETELY_SENT = "Buffer not send entirely before deactivated";

    private byte[] byteBuffer = null;
    private int size;
    private final Object lock = new Object();

    @Override
    public byte[] getNextMessage() {
        final byte[] data = getBytesFromBuffer(size);
        return data;
    }

    @Override
    public void onDeactivated(int reason) {
        synchronized (lock) {
            if (byteBuffer != null && byteBuffer.length > 0) {
                throw new IllegalStateException(EXCEPTION_BUFFER_NOT_COMPLETELY_SENT);
            }
        }
    }

    @Override
    public void setMaxSize(int size) {
        this.size = size;
    }

    public void setData(byte[] data) {
        synchronized (lock) {
            if (byteBuffer != null && byteBuffer.length > 0) {
                throw new IllegalStateException(EXCEPTION_BUFFER_NOT_EMPTY);
            }
            this.byteBuffer = data;
        }
    }

    public byte[] getBytesFromBuffer(int maxLength) {
        synchronized (lock) {
            if (byteBuffer == null || 0 == byteBuffer.length) {
                byteBuffer = null;
                return null;
            }

            int length = Math.min(byteBuffer.length, maxLength);
            final byte[] currentBuffer = Arrays.copyOfRange(byteBuffer, 0, length);

            byteBuffer = Arrays.copyOfRange(byteBuffer, length, byteBuffer.length);
            return currentBuffer;
        }
    }
}
