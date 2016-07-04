package net.sharksystem.android.protocols.routing.db;

import com.vividsolutions.jts.geom.Coordinate;

public class CoordinateDTO {

    private long id;
    private float latitude;
    private float longitude;
    private long insertionDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public long getInsertionDate() {
        return insertionDate;
    }

    public void setInsertionDate(long insertionDate) {
        this.insertionDate = insertionDate;
    }

    public Coordinate toCoordinate() {
        return new Coordinate(this.latitude, this.longitude);
    }
}
