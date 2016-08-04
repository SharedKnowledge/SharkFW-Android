package net.sharksystem.android.protocols.nfc;

import net.sharksystem.android.protocols.nfc.ux.NfcUxHandler;

import java.util.Arrays;

/**
 * Created by Mario Neises (mn-io) on 25.01.2016.
 */
public class NfcMessageSendHandler implements OnMessageSend {
    private byte[] byteBuffer = null;
    private int size;
    private final Object lock = new Object();
    private NfcUxHandler uxHandler;
    public static final String EXCEPTION_BUFFER_NOT_EMPTY = "Buffer not empty. Data loss on attempt to overwrite existing data";
    public static final String EXCEPTION_BUFFER_NOT_COMPLETELY_SENT = "Buffer not send entirely before deactivated";


    @Override
    public byte[] getNextMessage() {
        final byte[] data = getBytesFromBuffer(size);
        return data;
    }

    @Override
    public void onDeactivated(int reason) {
        synchronized (lock) {
            if (byteBuffer != null && byteBuffer.length > 0) {
                getUxHandler().sendingNotDoneCompletely(byteBuffer);
                this.byteBuffer = null;
                return;
            }
            this.byteBuffer = null;
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
            if (data != null) {
                getUxHandler().preparedSending(data.length);
            }
        }
    }

    public byte[] getBytesFromBuffer(int maxLength) {
        synchronized (lock) {
            if (byteBuffer == null || 0 == byteBuffer.length) {
                byteBuffer = null;
                getUxHandler().sending(0, 0);
                return null;
            }

            int length = Math.min(byteBuffer.length, maxLength);
            final byte[] currentBuffer = Arrays.copyOfRange(byteBuffer, 0, length);

            byteBuffer = Arrays.copyOfRange(byteBuffer, length, byteBuffer.length);
            getUxHandler().sending(currentBuffer.length, byteBuffer.length);
            return currentBuffer;
        }
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
