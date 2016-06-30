package net.sharksystem.android.protocols.routing.service;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharksystem.android.protocols.routing.Utils;
import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageContentProvider;
import net.sharksystem.android.protocols.routing.db.MessageDTO;

import org.json.JSONException;

import java.util.List;

public class MovingRouterLocationListener implements LocationListener {

    Location _lastLocation;
    CoordinateContentProvider _coordinateContentProvider;
    MessageContentProvider _messageContentProvider;
    Context _context;

    public MovingRouterLocationListener(String provider, Context context)
    {
        _coordinateContentProvider = new CoordinateContentProvider(context);
        _messageContentProvider = new MessageContentProvider(context);
        _lastLocation = new Location(provider);
        _context = context;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        _lastLocation.set(location);

        this.updateMovementProfile(location);
        this.checkMessages(location);
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    private void updateMovementProfile(Location location) {
        Coordinate coordinate = new Coordinate(location.getLatitude(), location.getLongitude());
        Geometry geometry = new GeometryFactory().createPoint(coordinate);
        if (!Utils.isInMovementProfile(geometry, _context)) {
            _coordinateContentProvider.persist(coordinate);
        }
    }

    private void checkMessages(Location location) {
        try {
            List<MessageDTO> messages = _messageContentProvider.getAllMessages();
            for (int i = messages.size() - 1; i >=0; i--) {
                MessageDTO message = messages.get(i);
                Geometry geometry = new WKTReader().read(message.getReceiverSpatial().getGeometry().getWKT());
                if (geometry instanceof Point) {
                    Location destination = new Location("");
                    destination.setLatitude(geometry.getCoordinate().x);
                    destination.setLongitude(geometry.getCoordinate().y);
                    if (location.distanceTo(destination) < 100) {
                        // TODO real broadcast
                        Toast.makeText(_context, "Message broadcast because destination reached", Toast.LENGTH_SHORT).show();
                        _messageContentProvider.delete(message);
                    } else {
                        this.decreaseChecks(message);
                    }
                } else if (geometry instanceof LineString) {
                    // TODO
                } else if (geometry instanceof Polygon) {
                    Geometry locationPoint = new GeometryFactory().createPoint(new Coordinate(location.getLatitude(), location.getLongitude()));
                    if (locationPoint.within(geometry)) {
                        // TODO real broadcast
                        Toast.makeText(_context, "Message broadcast because destination reached", Toast.LENGTH_SHORT).show();
                        _messageContentProvider.delete(message);
                    }
                    else {
                        this.decreaseChecks(message);
                    }
                }
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void decreaseChecks(MessageDTO message) throws JSONException, SharkKBException {
        long checks = message.getChecks();
        if (checks > 1) {
            checks--;
            message.setChecks(checks);
            _messageContentProvider.update(message);
        } else {
            // TODO real broadcast
            Toast.makeText(_context, "Message broadcast because time expired", Toast.LENGTH_SHORT).show();
            _messageContentProvider.delete(message);
        }
    }
}