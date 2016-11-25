package net.sharksystem.android.protocols.routing;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.asip.engine.ASIPOutMessage;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkCSAlgebra;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.ASIPPort;
import net.sharksystem.android.peer.AndroidSharkEngine;
import net.sharksystem.android.protocols.routing.db.MessageContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageDTO;
import net.sharksystem.android.protocols.routing.location.LocationService;

import java.util.ArrayList;
import java.util.List;

// TODO what about the start of the routing? the original sender needs to insert the message he wants to send to the RouterKP's database somehow
public class RouterKP extends ASIPPort {
    private static final String ROUTING_MESSAGE_ACCEPTED_STRING = "RoutingMessageAcceptedThisMessageShouldntBeSentByAUserOrHeHasAProblem";

    //-----------------------------------------------------------------------------
    //------------------------------- Objects -------------------------------------
    //-----------------------------------------------------------------------------
    private Handler mHandler;
    private Runnable mRunnable;
    private AndroidSharkEngine mEngine;
    private MessageContentProvider mMessageContentProvider;

    //-----------------------------------------------------------------------------
    //------------------------- Configuration Parameters --------------------------
    //-----------------------------------------------------------------------------
    private STSet mTopicsToRoute;
    private boolean mRouteAnyTopics;
    private int mMaxCopies;

    //-----------------------------------------------------------------------------
    //------------------------- Configuration Defaults-- --------------------------
    //-----------------------------------------------------------------------------
    private static final STSet DEFAULT_TOPICS_TO_ROUTE = null;
    private static final boolean DEFAULT_ROUTE_ANY_TOPICS = false;
    private static final int DEFAULT_MAX_COPIES = 10;
    private static final int MESSAGE_CHECK_INTERVAL = 2000;


    public RouterKP(AndroidSharkEngine engine, Context context) {
        this(engine, context, DEFAULT_TOPICS_TO_ROUTE, DEFAULT_ROUTE_ANY_TOPICS, DEFAULT_MAX_COPIES, LocationService.DEFAULT_COORDINATE_TTL);
    }

    public RouterKP(AndroidSharkEngine engine, Context context, STSet topics, boolean routeAnyTopics, int maxCopies, long coordinateTTL) {
        super(engine);

        mEngine = engine;

        mTopicsToRoute = (topics != null) ? topics : InMemoSharkKB.createInMemoSTSet();
        mRouteAnyTopics = routeAnyTopics;
        mMaxCopies = mMaxCopies;

        mMessageContentProvider = new MessageContentProvider(context);

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                checkMessagesToRoute();
                mHandler.postDelayed(mRunnable, MESSAGE_CHECK_INTERVAL);
            }
        };
    }

    public void startRouting() {
        Log.e("ROUTERKP", "Routing started");
        mHandler.post(mRunnable);
    }

    public void stopRouting() {
        Log.e("ROUTERKP", "Routing stopped");
        mHandler.removeCallbacks(mRunnable);
    }

    // TODO message == connection ???
    @Override
    public boolean handleMessage(ASIPInMessage message, ASIPConnection connection) {
//        super.doProcess(msg, con);

        boolean persist = false;
        boolean topicOk = false;
        boolean messageAlreadyStored = false;

        try {
            if (message.getTopic().isAny() && mRouteAnyTopics) {
                topicOk = true;
            } else if (mTopicsToRoute.isEmpty() || SharkCSAlgebra.isIn(mTopicsToRoute, message.getTopic())) {
                topicOk = true;
            }

            if (topicOk) {
                messageAlreadyStored = mMessageContentProvider.doesMessageAlreadyExist(message);
            }

            // TODO Spatial Routing, Peer Routing etc.
            if (topicOk && !messageAlreadyStored) {
                persist = true;

                if (persist) {
                    this.sendResponse(message, connection);
                    mMessageContentProvider.persist(message, mMaxCopies);
                }
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }


        // TODO can other KP's still handle this if true is returned?
        return persist;
    }

    // TODO return response to the physical sender, not the sender peer, related to AndroidSharkEngine.sendMessage
    // TODO how to return a short response that says that this certain, UNIQUE ASIPInMessage gets further routed by this RouterKP?
    // TODO implement method that waits for that response
    private void sendResponse(final ASIPInMessage message, final ASIPConnection connection) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ASIPOutMessage response = mEngine.createASIPOutMessage(message.getSender().getAddresses(), message.getSender());
                response.raw(ROUTING_MESSAGE_ACCEPTED_STRING.getBytes());
            }
        }).start();
    }

    // TODO Spatial Routing, Peer Routing etc.
    private void checkMessagesToRoute() {
        Log.e("ROUTERKP", "Checking messages");
        List<MessageDTO> allMessages = mMessageContentProvider.getAllMessages();

        for (int i = allMessages.size(); i >= 0; i--) {
            MessageDTO message = allMessages.get(i);
            this.forwardMessage(message);
        }
    }

    private void forwardMessage(MessageDTO message) {
        String[] nearbyPeerTCPAddresses = mEngine.getNearbyPeerTCPAddresses();
        List<String> previousReceiverAdresses = mMessageContentProvider.getReceiverAddresses(message);
        List<String> addressesToSend = new ArrayList<>();

        for (String address : nearbyPeerTCPAddresses) {
            if (!previousReceiverAdresses.contains(address)) {
                addressesToSend.add(address);
            }
        }

        mEngine.sendMessage(message, addressesToSend.toArray(new String[addressesToSend.size()]));

        // TODO update sentCopies only after routing response
        int sentCopies = message.getSentCopies() + addressesToSend.size();
        if (sentCopies > mMaxCopies) {
            Log.e("ROUTERKP", "Deleting message because max copies number exceeded");
            mMessageContentProvider.delete(message);
        } else {
            message.setSentCopies(sentCopies);
            mMessageContentProvider.updateReceiverAddresses(message, addressesToSend);
            mMessageContentProvider.update(message);
        }
    }

    public void setTopicsToRoute(STSet topics) {
        this.mTopicsToRoute = topics;
    }
}
