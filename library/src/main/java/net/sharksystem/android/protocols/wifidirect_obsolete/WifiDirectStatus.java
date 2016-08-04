package net.sharksystem.android.protocols.wifidirect_obsolete;

/**
 * Created by j4rvis on 03.02.16.
 */
public interface WifiDirectStatus {

    public static final int INITIATED = 0;
    public static final int DISCOVERING = 1;
    public static final int CONNECTED = 2;
    public static final int STOPPED = 3;
    public static final int DISCONNECTED = 4;

    public void onStatusChanged(int status);
}
