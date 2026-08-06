package fr.ynryo.ouestcefdpdetram.genericMarkerDatas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleStop;
import fr.ynryo.ouestcefdpdetram.utils.Time;

public class MarkerDataStandardized {

    // ==================== DONNÉES D'IDENTIFICATION ====================
    private MarkerType markerType; // Type du véhicule (train ou bus/tram)
    private String id; // ID unique (numéro train ou id bus tracker)
    private int lineId; // Numéro de ligne (vehicleNumber pour train et lineNumber pour le reste)
    private String lineNumber; // Numéro de ligne pour l'affichage
    private String networkRef; // Référence réseau (ex: "SNCF", "RATP")
    private int networkId; // ID numérique du réseau (pour fetch logo)

    // ==================== DONNÉES D'AFFICHAGE ====================
    private String fillColor; // Couleur de remplissage du marqueur
    private String textColor; // Couleur du texte (numéro de ligne)

    // ==================== POSITION ET DIRECTION ====================
    private double latitude; // Latitude actuelle
    private double longitude; // Longitude actuelle
    private float bearing; // Azimut/direction du véhicule (0-360°)
    private String pathRef; // Référence du tracé
    private Object markerDataRoute; // Liste des points du tracé

    // ==================== DONNÉES DE VOYAGE ====================
    private String destination; // Destination finale
    private List<MarkerDataStop> stops; // Liste des arrêts à venir
    private boolean atStop;
    private float distanceTraveled;

    // ==================== MÉTADONNÉES ====================
    private boolean isFollowed; // Est-ce que l'utilisateur suit ce véhicule?
    private Time createdAt; // Quand ce marqueur a été créé
    private Time lastUpdatedAt; // Quand la position a été mise à jour
    private boolean detailsLoaded; // Les infos détaillées (stops) ont-ils été fetched?

    // ==================== SI TRAIN EN UM ====================
    private MarkerDataStandardized umA; // Train en UM A
    private MarkerDataStandardized umB; // Train en UM B

    private final static String TAG = "MarkerDataStandardized";
    private final static int NETWORK_ID_SNCF = 17;

    // ==================== CONSTRUCTEURS ====================
    public MarkerDataStandardized() {
        this.stops = new ArrayList<>();
        this.createdAt = Time.now();
        this.lastUpdatedAt = Time.now();
        this.isFollowed = false;
        this.detailsLoaded = false;
    }

    // ==================== CONVERSION ====================

    /**
     * Converts a {@link MarkerData} object into a {@link MarkerDataStandardized} object with the specified {@link MarkerType}.
     *
     * @param markerData the source {@link MarkerData} object containing the data to be converted
     * @param type       the {@link MarkerType} to be associated with the resulting {@link MarkerDataStandardized} object
     * @return a {@link MarkerDataStandardized} object populated with the data from the given {@link MarkerData} and the specified {@link MarkerType
     * }
     */
    public static MarkerDataStandardized createNewMarkerFrom(@NonNull MarkerData markerData, @NonNull MarkerType type) {
        MarkerDataStandardized marker = new MarkerDataStandardized();

        marker.markerType = type;
        marker.id = markerData.getId();
        marker.lineId = marker.isTrain() ? Integer.parseInt(markerData.getVehicleNumber()) : 0;
        marker.lineNumber = marker.isTrain() ? markerData.getVehicleNumber() : markerData.getLineNumber();
        marker.networkRef = markerData.getNetworkRef();
        marker.fillColor = markerData.getFillColor();
        marker.textColor = markerData.getColor();
        marker.latitude = markerData.getPosition().getLatitude();
        marker.longitude = markerData.getPosition().getLongitude();
        marker.bearing = markerData.getPosition().getBearing();
        marker.createdAt = Time.now();
        marker.lastUpdatedAt = Time.now();
        marker.detailsLoaded = false;

        return marker;
    }

    /**
     * Creates a standardized marker data instance from the given marker data, vehicle data, and marker type.
     *
     * @param markerData   the original marker data to be transformed into a standardized format, must not be null
     * @param vehicleData  the vehicle data to be associated with the marker, must not be null
     * @param type         the type of marker to be used for standardization, must not be null
     * @return a new instance of {@code MarkerDataStandardized} containing the standardized marker data
     */
    public static MarkerDataStandardized createNewMarkerFrom(@NonNull MarkerData markerData, @NonNull VehicleData vehicleData, @NonNull MarkerType type) {
        MarkerDataStandardized marker = createNewMarkerFrom(markerData, type);
        marker.setVehicleDetailsVehicleData(vehicleData);
        return marker;
    }

    // à la priorité sur les datas (bus tracker api)
    /**
     * Updates the details of the current vehicle instance using the given {@code VehicleData} object.
     * Populates various fields such as line ID, destination, network ID, path reference, stops,
     * and additional attributes related to the vehicle's journey and live data.
     *
     * @param vehicleData A non-null {@link VehicleData} object containing details such as line ID,
     *                    destination, network ID, path reference, and a list of vehicle stops.
     *                    Each stop may contain information about stop reference, stop name, platform name,
     *                    expected and aimed times, stop order, coordinates, distance traveled,
     *                    flags (e.g., NO_PICKUP, NO_DROPOFF), and other related metadata.
     */
    public void setVehicleDetailsVehicleData(@NonNull VehicleData vehicleData) {
        this.lineId = vehicleData.getLineId();
        this.destination = vehicleData.getDestination();
        this.networkId = vehicleData.getNetworkId();
        this.pathRef = vehicleData.getPathRef();
        this.atStop = vehicleData.getPosition().isAtStop();
        this.distanceTraveled = vehicleData.getPosition().getDistanceTraveled();

        if (vehicleData.getCalls() != null && !vehicleData.getCalls().isEmpty()) {
            this.stops = new ArrayList<>();
            for (int i = 0; i < vehicleData.getCalls().size(); i++) { //calls = stops
                VehicleStop vehicleStop = vehicleData.getCalls().get(i);
                MarkerDataStop stop = new MarkerDataStop();

                stop.setStopRef(vehicleStop.getStopUIC());
                stop.setStopName(vehicleStop.getStopName());
                stop.setPlatform(new StopPlatform(vehicleStop.getPlatformName(), vehicleStop.getStopUIC(), 100));
                Time aimedTime = Time.parse(vehicleStop.getAimedTime());
                Time expectedTime = Time.parse(vehicleStop.getExpectedTime());
                boolean onLive = expectedTime != null;

                stop.setOnLive(onLive);
                stop.setDelay(Time.calculateDelayMinutes(aimedTime, expectedTime));
                stop.setDepartureTime(onLive ? expectedTime : aimedTime);
                stop.setStopOrder(vehicleStop.getStopOrder());
                stop.setLongitude(vehicleStop.getLongitude());
                stop.setLatitude(vehicleStop.getLatitude());
                stop.setDistanceTraveled(vehicleStop.getDistanceTraveled());
                stop.setIsDepartureStop(vehicleStop.getDistanceTraveled() == 0);
                stop.setIsDestinationStop(i == vehicleData.getCalls().size() - 1);
                stop.setVehicle(this);

                if (vehicleStop.getFlags().contains("NO_PICKUP")) {
                    stop.setStopType(StopType.NO_PICKUP);
                } else if (vehicleStop.getFlags().contains("NO_DROPOFF")) {
                    stop.setStopType(StopType.NO_DROPOFF);
                } else {
                    stop.setStopType(StopType.BOTH);
                }

                this.stops.add(stop);
            }
        }

        this.detailsLoaded = true;
        this.lastUpdatedAt = Time.now();
    }

    // ==================== GETTERS ====================
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

    public String getFillColor() {
        return fillColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getBearing() {
        return bearing;
    }

    public String getDestination() {
        return destination;
    }

    public List<MarkerDataStop> getStops() {
        return stops != null ? stops : new ArrayList<>();
    }

    public boolean isAtStop() {
        return atStop;
    }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }

    public boolean isFollowed() {
        return isFollowed;
    }

    public Time getCreatedAt() {
        return createdAt;
    }

    public Time getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getPathRef() {
        return pathRef;
    }

    public Object getMarkerDataRoute() {
        return markerDataRoute;
    }

    public boolean isDetailsLoaded() {
        return detailsLoaded;
    }

    public MarkerDataStandardized getUmA() {
        return umA;
    }

    public MarkerDataStandardized getUmB() {
        return umB;
    }

    // ==================== SETTERS ====================

    /**
     * Sets the marker type for the current instance.
     *
     * @param markerType the MarkerType to be set
     */
    public void setMarkerType(MarkerType markerType) {
        this.markerType = markerType;
    }

    /**
     * Sets the unique identifier for this instance.
     *
     * @param id the identifier to be set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the line identifier for this object.
     *
     * @param lineId the identifier to be assigned to the line
     */
    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    /**
     * Sets the line number to the specified value.
     *
     * @param lineNumber the line number to be set
     */
    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Sets the network reference with the provided value.
     *
     * @param networkRef The identifier or reference of the network to be set.
     */
    public void setNetworkRef(String networkRef) {
        this.networkRef = networkRef;
    }

    /**
     * Sets the network identifier for the current instance.
     *
     * @param networkId the unique identifier of the network to be set
     */
    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    /**
     * Sets the fill color for the object.
     *
     * @param fillColor the color to use for filling, specified as a string
     */
    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    /**
     * Sets the text color.
     *
     * @param textColor the color to set for the text, specified as a string
     */
    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    /**
     * Updates the latitude for the marker and records the current timestamp.
     *
     * @param latitude The new latitude value to set. It is expected to follow the standard
     *                 geographic coordinate system, where valid values range between -90.0
     *                 and 90.0.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.lastUpdatedAt = Time.now();
    }

    /**
     * Updates the longitude for the marker and records the current timestamp.
     *
     * @param longitude The new longitude value to set. It is expected to follow the standard
     *                  geographic coordinate system, where valid values range
     *                  between -180.0 and 180.0.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.lastUpdatedAt = Time.now();
    }

    /**
     * Sets the bearing of the marker and updates the timestamp of the last modification.
     *
     * @param bearing The new bearing value to set. It represents the direction or angle
     *                the marker is facing, specified in degrees. Valid values typically
     *                range from 0.0 to 360.0, where 0.0 points to the north.
     */
    public void setBearing(float bearing) {
        this.bearing = bearing;
        this.lastUpdatedAt = Time.now();
    }

    /**
     * Sets the destination for the marker.
     *
     * @param destination The name of the destination. It represents the final
     *                    endpoint or target location associated with the marker.
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * Sets the list of stops associated with the marker and updates the detailsLoaded flag
     * based on the presence of valid stop data.
     *
     * @param stops The list of stops to associate with the marker. Each stop is represented
     *              by a MarkerDataStop object. Passing a null or empty list will mark the
     *              details as not loaded.
     */
    public void setStops(List<MarkerDataStop> stops) {
        this.stops = stops;
        this.detailsLoaded = (stops != null && !stops.isEmpty());
    }

    /**
     * Updates the followed status of the marker.
     *
     * @param followed The new followed status to set. A value of true indicates
     *                 that the marker is marked as followed, while false indicates
     *                 it is not followed.
     */
    public void setFollowed(boolean followed) {
        isFollowed = followed;
    }

    /**
     * Sets the creation timestamp for the marker data.
     *
     * @param createdAt The timestamp indicating when the marker data was created.
     *                  It is represented as an Instant object and should not be null.
     */
    public void setCreatedAt(Time createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Updates the timestamp indicating the last modification time for the marker data.
     *
     * @param lastUpdatedAt The timestamp of the last update. It is represented
     *                      as an Instant object and should not be null.
     */
    public void setLastUpdatedAt(Time lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /**
     * Sets the detailsLoaded flag, indicating whether additional details
     * for the marker data have been successfully loaded.
     *
     * @param detailsLoaded A boolean value representing the loaded status.
     *                      A value of true indicates that details are loaded,
     *                      while false indicates they are not loaded.
     */
    public void setDetailsLoaded(boolean detailsLoaded) {
        this.detailsLoaded = detailsLoaded;
    }

    /**
     * Sets the marker data route associated with the marker.
     *
     * @param markerDataRoute The data route object to associate with the marker.
     *                        It represents additional information regarding the
     *                        route or path that the marker is linked to.
     */
    public void setMarkerDataRoute(Object markerDataRoute) {
        this.markerDataRoute = markerDataRoute;
    }

    /**
     * Sets the first unit of a standardized marker forming a multiple-unit train (UM - Unité Multiple).
     *
     * @param umA The first unit of the train. It is represented as a MarkerDataStandardized object
     *            and encapsulates standardized data associated with the unit.
     */
    public void setUmA(MarkerDataStandardized umA) {
        this.umA = umA;
    }

    /**
     * Sets the second unit of a standardized marker forming a multiple-unit train (UM - Unité Multiple).
     *
     * @param umB The second unit of the train. It is represented as a MarkerDataStandardized object
     *            and encapsulates standardized data associated with the unit.
     */
    public void setUmB(MarkerDataStandardized umB) {
        this.umB = umB;
    }

    /**
     * Sets a pair of standardized marker units forming a multiple-unit train (UM - Unité Multiple).
     *
     * @param umA The first unit of the train. It is represented as a MarkerDataStandardized object
     *            and encapsulates standardized data associated with the unit.
     * @param umB The second unit of the train. It is represented as a MarkerDataStandardized object
     *            and encapsulates standardized data associated with the unit.
     */
    public void setUmPair(MarkerDataStandardized umA, MarkerDataStandardized umB) {
        this.setUmA(umA);
        this.setUmB(umB);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Determines whether the current marker represents a train.
     *
     * @return true if the marker type is TRAIN; false otherwise.
     */
    public boolean isTrain() {
        return markerType == MarkerType.TRAIN;
    }

    /**
     * Determines whether the current marker represents a vehicle.
     *
     * @return true if the marker type is BUS_TRAM; false otherwise.
     */
    public boolean isVehicle() {
        return markerType == MarkerType.BUS_TRAM;
    }

    /**
     * Determines whether the current marker represents a multiple-unit train (UM - Unité Multiple).
     * A marker is considered a multiple-unit train if it represents a train
     * and both unit components (umA and umB) are non-null.
     *
     * @return true if the marker represents a multiple-unit train; false otherwise.
     */
    public boolean isUm() {
        return isTrain() && umA != null && umB != null;
    }

    /**
     * Retrieves the next stop in the list of stops, if available.
     *
     * @return the next stop as a {@code MarkerDataStop} object if the stops list is not null or empty;
     *         otherwise, returns {@code null}.
     */
    @Nullable
    public MarkerDataStop getNextStop() {
        if (stops != null && !stops.isEmpty()) {
            return stops.get(0);
        }
        return null;
    }

    /**
     * Calculates and returns the number of remaining stops.
     *
     * @return the count of remaining stops, or 0 if the stops list is null.
     */
    public int getRemainingStopsCount() {
        return stops != null ? stops.size() : 0;
    }

    /**
     * Updates the position of an object with new latitude, longitude, and bearing values.
     *
     * @param newLatitude  the updated latitude value
     * @param newLongitude the updated longitude value
     * @param newBearing   the updated bearing value in degrees
     */
    public void updatePosition(double newLatitude, double newLongitude, float newBearing) {
        this.latitude = newLatitude;
        this.longitude = newLongitude;
        this.bearing = newBearing;
        this.lastUpdatedAt = Time.now();
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStandardized{" +
                "markerType=" + markerType +
                ", id='" + id + '\'' +
                ", lineId='" + lineId + '\'' +
                ", lineNumber='" + lineNumber + '\'' +
                ", networkRef='" + networkRef + '\'' +
                ", networkId=" + networkId +
                ", fillColor='" + fillColor + '\'' +
                ", textColor='" + textColor + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", bearing=" + bearing +
                ", markerDataRoute=" + markerDataRoute +
                ", destination='" + destination + '\'' +
                ", stops=" + stops +
                ", isFollowed=" + isFollowed +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", detailsLoaded=" + detailsLoaded +
                ", umA=" + (umA != null ? umA.getId() : "null") +
                ", umB=" + (umB != null ? umB.getId() : "null") +
                '}';
    }
}