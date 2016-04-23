package net.sharksystem.android.protocols.nfc.ux;

import junit.framework.TestCase;

/**
 * Created by m on 4/23/16.
 */
public class TdmaNfcUxHandlerTest extends TestCase {

    public void testGetRandomTimeout() throws Exception {
        final TdmaNfcUxHandler handler = new TdmaNfcUxHandler();

        for (int i = 0; i < 10; i++) {
            handler.getRandomTimeout();
        }
    }
}
