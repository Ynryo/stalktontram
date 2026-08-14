package fr.ynryo.stalktonbus.apiResponsesPOJO.region;

import androidx.annotation.NonNull;

public class RegionData {
    private int id;
    private String name;

    public RegionData(int id, String name) {
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
        return "RegionData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
