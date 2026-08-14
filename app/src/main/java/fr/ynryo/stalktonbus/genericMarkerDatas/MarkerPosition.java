package fr.ynryo.stalktonbus.genericMarkerDatas;

import androidx.annotation.NonNull;

public class MarkerPosition {
    private double latitude; //Latitude du point
    private double longitude; //Longitude du point
    private float bearing; //Orientation du point

    public MarkerPosition() {
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.bearing = 0.0f;
    }

    public MarkerPosition(double latitude, double longitude, float bearing) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getBearing() {
        return bearing;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerPosition{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", bearing=" + bearing +
                '}';
    }
}
