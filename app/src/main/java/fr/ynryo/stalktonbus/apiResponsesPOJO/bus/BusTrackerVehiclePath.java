package fr.ynryo.stalktonbus.apiResponsesPOJO.bus;

import androidx.annotation.NonNull;

public class BusTrackerVehiclePath {
    private Object p;

    public Object getGeometry() {
        return p;
    }

    @NonNull
    @Override
    public String toString() {
        return "BusTrackerVehiclePath{" +
                "geometry=" + p +
                '}';
    }
}
