package fr.ynryo.spotted.apiResponsesPOJO.region;

import androidx.annotation.NonNull;

public class BusTrackerRegionData {
    private int id;
    private String name;

    public BusTrackerRegionData(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return "BusTrackerRegionData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
