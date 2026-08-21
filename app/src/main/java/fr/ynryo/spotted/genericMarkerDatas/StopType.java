package fr.ynryo.spotted.genericMarkerDatas;

public enum StopType {
    NO_DROPOFF("Montée uniquement"),
    NO_PICKUP("Descente uniquement"),
    BOTH("");

    private final String displayName;

    StopType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}