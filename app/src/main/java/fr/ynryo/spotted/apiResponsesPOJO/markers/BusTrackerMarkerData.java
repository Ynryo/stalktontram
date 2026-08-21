package fr.ynryo.spotted.apiResponsesPOJO.markers;

import androidx.annotation.NonNull;

public class BusTrackerMarkerData {
    private String id;
    private String lineNumber;
    private String vehicleNumber;
    private BusTrackerMarkerPosition position;
    private String fillColor;
    private String color;

    public String getId() {
        return id;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public BusTrackerMarkerPosition getPosition() {
        return position;
    }

    public String getFillColor() {
        return fillColor;
    }

    public String getColor() {
        return color;
    }

    public String getNetworkRef() {
        if (id == null) return "";
        return id.split("::")[0];
    }

    @NonNull
    @Override
    public String toString() {
        return "BusTrackerMarkerData{" +
                "id='" + id + '\'' +
                ", lineNumber='" + lineNumber + '\'' +
                ", vehicleNumber='" + vehicleNumber + '\'' +
                ", position=" + position +
                ", fillColor='" + fillColor + '\'' +
                ", color='" + color + '\'' +
//                ", networkId=" + networkId +
                '}';
    }
}
