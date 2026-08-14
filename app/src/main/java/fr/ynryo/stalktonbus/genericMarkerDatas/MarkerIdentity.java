package fr.ynryo.stalktonbus.genericMarkerDatas;

import androidx.annotation.NonNull;

public class MarkerIdentity {
    private MarkerType markerType; // Type du véhicule (train ou bus/tram)
    private String id; // ID unique (numéro train ou id bus tracker)
    private int lineId; // Numéro de ligne (vehicleNumber pour train et lineNumber pour le reste)
    private String lineNumber; // Numéro de ligne pour l'affichage
    private String networkRef; // Référence réseau (ex: "SNCF", "RATP")
    private int networkId; // ID numérique du réseau (pour fetch logo)

    public MarkerIdentity() {
        this.markerType = MarkerType.BUS_TRAM;
        this.id = "";
        this.lineId = 0;
        this.lineNumber = "";
        this.networkRef = "";
        this.networkId = 0;
    }

    public MarkerIdentity(MarkerType markerType, String id, int lineId, String lineNumber, String networkRef) {
        this.markerType = markerType;
        this.id = id;
        this.lineId = lineId;
        this.lineNumber = lineNumber;
        this.networkRef = networkRef;
    }

    public MarkerIdentity(MarkerType markerType, String id, int lineId, String lineNumber, String networkRef, int networkId) {
        this(markerType, id, lineId, lineNumber, networkRef);
        this.networkId = networkId;
    }

    public MarkerType getMarkerType() {
        return markerType;
    }

    public String getId() {
        return id;
    }

    public int getLineId() {
        return lineId;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getNetworkRef() {
        return networkRef;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setMarkerType(MarkerType markerType) {
        this.markerType = markerType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setNetworkRef(String networkRef) {
        this.networkRef = networkRef;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerIdentity{" +
                "markerType=" + markerType +
                ", id='" + id + '\'' +
                ", lineId=" + lineId +
                ", lineNumber='" + lineNumber + '\'' +
                ", networkRef='" + networkRef + '\'' +
                ", networkId=" + networkId +
                '}';
    }
}
