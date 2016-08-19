package net.sharksystem.android.ports;

import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.asip.engine.ASIPMessage;
import net.sharkfw.kp.KPNotifier;
import net.sharkfw.peer.ContentPort;
import net.sharkfw.peer.SharkEngine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by j4rvis on 21.07.16.
 */
public class RawMessagePort extends ContentPort {

    private ArrayList<KPNotifier> notifiers;

    public RawMessagePort(SharkEngine se, KPNotifier notifier) {
        super(se);

        this.notifiers = new ArrayList();
        if(notifier != null) {
            this.notifiers.add(notifier);
        }
    }

    public final void addNotifier(KPNotifier notifier) {
        if(!this.notifiers.contains(notifier) && notifier != null) {
            this.notifiers.add(notifier);
        }

    }

    public final void removeNotifier(KPNotifier notifier) {
        if(this.notifiers.contains(notifier) && notifier != null) {
            this.notifiers.remove(notifier);
        }

    }

    @Override
    protected boolean handleRaw(ASIPInMessage asipInMessage, ASIPConnection asipConnection, InputStream inputStream) {
        if(asipInMessage.getCommand() == ASIPMessage.ASIP_RAW){
            Iterator var4 = this.notifiers.iterator();

            while(var4.hasNext()) {
                KPNotifier notifier = (KPNotifier)var4.next();
                notifier.notifyRawReceived(inputStream, asipConnection);
            }
            return true;
        }
        return false;
    }
}
