package net.sharksystem.android.protocols.routing;

import android.content.Context;
import android.text.TextUtils;

import com.vividsolutions.jts.geom.Geometry;
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

import org.json.JSONException;

import java.io.IOException;

public class RouterKP extends KnowledgePort {

    CoordinateContentProvider coordinateContentProvider;
    MessageContentProvider _messageContentProvider;
    Context _context;

    public RouterKP(SharkEngine se, Context context) {
        super(se);
        coordinateContentProvider = new CoordinateContentProvider(context);
        _messageContentProvider = new MessageContentProvider(context);
        _context = context;
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

        boolean persist = false;

        try {
            if (msg.getReceiverSpatial() != null && this.isInMovementProfile(msg.getReceiverSpatial())) {
                persist = true;
            }
            else if (msg.getReceiverTime() != null && !this.isTimeSpanInPast(msg.getReceiverTime())) {
                persist = true;
            }

            if (persist) {
                try {
                    _messageContentProvider.persist(msg);
                } catch (JSONException | SharkKBException | IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (SharkKBException | ParseException e) {
            e.printStackTrace();
        }
    }

    private boolean isInMovementProfile(SpatialSemanticTag spatialSemanticTag) throws SharkKBException, ParseException {
        SpatialAlgebra algebra = new SpatialAlgebra();
        String wkt = spatialSemanticTag.getGeometry().getWKT();
        String ewkt = spatialSemanticTag.getGeometry().getEWKT();

        if (!TextUtils.isEmpty(wkt) && algebra.isValidWKT(wkt)) {
            Geometry destination = new WKTReader().read(wkt);
            return Utils.isInMovementProfile(destination, _context);
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
