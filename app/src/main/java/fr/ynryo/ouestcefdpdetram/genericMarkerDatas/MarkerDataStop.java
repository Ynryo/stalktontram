package fr.ynryo.ouestcefdpdetram.genericMarkerDatas;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.ynryo.ouestcefdpdetram.utils.Time;

public class MarkerDataStop {
    private String stopRef; // Identifiant unique de l'arrêt
    private String stopName; // Nom de l'arrêt
    private String platformName; // Quai/Platform (ex: "A3", "Voie 2")
    private Time arrivalTime; // Heure d'arrivée
    private Time departureTime; // Heure de départ
    private Long delay; // Retard/décalage par rapport à l'horaire prévu
    private StopType stopType; // Type d'arrêt (PICKUP, DROPOFF)
    private double distanceTraveled; // Distance parcouru par le véhicule à cet arrêt
    private double latitude; // Latitude de l'arrêt
    private double longitude; // Longitude de l'arrêt
    private int stopOrder; // Position dans la liste des arrêts (0, 1, 2, ...)
    private boolean isOnLive; // Statut de l'appel (EXPECTED, ACTUAL, etc.)
    private boolean isDestinationStop = false;
    private boolean isDepartureStop = false;
    private MarkerDataStandardized vehicle; // Véhicle parent

    private final static String TAG = "MarkerDataStop";

    // ==================== CONSTRUCTEURS ====================
    public MarkerDataStop() {
        this.stopType = StopType.BOTH;
    }

    public MarkerDataStop(MarkerDataStop markerDataStop) {
        this.stopRef = markerDataStop.stopRef;
        this.stopName = markerDataStop.stopName;
        this.platformName = markerDataStop.platformName;
        this.arrivalTime = markerDataStop.arrivalTime;
        this.departureTime = markerDataStop.departureTime;
        this.delay = markerDataStop.delay;
        this.stopType = markerDataStop.stopType;
        this.distanceTraveled = markerDataStop.distanceTraveled;
        this.latitude = markerDataStop.latitude;
        this.longitude = markerDataStop.longitude;
        this.stopOrder = markerDataStop.stopOrder;
        this.isOnLive = markerDataStop.isOnLive;
        this.isDestinationStop = markerDataStop.isDestinationStop;
        this.isDepartureStop = markerDataStop.isDepartureStop;
        this.vehicle = markerDataStop.vehicle;
    }

    public MarkerDataStop(String stopRef, String stopName, Time arrivingTime, Time departureTime) {
        this.stopRef = stopRef;
        this.stopName = stopName;
        this.arrivalTime = arrivingTime;
        this.departureTime = departureTime;
        this.stopType = StopType.BOTH;
    }

    // ==================== GETTERS ====================
    public String getStopRef() {
        return stopRef;
    }

    public String getStopName() {
        return stopName;
    }

    public String getPlatformName() {
        return platformName;
    }

    @Nullable
    public Time getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Time arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @Nullable
    public Long getAtStopTime() {
        return Time.minutesBetween(arrivalTime, departureTime);
    }

    public Long getDelay() {
        return delay;
    }

    public StopType getStopType() {
        return stopType;
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

    public MarkerDataStandardized getVehicle() {
        return vehicle;
    }

    public boolean isOnLive() {
        return isOnLive;
    }

    public boolean isDepartureStop() {
        return isDepartureStop;
    }

    public boolean isDestinationStop() {
        return isDestinationStop;
    }

    // ==================== SETTERS ====================

    public void setStopRef(String stopRef) {
        this.stopRef = stopRef;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    @Nullable
    public Time getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Time departureTime) {
        this.departureTime = departureTime;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public void setStopType(StopType stopType) {
        this.stopType = stopType;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public void setDistanceTraveled(double distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setOnLive(boolean onLive) {
        this.isOnLive = onLive;
    }

    public void setIsDestinationStop(boolean isDestinationStop) {
        this.isDestinationStop = isDestinationStop;
    }

    public void setIsDepartureStop(boolean isDepartureStop) {
        this.isDepartureStop = isDepartureStop;
    }

    public void setVehicle(MarkerDataStandardized markerDataStandardized) {
        this.vehicle = markerDataStandardized;
    }

    public String getDelayText() {
        if (delay == null) {
            return "";
        }

        if (delay == 0) {
            return "À l'heure";
        }

        if (delay > 0) {
            return "Retard " + delay + " min";
        } else {
            return "Avance " + Math.abs(delay) + " min";
        }
    }

    public int getDelayColor() {
        if (delay == null) {
            return Color.GRAY;
        }

        if (delay == 0) {
            return Color.rgb(15, 150, 40);  // Vert
        }

        if (delay < 5) {
            return Color.rgb(224, 159, 7);  // Orange clair
        }

        if (delay < 15) {
            return Color.rgb(224, 112, 7);  // Orange foncé
        }

        return Color.RED;
    }

    public boolean cantDropoff() {
        return stopType == StopType.NO_DROPOFF;
    }

    public boolean cantPickup() {
        return stopType == StopType.NO_PICKUP;
    }

    public boolean isLate() {
        return delay != null && delay > 0;
    }

    public boolean isEarly() {
        return delay != null && delay < 0;
    }

    public boolean isOnTime() {
        return delay != null && delay == 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStop{" +
                "stopRef='" + stopRef + '\'' +
                ", stopName='" + stopName + '\'' +
                ", platformName='" + platformName + '\'' +
                ", arrivalTime='" + arrivalTime + '\'' +
                ", departureTime='" + departureTime + '\'' +
                ", delay=" + delay +
                ", stopType=" + stopType +
                ", distanceTraveled=" + distanceTraveled +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", stopOrder=" + stopOrder +
                ", isOnLive=" + isOnLive +
                ", isDestinationStop=" + isDestinationStop +
                ", isDepartureStop=" + isDepartureStop +
                '}';
    }
}