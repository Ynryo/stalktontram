package fr.ynryo.spotted.services;

import java.util.List;

import fr.ynryo.spotted.apiResponsesPOJO.bus.BusTrackerVehiclePath;
import fr.ynryo.spotted.apiResponsesPOJO.markers.BusTrackerMarkersList;
import fr.ynryo.spotted.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.spotted.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.spotted.apiResponsesPOJO.vehicle.BusTrackerVehicleDetails;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface de l'API, gère les requêtes et les réponses
 */
public interface BusTrackerApiService {
    /**
     * API call vers https://bus-tracker.fr/api/vehicle-journeys/markers + params
     * @param swLat sud ouest latitude
     * @param swLon sud ouest longitude
     * @param neLat nord est latitude
     * @param neLon nord est longitude
     * @param lineId id de ligne optionnel
     */
    @GET("vehicle-journeys/markers")
    Call<BusTrackerMarkersList> getVehicleMarkers(
        @Query("swLat") double swLat,
        @Query("swLon") double swLon,
        @Query("neLat") double neLat,
        @Query("neLon") double neLon,
        @Query("lineId") String lineId
    );

    @GET("vehicle-journeys/{vehicleId}")
    Call<BusTrackerVehicleDetails> getVehicleDetails(
        @Path(value = "vehicleId", encoded = true) String vehicleId
    );

    @GET("regions")
    Call<List<BusTrackerRegionData>> getRegions();

    @GET("networks")
    Call<List<BusTrackerNetworkData>> getNetworks();

    @GET("networks/{networkId}?withDetails=true")
    Call<BusTrackerNetworkData> getNetworkData(
        @Path(value = "networkId") int networkId
    );

    @GET("paths/{pathRef}")
    Call<BusTrackerVehiclePath> getPath(
        @Path(value = "pathRef", encoded = true) String pathRef
    );
}
