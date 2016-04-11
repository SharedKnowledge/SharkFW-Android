package net.sharksystem.android.protocols.wifidirect;

import java.io.IOException;

/**
 * Created by micha on 03.02.16.
 */
public interface StubController {
    public void onStubStart() throws IOException;
    public void onStubStop();
    public void onStubRestart();
}
