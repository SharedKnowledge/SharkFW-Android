package net.sharksystem.android.protocols.wifidirect;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.peer.SharkEngine;

/**
 * Created by j4rvis on 31.05.16.
 */
public class RadarKP extends net.sharkfw.kp.FilterKP {
    public RadarKP(SharkEngine se, ASIPInterest filter, KPNotifier notifier) {
        super(se, filter, notifier);
    }
}
