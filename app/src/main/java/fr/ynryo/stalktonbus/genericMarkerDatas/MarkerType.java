package fr.ynryo.stalktonbus.genericMarkerDatas;

public enum MarkerType {
    TRAIN("Train"),
    BUS_TRAM("Bus/Tram");

    private final String displayName;

    MarkerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Détermine le type en fonction de l'ID du marqueur.
     * Si l'ID contient "SNCF", c'est un train.
     *
     * @param markerId L'ID du marqueur
     * @return TRAIN si l'ID commence par "SNCF", sinon c'est un BUS_TRAM
     */
    public static MarkerType guestFromMarkerId(String markerId) {
        if (markerId != null && markerId.startsWith("SNCF")) {
            return TRAIN;
        }
        return BUS_TRAM;
    }
}