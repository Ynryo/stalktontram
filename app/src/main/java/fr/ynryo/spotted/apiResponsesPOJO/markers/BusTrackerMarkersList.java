package fr.ynryo.spotted.apiResponsesPOJO.markers;

import androidx.annotation.NonNull;

import java.util.List;

public class BusTrackerMarkersList {
    private List<BusTrackerMarkerData> items;

    // on a une classe à part parce que y'a que items dans la response api
    public List<BusTrackerMarkerData> getItems() {
        return items;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataResponse{" +
                "items=" + items +
                '}';
    }
}
