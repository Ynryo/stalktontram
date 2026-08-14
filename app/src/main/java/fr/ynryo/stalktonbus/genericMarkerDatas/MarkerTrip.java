package fr.ynryo.stalktonbus.genericMarkerDatas;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MarkerTrip {
    private String destination; // Destination finale
    private List<MarkerDataStop> stops; // Liste des arrêts à venir
    private boolean atStop;
    private float distanceTraveled;
    private String pathRef; // Référence du tracé
    private Object markerDataRoute; // Liste des points du tracé

    public MarkerTrip() {
        this.destination = "";
        this.stops = new ArrayList<>();
        this.atStop = false;
        this.distanceTraveled = 0;
        this.pathRef = "";
        this.markerDataRoute = null;
    }

    public MarkerTrip(String destination, List<MarkerDataStop> stops, boolean atStop, float distanceTraveled, String pathRef, Object markerDataRoute) {
        this.destination = destination;
        this.stops = stops;
        this.atStop = atStop;
        this.distanceTraveled = distanceTraveled;
        this.pathRef = pathRef;
        this.markerDataRoute = markerDataRoute;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public List<MarkerDataStop> getStops() {
        return stops;
    }

    public void setStops(List<MarkerDataStop> stops) {
        this.stops = stops;
    }

    public boolean isAtStop() {
        return atStop;
    }

    public void setAtStop(boolean atStop) {
        this.atStop = atStop;
    }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setDistanceTraveled(float distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }

    public String getPathRef() {
        return pathRef;
    }

    public void setPathRef(String pathRef) {
        this.pathRef = pathRef;
    }

    public Object getMarkerDataRoute() {
        return markerDataRoute;
    }

    public void setMarkerDataRoute(Object markerDataRoute) {
        this.markerDataRoute = markerDataRoute;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerTrip{" +
                "destination='" + destination + '\'' +
                ", stops=" + stops +
                ", atStop=" + atStop +
                ", distanceTraveled=" + distanceTraveled +
                ", pathRef='" + pathRef + '\'' +
                ", markerDataRoute=" + markerDataRoute +
                '}';
    }
}
