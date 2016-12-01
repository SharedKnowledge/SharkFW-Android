package net.sharksystem.android.protocols.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.util.List;

public class LocationReceiver extends BroadcastReceiver {

    public static final String ACTION_REFRESH_SCHEDULE_ALARM = "net.sharksystem.android.ACTION_REFRESH_SCHEDULE_ALARM";;
    public static final String TAG_COORDINATE_TTL = "coordinateTTL";

    private static long mTimeToLive;
    private static LocationManager mLocationManager;
    private static CoordinateContentProvider mContentProvider;
    private static GeometryFactory mGeometryFactory;

    private static LocationListener _locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            handleLocation(location);
        }
    };

    // received request from the calling service
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (mContentProvider == null) {
            mContentProvider = new CoordinateContentProvider(context);
        }
        if (mGeometryFactory == null) {
            mGeometryFactory = new GeometryFactory();
        }

        String provider = LocationManager.NETWORK_PROVIDER;
        mTimeToLive = intent.getLongExtra(TAG_COORDINATE_TTL, LocationService.DEFAULT_COORDINATE_TTL);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager.isProviderEnabled(provider)) {
            mLocationManager.requestLocationUpdates(provider, 0, 0, _locationListener);
        } else {
            Toast.makeText(context, "please turn on positioning stuff and give permissions", Toast.LENGTH_LONG).show();
        }
    }

    private static void handleLocation(Location location) {
        Coordinate coordinate = new Coordinate(location.getLatitude(), location.getLongitude());
        Geometry geometry = mGeometryFactory.createPoint(coordinate);

        //check if new coordinate is out of current movement profile. If yes, add it
        if (!mContentProvider.getConvexHull().intersects(geometry)) {
            mContentProvider.persist(coordinate);
        }
        List<CoordinateDTO> coordinates = mContentProvider.getAllCoordinates();
        //check coordinates of movement profile for their expiration
        for (CoordinateDTO coordinateDTO : coordinates) {
            if (System.currentTimeMillis() > coordinateDTO.getInsertionDate() + mTimeToLive) {
                mContentProvider.deleteCoordinate(coordinateDTO);
            }
        }

        removeLocationListeners();
    }

    public static void removeLocationListeners() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(_locationListener);
        }
    }
}
