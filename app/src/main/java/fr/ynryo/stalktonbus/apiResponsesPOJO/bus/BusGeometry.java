package fr.ynryo.stalktonbus.apiResponsesPOJO.bus;

import androidx.annotation.NonNull;

public class BusGeometry {
    private Object p;

    public Object getGeometry() {
        return p;
    }

    @NonNull
    @Override
    public String toString() {
        return "BusGeometry{" +
                "geometry=" + p +
                '}';
    }
}
