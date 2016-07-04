package net.sharksystem.android.protocols.routing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import net.sharkfw.asip.engine.ASIPConnection;
import net.sharkfw.asip.engine.ASIPInMessage;
import net.sharkfw.knowledgeBase.Knowledge;
import net.sharkfw.knowledgeBase.SharkCS;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.SpatialSemanticTag;
import net.sharkfw.knowledgeBase.TimeSemanticTag;
import net.sharkfw.knowledgeBase.geom.SpatialAlgebra;
import net.sharkfw.peer.KEPConnection;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.peer.SharkEngine;
import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageDTO;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class RouterKP extends KnowledgePort {
    public static final String TAG_COORDINATE_TTL = "coordinateTTL";

    public static final long DEFAULT_COORDINATE_TTL = 24 * 60 * 60 * 1000; //days in milliseconds
    public static final long DEFAULT_LOCATION_CHECK_INTERVAL = 2 * 60 * 1000;

    private SharkEngine mEngine;
    private Context mContext;

    private CoordinateContentProvider mCoordinateContentProvider;
    private MessageContentProvider mMessageContentProvider;

    private long mCoordinateTTL;

    private AlarmManager mAlarmManager;
    private PendingIntent mLocationIntent;

    private Runnable mRunnable;
    private Handler mHandler;

    private boolean mIsRouting;

    public static final int MESSAGE_CHECK_INTERVAL = 10000;

    public RouterKP(SharkEngine engine, Context context) {
        this(engine, context, DEFAULT_COORDINATE_TTL);
    }

    public RouterKP(SharkEngine engine, Context context, long coordinateTTL) {
        super(engine);

        mIsRouting = false;

        mEngine = engine;
        mContext = context;
        mCoordinateTTL = coordinateTTL;

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
    protected void handleInsert(Knowledge knowledge, KEPConnection kepConnection) {

    }

    @Override
    protected void handleExpose(SharkCS sharkCS, KEPConnection kepConnection) {

    }

    @Override
    protected void doProcess(ASIPInMessage msg, ASIPConnection con) {
        super.doProcess(msg, con);

        if (mIsRouting) {
            boolean persist = false;

            try {
                if (msg.getReceiverSpatial() != null && this.isMovementProfileCloser(msg.getReceiverSpatial())) {
                    persist = true;
                }
                else if (msg.getReceiverTime() != null && !this.isTimeSpanInPast(msg.getReceiverTime())) {
                    persist = true;
                }

                if (persist) {
                    try {
                        mMessageContentProvider.persist(msg);
                    } catch (JSONException | SharkKBException | IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (SharkKBException | ParseException e) {
                e.printStackTrace();
            }
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
        try {
            List<MessageDTO> messages = mMessageContentProvider.getAllMessages();
            for (int i = messages.size() - 1; i >=0; i--) {
                MessageDTO message = messages.get(i);

                if (message.getReceiverSpatial() != null) {
                    this.checkReceiverSpatial(message);
                } else if (message.getReceiverTime() != null) {
                    this.checkReceiverTime(message);
                } else if (message.getReceiverPeer() != null) {
                    // TODO
                }
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    private void checkReceiverSpatial(MessageDTO message) {
        try {
            Geometry geometry = new WKTReader().read(message.getReceiverSpatial().getGeometry().getWKT());
            Location lastLocation = LocationReceiver.getLastLocation();
            
            if (geometry instanceof Point) {
                Location destination = new Location("");
                destination.setLatitude(geometry.getCoordinate().x);
                destination.setLongitude(geometry.getCoordinate().y);
                if (lastLocation.distanceTo(destination) < 100) {

                    //TODO real broadcast
                    Toast.makeText(mContext, "Message broadcast because destination reached", Toast.LENGTH_SHORT).show();
                    mMessageContentProvider.delete(message);
                }
            } else if (geometry instanceof LineString) {
                // TODO
            } else if (geometry instanceof Polygon) {
                Geometry locationPoint = new GeometryFactory().createPoint(new Coordinate(lastLocation.getLatitude(), lastLocation.getLongitude()));
                if (locationPoint.within(geometry)) {
                    // TODO real broadcast
                    Toast.makeText(mContext, "Message broadcast because destination reached", Toast.LENGTH_SHORT).show();
                    mMessageContentProvider.delete(message);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void checkReceiverTime(MessageDTO message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= message.getReceiverTime().getFrom() && currentTime <= message.getReceiverTime().getFrom() + message.getReceiverTime().getDuration()) {
            // TODO real broadcast
            Toast.makeText(mContext, "Message broadcast because destination reached", Toast.LENGTH_SHORT).show();
            mMessageContentProvider.delete(message);
        }
    }

    private boolean isMovementProfileCloser(SpatialSemanticTag spatialSemanticTag) throws SharkKBException, ParseException {
        SpatialAlgebra algebra = new SpatialAlgebra();
        String wkt = spatialSemanticTag.getGeometry().getWKT();
        String ewkt = spatialSemanticTag.getGeometry().getEWKT();

        if (!TextUtils.isEmpty(wkt) && algebra.isValidWKT(wkt)) {
            Location locationTmp = LocationReceiver.getLastLocation();
            Geometry location = new GeometryFactory().createPoint(new Coordinate(locationTmp.getLatitude(), locationTmp.getLongitude()));
            Geometry destination = new WKTReader().read(wkt);
            Geometry convexHull = mCoordinateContentProvider.getConvexHull();

            return (destination.distance(convexHull) < destination.distance(location));
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
}
