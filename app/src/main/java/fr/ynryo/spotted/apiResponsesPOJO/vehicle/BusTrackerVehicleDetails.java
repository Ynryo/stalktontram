package fr.ynryo.spotted.apiResponsesPOJO.vehicle;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import fr.ynryo.spotted.apiResponsesPOJO.markers.BusTrackerMarkerPosition;

//bus tracker api response
public class BusTrackerVehicleDetails {
    private String id;
    private int lineId;
    private String direction;
    private String destination;
    private List<BusTrackerVehicleStopDetails> calls;
    private BusTrackerMarkerPosition position;
    private int networkId;
    private String serviceDate;
    private String pathRef;
    private String updatedAt;
    private Context context;

    public String getId() {
        return id;
    }

    public int getLineId() {
        return lineId;
    }

    public String getDirection() {
        return direction;
    }

    public String getDestination() {
        return destination;
    }

    public List<BusTrackerVehicleStopDetails> getCalls() {
        return calls;
    }

    public BusTrackerMarkerPosition getPosition() {
        return position;
    }

    public int getNetworkId() {
        return networkId;
    }

    public String getServiceDate() {
        return serviceDate;
    }

    public String getPathRef() {
        return pathRef;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "VehicleDetails{" +
                "id='" + id + '\'' +
                ", lineId=" + lineId +
                ", direction='" + direction + '\'' +
                ", destination='" + destination + '\'' +
                ", calls=" + calls +
                ", position=" + position +
                ", networkId=" + networkId +
                ", serviceDate='" + serviceDate + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", context=" + context +
                '}';
    }
}
