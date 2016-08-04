package net.sharksystem.android.peer;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;

/**
 * Created by Hirsch on 30.06.2016.
 */
public interface KPListener {
    void onNewInterest(ASIPInterest interest);
    void onNewKnowledge(ASIPKnowledge knowledge);
    void onNewStringMessage(String message);
}
