package net.sharksystem.android.peer;

import net.sharkfw.asip.ASIPInterest;
import net.sharkfw.asip.ASIPKnowledge;

/**
 * Created by Hirsch on 30.06.2016.
 */
public interface KPListener {
    public void onNewInterest(ASIPInterest interest);
    public void onNewKnowledge(ASIPKnowledge knowledge);
}
