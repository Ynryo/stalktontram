package fr.ynryo.stalktonbus.apiResponsesPOJO.bus;

import androidx.annotation.NonNull;

public class BusTrackerPath {
    private Object p;

    public Object getGeometry() {
        return p;
    }

    @NonNull
    @Override
    public String toString() {
        return "BusTrackerPath{" +
                "geometry=" + p +
                '}';
    }
}
