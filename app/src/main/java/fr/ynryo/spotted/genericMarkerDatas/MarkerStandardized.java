package fr.ynryo.spotted.genericMarkerDatas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.ynryo.spotted.apiResponsesPOJO.guessPlatform.CartoTchooGuessPlatform;
import fr.ynryo.spotted.apiResponsesPOJO.markers.BusTrackerMarkerData;
import fr.ynryo.spotted.apiResponsesPOJO.vehicle.BusTrackerVehicleDetails;
import fr.ynryo.spotted.apiResponsesPOJO.vehicle.BusTrackerVehicleStopDetails;
import fr.ynryo.spotted.utils.Time;

public class MarkerStandardized {

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
    private MarkerStandardized umA; // Train en UM A
    private MarkerStandardized umB; // Train en UM B

    private final static String TAG = "MarkerStandardized";
    private final static int NETWORK_ID_SNCF = 17;

    // ==================== CONSTRUCTEURS ====================
    public MarkerStandardized() {
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
     * Converts a {@link BusTrackerMarkerData} object into a {@link MarkerStandardized} object with the specified {@link MarkerType}.
     *
     * @param busTrackerMarkerData the source {@link BusTrackerMarkerData} object containing the data to be converted
     * @param type                 the {@link MarkerType} to be associated with the resulting {@link MarkerStandardized} object
     * @return a {@link MarkerStandardized} object populated with the data from the given {@link BusTrackerMarkerData} and the specified {@link MarkerType
     * }
     */
    public static MarkerStandardized createNewMarkerFrom(@NonNull BusTrackerMarkerData busTrackerMarkerData, @NonNull MarkerType type) {
        MarkerStandardized marker = new MarkerStandardized();

        boolean isTrain = (type == MarkerType.TRAIN);
        int lineId = 0;
        if (isTrain && busTrackerMarkerData.getVehicleNumber() != null) {
            try {
                lineId = Integer.parseInt(busTrackerMarkerData.getVehicleNumber());
            } catch (NumberFormatException ignored) {
            }
        }
        String lineNumber = isTrain ? busTrackerMarkerData.getVehicleNumber() : busTrackerMarkerData.getLineNumber();

        marker.markerIdentity = new MarkerIdentity(
                type,
                busTrackerMarkerData.getId(),
                lineId,
                lineNumber,
                busTrackerMarkerData.getNetworkRef()
        );
        if (busTrackerMarkerData.getPosition() != null) {
            marker.markerPosition = new MarkerPosition(
                    busTrackerMarkerData.getPosition().getLatitude(),
                    busTrackerMarkerData.getPosition().getLongitude(),
                    busTrackerMarkerData.getPosition().getBearing()
            );
        } else {
            marker.markerPosition = new MarkerPosition();
        }
        marker.markerStyle = new MarkerStyle(
                busTrackerMarkerData.getColor(),
                busTrackerMarkerData.getFillColor()
        );
        marker.createdAt = Time.now();
        marker.lastUpdatedAt = Time.now();
        marker.detailsLoaded = false;

        return marker;
    }

    // à la priorité sur les datas (bus tracker api)
    /**
     * Met à jour les détails de l'instance du véhicule actuel à l'aide de l'objet {@code BusTrackerVehicleDetails} fourni.
     * Remplit divers champs tels que l'identifiant de la ligne, la destination, l'identifiant du réseau, la référence de parcours, les arrêts,
     * ainsi que d'autres attributs liés au trajet du véhicule et aux données en temps réel.
     *
     * @param busTrackerVehicleDetails Un objet {@link BusTrackerVehicleDetails} non nul contenant les détails tels que l'identifiant de ligne,
     *                                 la destination, l'identifiant réseau, la référence de parcours et la liste des arrêts du véhicule.
     *                                 Chaque arrêt peut contenir des informations sur la référence d'arrêt, le nom de l'arrêt, le nom du quai,
     *                                 les horaires théoriques et prévus, l'ordre de l'arrêt, les coordonnées, la distance parcourue,
     *                                 les indicateurs (ex. : NO_PICKUP, NO_DROPOFF) et d'autres métadonnées associées.
     */
    public void setVehicleDetails(@NonNull BusTrackerVehicleDetails busTrackerVehicleDetails) {
        this.markerIdentity.setLineId(busTrackerVehicleDetails.getLineId());
        this.markerTrip.setDestination(busTrackerVehicleDetails.getDestination());
        this.markerIdentity.setNetworkId(busTrackerVehicleDetails.getNetworkId());
        this.markerTrip.setPathRef(busTrackerVehicleDetails.getPathRef());
        this.markerTrip.setAtStop(busTrackerVehicleDetails.getPosition().isAtStop());
        this.markerTrip.setDistanceTraveled(busTrackerVehicleDetails.getPosition().getDistanceTraveled());

        if (busTrackerVehicleDetails.getCalls().isEmpty()) return;
        this.markerTrip.getStops().clear();

        for (int i = 0; i < busTrackerVehicleDetails.getCalls().size(); i++) { //calls = stops
            BusTrackerVehicleStopDetails busTrackerVehicleStopDetails = busTrackerVehicleDetails.getCalls().get(i);

            Time aimedTime = Time.parse(busTrackerVehicleStopDetails.getAimedTime());
            Time expectedTime = Time.parse(busTrackerVehicleStopDetails.getExpectedTime());
            boolean isRealtime = expectedTime != null;

            String stopRef = isTrain() ? busTrackerVehicleStopDetails.getStopUIC() : busTrackerVehicleStopDetails.getStopRef();

            MarkerStop stop = new MarkerStop(
                    stopRef,
                    busTrackerVehicleStopDetails.getStopName(),
                    Time.calculateDelayMinutes(aimedTime, expectedTime),
                    isRealtime ? expectedTime : aimedTime,
                    busTrackerVehicleStopDetails.getStopOrder(),
                    busTrackerVehicleStopDetails.getLongitude(),
                    busTrackerVehicleStopDetails.getLatitude(),
                    busTrackerVehicleStopDetails.getDistanceTraveled(),
                    busTrackerVehicleStopDetails.getDistanceTraveled() == 0,
                    i == busTrackerVehicleDetails.getCalls().size() - 1,
                    this
            );

            if (busTrackerVehicleStopDetails.getPlatformName() != null) {
                stop.setPlatform(
                        new MarkerStopPlatform(busTrackerVehicleStopDetails.getPlatformName(), stopRef, 100)
                );
            }

            stop.setOnLive(isRealtime);

            if (busTrackerVehicleStopDetails.getFlags().contains("NO_PICKUP")) {
                stop.setStopType(StopType.NO_PICKUP);
            } else if (busTrackerVehicleStopDetails.getFlags().contains("NO_DROPOFF")) {
                stop.setStopType(StopType.NO_DROPOFF);
            } else {
                stop.setStopType(StopType.BOTH);
            }

            this.markerTrip.getStops().add(stop);
        }

        this.detailsLoaded = true;
        this.lastUpdatedAt = Time.now();
    }

    public void setGuessStopPlatform(@NonNull String uicCode, @NonNull List<CartoTchooGuessPlatform> guessPlatforms) {
        if (guessPlatforms.isEmpty()) return;

        // CartoTchooGuessPlatform bestGuessPlatform = guessPlatforms.get(0);
        CartoTchooGuessPlatform bestGuessPlatform = Collections.max(guessPlatforms, Comparator.comparingDouble(CartoTchooGuessPlatform::getPercentage));
        for (MarkerStop markerStop : this.markerTrip.getStops()) {
            if (uicCode.equals(markerStop.getStopRef())) {
                // secu pour éviter d'écraser un quai officiel à 100% par un guess platform
                if (markerStop.getPlatform() != null && markerStop.getPlatform().getPercentage() == 100) {
                    break;
                }
                markerStop.setPlatform(new MarkerStopPlatform(
                        bestGuessPlatform.getPlatform(),
                        uicCode,
                        bestGuessPlatform.getPercentage()));
                break;
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

    public List<MarkerStop> getStops() {
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

    public MarkerStandardized getUmA() {
        return umA;
    }

    public MarkerStandardized getUmB() {
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
     *              by a MarkerStop object. Passing a null or empty list will mark the
     *              details as not loaded.
     */
    public void setStops(List<MarkerStop> stops) {
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
     * @param umA The first unit of the train. It is represented as a MarkerStandardized object
     *            and encapsulates standardized data associated with the unit.
     */
    public void setUmA(MarkerStandardized umA) {
        this.umA = umA;
    }

    /**
     * Sets the second unit of a standardized marker forming a multiple-unit train (UM - Unité Multiple).
     *
     * @param umB The second unit of the train. It is represented as a MarkerStandardized object
     *            and encapsulates standardized data associated with the unit.
     */
    public void setUmB(MarkerStandardized umB) {
        this.umB = umB;
    }

    /**
     * Sets a pair of standardized marker units forming a multiple-unit train (UM - Unité Multiple).
     *
     * @param umA The first unit of the train. It is represented as a MarkerStandardized object
     *            and encapsulates standardized data associated with the unit.
     * @param umB The second unit of the train. It is represented as a MarkerStandardized object
     *            and encapsulates standardized data associated with the unit.
     */
    public void setUmPair(MarkerStandardized umA, MarkerStandardized umB) {
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
     * @return the next stop as a {@code MarkerStop} object if the stops list is not null or empty;
     * otherwise, returns {@code null}.
     */
    @Nullable
    public MarkerStop getNextStop() {
        List<MarkerStop> stops = getStops();
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
        List<MarkerStop> stops = getStops();
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
        return "MarkerStandardized{" +
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