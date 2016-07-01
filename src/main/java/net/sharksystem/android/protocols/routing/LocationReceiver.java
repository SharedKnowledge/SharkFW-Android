package net.sharksystem.android.protocols.routing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;

public class LocationReceiver extends BroadcastReceiver {

    public static final int MIN_TIME_REQUEST = 5 * 1000;
    private static String _provider = LocationManager.NETWORK_PROVIDER;
    private static Location _currentLocation;
    private static Location _prevLocation;
    private static Context _context;
    private static LocationManager _locationManager;

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
        Log.e("LOCATION", "onReceive");
        _context = context;
        _locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (_locationManager.isProviderEnabled(_provider)) {
            _locationManager.requestLocationUpdates(_provider, 0, 0, _locationListener);
        } else {
            Toast t = Toast.makeText(context, "please turn on positioning stuff", Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            Intent settingsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(settingsIntent);
        }
    }

    private static void handleLocation(Location location) {
        _prevLocation = _currentLocation == null ? null : new Location(_currentLocation);
        _currentLocation = location;

        Log.e("LOCATION", "new location");
        if (isLocationNew()) {
            Toast.makeText(_context, "new location received", Toast.LENGTH_SHORT).show();
            Coordinate coordinate = new Coordinate(_currentLocation.getLatitude(), _currentLocation.getLongitude());
            Geometry geometry = new GeometryFactory().createPoint(coordinate);
            CoordinateContentProvider _coordinateContentProvider = new CoordinateContentProvider(_context);

            if (!Utils.isInMovementProfile(geometry, _context)) {
                _coordinateContentProvider.persist(coordinate);
            }

            stopLocationListener();
        }
    }

    private static boolean isLocationNew() {
        if (_currentLocation == null || _prevLocation == null || _currentLocation.getTime() == _prevLocation.getTime()) {
            return false;
        }
        return true;
    }

    public static void stopLocationListener() {
        if (_locationManager != null) {
            _locationManager.removeUpdates(_locationListener);
        }

        if (_context != null) {
            Toast.makeText(_context, "provider stopped", Toast.LENGTH_SHORT).show();
        }
    }
}
