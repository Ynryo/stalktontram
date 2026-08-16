package fr.ynryo.stalktonbus.apiResponsesPOJO.vehicle;

import androidx.annotation.NonNull;

import java.util.List;

public class BusTrackerVehicleStopDetails {
    private String aimedTime;
    private String expectedTime;
    private String stopRef;
    private String stopName;
    private int stopOrder;
    private double distanceTraveled;
    private double latitude;
    private double longitude;
    private String platformName;
    private String callStatus;
    private List<String> flags;

    public String getAimedTime() {
        return aimedTime;
    }

    public String getExpectedTime() {
        return expectedTime;
    }

    public String getStopUIC() {
        return stopRef.contains("SNCF:StopPoint") ? stopRef.split(":")[2].substring(0, 7) : null;
    }

    public String getStopName() {
        return stopName;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public double getDistanceTraveled() {
        return distanceTraveled;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public List<String> getFlags() {
        return flags;
    }

    @NonNull
    @Override
    public String toString() {
        return "BusTrackerVehicleStopDetails{" +
                "aimedTime='" + aimedTime + '\'' +
                ", expectedTime='" + expectedTime + '\'' +
                ", stopRef='" + stopRef + '\'' +
                ", stopName='" + stopName + '\'' +
                ", stopOrder=" + stopOrder +
                ", distanceTraveled=" + distanceTraveled +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", callStatus='" + callStatus + '\'' +
                ", flags=" + flags +
                '}';
    }
}
