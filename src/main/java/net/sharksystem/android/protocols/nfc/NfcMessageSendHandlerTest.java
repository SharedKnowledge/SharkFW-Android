package net.sharksystem.android.protocols.nfc;

import junit.framework.TestCase;

/**
 * Created by Mario Neises (mn-io) on 25.01.2016.
 */
public class NfcMessageSendHandlerTest extends TestCase {

    public void testGetBytesFromBuffer() throws Exception {
        final NfcMessageSendHandler service = new NfcMessageSendHandler();

        service.setData("Hello World".getBytes());

        byte[] bytesFromBuffer = service.getBytesFromBuffer(5);
        assertEquals(bytesFromBuffer.length, "Hello".getBytes().length);

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertEquals(bytesFromBuffer.length, " Worl".getBytes().length);

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertEquals(bytesFromBuffer.length, "d".getBytes().length);

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertNull(bytesFromBuffer);


        service.setData("m".getBytes());

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertEquals(bytesFromBuffer.length, "m".getBytes().length);

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertNull(bytesFromBuffer);


        service.setData("".getBytes());

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertNull(bytesFromBuffer);


        service.setData(null);

        bytesFromBuffer = service.getBytesFromBuffer(5);
        assertNull(bytesFromBuffer);
    }
}