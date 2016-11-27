package net.sharksystem.android.protocols.routing;

/**
 * Created by Hirsch on 27.11.2016.
 */

public class RoutingServiceNotRunningException extends Exception {
    private static final long serialVersionUID = -6062567037164849703L;

    public RoutingServiceNotRunningException() {
        super();
    }

    public RoutingServiceNotRunningException(String s) {
        super(s);
    }

    public RoutingServiceNotRunningException(String msg, Throwable cause) {
        super(msg + cause.getMessage());
    }

}
