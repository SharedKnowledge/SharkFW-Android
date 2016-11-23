package net.sharksystem.android.protocols.routing;

import android.content.Context;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import net.sharksystem.android.protocols.routing.db.CoordinateContentProvider;
import net.sharksystem.android.protocols.routing.db.CoordinateDTO;

import java.util.List;

public class Utils {

    public static boolean isInMovementProfile(Geometry geometry, Context context) {
        List<CoordinateDTO> allCoordinateDTOs = new CoordinateContentProvider(context).getAllCoordinates();
        Coordinate[] coordinates = new Coordinate[allCoordinateDTOs.size()];
        for (int i = 0; i < allCoordinateDTOs.size(); i++) {
            coordinates[i] = allCoordinateDTOs.get(i).toCoordinate();
        }
        Geometry convexHull = new ConvexHull(coordinates, new GeometryFactory()).getConvexHull();

        return convexHull.intersects(geometry);
    }
}