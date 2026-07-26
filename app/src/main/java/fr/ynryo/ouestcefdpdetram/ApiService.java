package fr.ynryo.ouestcefdpdetram;

import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.bus.BusGeometry;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.markers.MarkersList;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.region.RegionData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.version.VersionResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface de l'API, gère les requêtes et les réponses
 */
public interface ApiService {
    /**
     * API call vers https://bus-tracker.fr/api/vehicle-journeys/markers + params
     * @param swLat sud ouest latitude
     * @param swLon sud ouest longitude
     * @param neLat nord est latitude
     * @param neLon nord est longitude
     * @param lineId id de ligne optionnel
     */
    @GET("vehicle-journeys/markers")
    Call<MarkersList> getVehicleMarkers(
        @Query("swLat") double swLat,
        @Query("swLon") double swLon,
        @Query("neLat") double neLat,
        @Query("neLon") double neLon,
        @Query("lineId") String lineId
    );

    @GET("vehicle-journeys/{vehicleId}")
    Call<VehicleData> getVehicleDetails(
        @Path(value = "vehicleId", encoded = true) String vehicleId
    );

    @GET("regions")
    Call<List<RegionData>> getRegions();

    @GET("networks")
    Call<List<NetworkData>> getNetworks();

    @GET("networks/{networkId}?withDetails=true")
    Call<NetworkData> getNetworkData(
        @Path(value = "networkId") int networkId
    );

    @GET("paths/{pathRef}")
    Call<BusGeometry> getBusLine(
        @Path(value = "pathRef", encoded = true) String pathRef
    );

    @GET("version/latest")
    Call<VersionResponse> getLatestVersion();
}
