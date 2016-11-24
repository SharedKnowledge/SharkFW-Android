package net.sharksystem.android.protocols.routing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.text.TextUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SharkCSAlgebra;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.SpatialSemanticTag;
import net.sharkfw.knowledgeBase.TimeSemanticTag;
import net.sharkfw.knowledgeBase.geom.SpatialAlgebra;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.ASIPPort;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkException;
import net.sharksystem.android.peer.AndroidSharkEngine;
import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageDTO;

import java.util.ArrayList;
import java.util.List;

public class RouterKP extends ASIPPort {
    public static final String TAG_COORDINATE_TTL = "coordinateTTL";

    public static final long DEFAULT_COORDINATE_TTL = 24 * 60 * 60 * 1000; //days in milliseconds
    public static final int DEFAULT_MIN_DISTANCE = 1000;

    public static final int DEFAULT_LOCATION_CHECK_INTERVAL = 2 * 60 * 1000;
    public static final int MESSAGE_CHECK_INTERVAL = 10000;

    private AndroidSharkEngine mEngine;
    private Context mContext;

    private CoordinateContentProvider mCoordinateContentProvider;
    private MessageContentProvider mMessageContentProvider;

    private long mCoordinateTTL;
    private AlarmManager mAlarmManager;

    private PendingIntent mLocationIntent;
    private Runnable mRunnable;

    private Handler mHandler;
    private boolean mIsRouting;

    //-----------------------------------------------------------------------------
    //------------------------- Configuration Parameters --------------------------
    //-----------------------------------------------------------------------------
    private STSet mTopicsToRoute;
    private boolean mRouteAnyTopics;
    private int mMaxCopies;

    //-----------------------------------------------------------------------------
    //------------------------- Configuration Defaults-- --------------------------
    //-----------------------------------------------------------------------------
    private static final int DEFAULT_MAX_COPIES = 10;


    public RouterKP(AndroidSharkEngine engine, Context context) {
        this(engine, context, null, false, DEFAULT_MAX_COPIES, DEFAULT_COORDINATE_TTL);
    }

    public RouterKP(AndroidSharkEngine engine, Context context, STSet topics, boolean routeAnyTopics, int maxCopies, long coordinateTTL) {
        super(engine);

        mIsRouting = false;
        mEngine = engine;
        mContext = context;
        mCoordinateTTL = coordinateTTL;

        mTopicsToRoute = (topics != null) ? topics : InMemoSharkKB.createInMemoSTSet();
        mRouteAnyTopics = routeAnyTopics;
        mMaxCopies = mMaxCopies;

        mCoordinateContentProvider = new CoordinateContentProvider(context);
        mMessageContentProvider = new MessageContentProvider(context);

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intentToFire = new Intent(mContext, LocationReceiver.class);
        intentToFire.putExtra(TAG_COORDINATE_TTL, mCoordinateTTL);
        mLocationIntent = PendingIntent.getBroadcast(mContext, 0, intentToFire, PendingIntent.FLAG_CANCEL_CURRENT);

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                checkMessagesToRoute();
                mHandler.postDelayed(mRunnable, MESSAGE_CHECK_INTERVAL);
            }
        };
    }

    @Override
    public boolean handleMessage(ASIPInMessage message, ASIPConnection connection) {
//        super.doProcess(msg, con);

        boolean persist = false;
        if (mIsRouting) {
            boolean topicOk = false;
            boolean messageAlreadyStored = false;
            persist = false;

            try {
                if (message.getTopic().isAny() && mRouteAnyTopics) {
                    topicOk = true;
                } else if (mTopicsToRoute.isEmpty() || SharkCSAlgebra.isIn(mTopicsToRoute, message.getTopic())) {
                    topicOk = true;
                }

                if (topicOk) {
                    messageAlreadyStored = mMessageContentProvider.doesMessageAlreadyExist(message);
                }

                if (topicOk && !messageAlreadyStored) {
                    if (message.getReceiverPeer() != null) {
                        persist = true;
                    } else if (message.getReceiverSpatial() != null && this.isMovementProfileCloser(message.getReceiverSpatial())) {
                        persist = true;
                    } else if (message.getReceiverTime() != null && !this.isTimeSpanInPast(message.getReceiverTime())) {
                        persist = true;
                    }

                    if (persist) {
                        this.sendResponse(message, connection);
                        mMessageContentProvider.persist(message, mMaxCopies);
                    }
                }
            } catch (SharkKBException | ParseException e) {
                e.printStackTrace();
            }
        }
        return persist;
    }

    private void sendResponse(ASIPInMessage message, ASIPConnection connection) {
        // TODO
        try {
            connection.expose(message.getInterest());
        } catch (SharkException e) {
            e.printStackTrace();
        }
    }

    public void startRouting() {
        mIsRouting = true;
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), DEFAULT_LOCATION_CHECK_INTERVAL, mLocationIntent);
        mHandler.postDelayed(mRunnable, MESSAGE_CHECK_INTERVAL);
    }

    public void stopRouting() {
        mIsRouting = false;
        mAlarmManager.cancel(mLocationIntent);
        LocationReceiver.stopLocationListener();
        mHandler.removeCallbacks(mRunnable);
    }

    private void checkMessagesToRoute() {
        List<MessageDTO> messages = mMessageContentProvider.getAllMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageDTO message = messages.get(i);
            if (message.getReceiverPeer() != null) {
                this.checkReceiverPeer(message);
            } else if (message.getReceiverSpatial() != null) {
                this.checkReceiverSpatial(message);
            } else if (message.getReceiverTime() != null) {
                this.checkReceiverTime(message);
            }
        }
    }

    private void checkReceiverPeer(MessageDTO message) {
        List<PeerSemanticTag> nearbyPeers = mEngine.getNearbyPeers();
        PeerSemanticTag receiver = message.getReceiverPeer();

        for (PeerSemanticTag peer : nearbyPeers) {
            if (peer.identical(receiver)) {
                mEngine.sendMessage(message, message.getReceiverPeer().getAddresses());
                mMessageContentProvider.delete(message);
                return;
            }
        }

        //Receiver is not nearby, so try to send it to as many new ppl as possible
        this.forwardMessage(message);
    }

    private void checkReceiverSpatial(MessageDTO message) {
        try {
            Geometry geometry = new WKTReader().read(message.getReceiverSpatial().getGeometry().getWKT());
            Point destinationPoint = geometry.getCentroid();
            Location destination = new Location("");
            destination.setLatitude(destinationPoint.getX());
            destination.setLongitude(destinationPoint.getY());

            if (destination.distanceTo(LocationReceiver.getLastLocation()) < 100) {
                this.forwardMessage(message);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void checkReceiverTime(MessageDTO message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= message.getReceiverTime().getFrom() && currentTime <= message.getReceiverTime().getFrom() + message.getReceiverTime().getDuration()) {
            this.forwardMessage(message);
        }
    }

    private void forwardMessage(MessageDTO message) {
        String[] nearbyPeerTCPAddresses = mEngine.getNearbyPeerTCPAddresses();
        List<String> previousReceiverAdresses = mMessageContentProvider.getReceivers(message);
        List<String> addressesToSend = new ArrayList<>();

        for (String address : nearbyPeerTCPAddresses) {
            if (!previousReceiverAdresses.contains(address)) {
                addressesToSend.add(address);
            }
        }

        mEngine.sendMessage(message, addressesToSend.toArray(new String[addressesToSend.size()]));
        mMessageContentProvider.updateReceivers(message, addressesToSend);
    }

    private boolean isMovementProfileCloser(SpatialSemanticTag spatialSemanticTag) throws SharkKBException, ParseException {
        SpatialAlgebra algebra = new SpatialAlgebra();
        String wkt = spatialSemanticTag.getGeometry().getWKT();
        String ewkt = spatialSemanticTag.getGeometry().getEWKT();

        if (!TextUtils.isEmpty(wkt) && algebra.isValidWKT(wkt)) {
            Point destinationPoint = new WKTReader().read(wkt).getCentroid();
            Location destination = new Location("");
            destination.setLatitude(destinationPoint.getX());
            destination.setLongitude(destinationPoint.getY());

            Point movementProfileCentroidPoint = mCoordinateContentProvider.getConvexHull().getCentroid();
            Location movementProfileCentroid = new Location("");
            movementProfileCentroid.setLatitude(movementProfileCentroidPoint.getX());
            movementProfileCentroid.setLongitude(movementProfileCentroidPoint.getY());

            Location lastLocation = LocationReceiver.getLastLocation();

            return (movementProfileCentroid.distanceTo(destination) < lastLocation.distanceTo(destination));
        } else if (!TextUtils.isEmpty(ewkt) && algebra.isValidEWKT(ewkt)) {
            // TODO there's no EWKT reader...mb also works with WKTREADER?
            return false;
        } else {
            // TODO throw exception cause of no valid wkt?
            return false;
        }
    }

    private boolean isTimeSpanInPast(TimeSemanticTag time) {
        return (time.getFrom() + time.getDuration()) < System.currentTimeMillis();
    }

    public void setTopicsToRoute(STSet topics) {

    }
}
