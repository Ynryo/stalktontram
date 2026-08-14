package fr.ynryo.stalktonbus.genericMarkerDatas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.stalktonbus.apiResponsesPOJO.markers.MarkerData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.vehicle.VehicleStop;
import fr.ynryo.stalktonbus.utils.Time;

public class MarkerDataStandardized {

    // ==================== DONNÉES D'IDENTIFICATION ====================
    private MarkerIdentity markerIdentity;

    // ==================== DONNÉES D'AFFICHAGE ====================
    private MarkerStyle markerStyle;

    // ==================== POSITION ET DIRECTION ====================
    private MarkerPosition markerPosition;

    // ==================== DONNÉES DE VOYAGE ====================
    private MarkerTrip markerTrip;

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
        this.markerIdentity = new MarkerIdentity();
        this.markerPosition = new MarkerPosition();
        this.markerStyle = new MarkerStyle();
        this.markerTrip = new MarkerTrip();
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

        boolean isTrain = (type == MarkerType.TRAIN);
        int lineId = 0;
        if (isTrain && markerData.getVehicleNumber() != null) {
            try {
                lineId = Integer.parseInt(markerData.getVehicleNumber());
            } catch (NumberFormatException ignored) {
            }
        }
        String lineNumber = isTrain ? markerData.getVehicleNumber() : markerData.getLineNumber();

        marker.markerIdentity = new MarkerIdentity(
                type,
                markerData.getId(),
                lineId,
                lineNumber,
                markerData.getNetworkRef()
        );
        if (markerData.getPosition() != null) {
            marker.markerPosition = new MarkerPosition(
                    markerData.getPosition().getLatitude(),
                    markerData.getPosition().getLongitude(),
                    markerData.getPosition().getBearing()
            );
        } else {
            marker.markerPosition = new MarkerPosition();
        }
        marker.markerStyle = new MarkerStyle(
                markerData.getColor(),
                markerData.getFillColor()
        );
        marker.createdAt = Time.now();
        marker.lastUpdatedAt = Time.now();
        marker.detailsLoaded = false;

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
        this.markerIdentity.setLineId(vehicleData.getLineId());
        this.markerTrip.setDestination(vehicleData.getDestination());
        this.markerIdentity.setNetworkId(vehicleData.getNetworkId());
        this.markerTrip.setPathRef(vehicleData.getPathRef());
        this.markerTrip.setAtStop(vehicleData.getPosition().isAtStop());
        this.markerTrip.setDistanceTraveled(vehicleData.getPosition().getDistanceTraveled());

        if (vehicleData.getCalls() != null && !vehicleData.getCalls().isEmpty()) {
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

                this.markerTrip.getStops().add(stop);
            }
        }

        this.detailsLoaded = true;
        this.lastUpdatedAt = Time.now();
    }

    // ==================== GETTERS ====================
    public MarkerType getMarkerType() {
        return markerIdentity.getMarkerType();
    }

    public String getId() {
        return markerIdentity.getId();
    }

    public int getLineId() {
        return markerIdentity.getLineId();
    }

    public String getLineNumber() {
        return markerIdentity.getLineNumber();
    }

    public String getNetworkRef() {
        return markerIdentity.getNetworkRef();
    }

    public int getNetworkId() {
        return markerIdentity.getNetworkId();
    }

    public String getFillColor() {
        return markerStyle.getFillColor();
    }

    public String getTextColor() {
        return markerStyle.getTextColor();
    }

    public double getLatitude() {
        return markerPosition.getLatitude();
    }

    public double getLongitude() {
        return markerPosition.getLongitude();
    }

    public float getBearing() {
        return markerPosition.getBearing();
    }

    public String getDestination() {
        return markerTrip.getDestination();
    }

    public List<MarkerDataStop> getStops() {
        return markerTrip.getStops() != null ? markerTrip.getStops() : new ArrayList<>();
    }

    public boolean isAtStop() {
        return markerTrip.isAtStop();
    }

    public float getDistanceTraveled() {
        return markerTrip.getDistanceTraveled();
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
        return markerTrip.getPathRef();
    }

    public Object getMarkerDataRoute() {
        return markerTrip.getMarkerDataRoute();
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
        this.markerIdentity.setMarkerType(markerType);
    }

    /**
     * Sets the unique identifier for this instance.
     *
     * @param id the identifier to be set
     */
    public void setId(String id) {
        this.markerIdentity.setId(id);
    }

    /**
     * Sets the line identifier for this object.
     *
     * @param lineId the identifier to be assigned to the line
     */
    public void setLineId(int lineId) {
        this.markerIdentity.setLineId(lineId);
    }

    /**
     * Sets the line number to the specified value.
     *
     * @param lineNumber the line number to be set
     */
    public void setLineNumber(String lineNumber) {
        this.markerIdentity.setLineNumber(lineNumber);
    }

    /**
     * Sets the network reference with the provided value.
     *
     * @param networkRef The identifier or reference of the network to be set.
     */
    public void setNetworkRef(String networkRef) {
        this.markerIdentity.setNetworkRef(networkRef);
    }

    /**
     * Sets the network identifier for the current instance.
     *
     * @param networkId the unique identifier of the network to be set
     */
    public void setNetworkId(int networkId) {
        this.markerIdentity.setNetworkId(networkId);
    }

    /**
     * Sets the fill color for the object.
     *
     * @param fillColor the color to use for filling, specified as a string
     */
    public void setFillColor(String fillColor) {
        this.markerStyle.setFillColor(fillColor);
    }

    /**
     * Sets the text color.
     *
     * @param textColor the color to set for the text, specified as a string
     */
    public void setTextColor(String textColor) {
        this.markerStyle.setTextColor(textColor);
    }

    /**
     * Updates the latitude for the marker and records the current timestamp.
     *
     * @param latitude The new latitude value to set. It is expected to follow the standard
     *                 geographic coordinate system, where valid values range between -90.0
     *                 and 90.0.
     */
    public void setLatitude(double latitude) {
        this.markerPosition.setLatitude(latitude);
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
        this.markerPosition.setLongitude(longitude);
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
        this.markerPosition.setBearing(bearing);
        this.lastUpdatedAt = Time.now();
    }

    /**
     * Sets the destination for the marker.
     *
     * @param destination The name of the destination. It represents the final
     *                    endpoint or target location associated with the marker.
     */
    public void setDestination(String destination) {
        this.markerTrip.setDestination(destination);
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
        this.markerTrip.setStops(stops);
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
        this.markerTrip.setMarkerDataRoute(markerDataRoute);
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
        return markerIdentity.getMarkerType() == MarkerType.TRAIN;
    }

    /**
     * Determines whether the current marker represents a vehicle.
     *
     * @return true if the marker type is BUS_TRAM; false otherwise.
     */
    public boolean isVehicle() {
        return markerIdentity.getMarkerType() == MarkerType.BUS_TRAM;
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
     * otherwise, returns {@code null}.
     */
    @Nullable
    public MarkerDataStop getNextStop() {
        List<MarkerDataStop> stops = getStops();
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
        List<MarkerDataStop> stops = getStops();
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
        this.markerPosition.setLatitude(newLatitude);
        this.markerPosition.setLongitude(newLongitude);
        this.markerPosition.setBearing(newBearing);
        this.lastUpdatedAt = Time.now();
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStandardized{" +
                "markerIdentity=" + markerIdentity +
                ", markerStyle=" + markerStyle +
                ", markerPosition=" + markerPosition +
                ", markerTrip=" + markerTrip +
                ", isFollowed=" + isFollowed +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", detailsLoaded=" + detailsLoaded +
                ", umA=" + (umA != null ? umA.getId() : "null") +
                ", umB=" + (umB != null ? umB.getId() : "null") +
                '}';
    }
}