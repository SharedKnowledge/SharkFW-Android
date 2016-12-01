package net.sharksystem.android.protocols.routing.location_to_be_implemented;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;
import net.sharksystem.android.protocols.routing.db.CoordinateDTO;

import java.util.List;

public class LocationReceiver extends BroadcastReceiver {

    public static final int MIN_TIME_REQUEST = 5 * 1000;
    private static Location mLastLocation;
    private static String _provider = LocationManager.NETWORK_PROVIDER;
    private static Context mContext;
    private static long mTimeToLive;
    private static LocationManager mLocationManager;

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
            mLastLocation = location;
            updateMovementProfile(location);
        }
    };

    // received request from the calling service
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.e("LOCATION", "onReceive");
        mContext = context;
        mTimeToLive = intent.getLongExtra(LocationService.TAG_COORDINATE_TTL, LocationService.DEFAULT_COORDINATE_TTL);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager.isProviderEnabled(_provider)) {
            mLocationManager.requestLocationUpdates(_provider, 0, 0, _locationListener);
        } else {
            Toast.makeText(context, "please turn on positioning stuff and give permissions", Toast.LENGTH_LONG).show();
        }
    }

    private static void updateMovementProfile(Location location) {
        Log.e("LOCATION", "new location");
        Coordinate coordinate = new Coordinate(location.getLatitude(), location.getLongitude());
        Geometry geometry = new GeometryFactory().createPoint(coordinate);
        CoordinateContentProvider contentProvider = new CoordinateContentProvider(mContext);

        //check if new coordinate is out of current movement profile. If yes, add it
        if (!contentProvider.getConvexHull().contains(geometry)) {
            contentProvider.persist(coordinate);
        }

        List<CoordinateDTO> coordinates = contentProvider.getAllCoordinates();
        //check coordinates of movement profile for their expiration
        for (CoordinateDTO coordinateDTO : coordinates) {
            if (System.currentTimeMillis() > coordinateDTO.getInsertionDate() + mTimeToLive) {
                contentProvider.deleteCoordinate(coordinateDTO);
            }
        }

        stopLocationListener();
    }

    public static void stopLocationListener() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(_locationListener);
        }

        if (mContext != null) {
            Toast.makeText(mContext, "provider stopped", Toast.LENGTH_SHORT).show();
        }
    }

    public static Location getLastLocation() {
        return mLastLocation;
    }
}
