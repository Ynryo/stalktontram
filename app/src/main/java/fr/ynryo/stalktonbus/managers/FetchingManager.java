package fr.ynryo.stalktonbus.managers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.stalktonbus.ApiService;
import fr.ynryo.stalktonbus.MainActivity;
import fr.ynryo.stalktonbus.apiResponsesPOJO.bus.BusTrackerVehiclePath;
import fr.ynryo.stalktonbus.apiResponsesPOJO.guessPlatform.CartoTchooGuessPlatform;
import fr.ynryo.stalktonbus.apiResponsesPOJO.markers.BusTrackerMarkerData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.markers.BusTrackerMarkersList;
import fr.ynryo.stalktonbus.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.vehicle.BusTrackerVehicleDetails;
import fr.ynryo.stalktonbus.apiResponsesPOJO.version.YnryoVersionResponse;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerType;
import fr.ynryo.stalktonbus.managers.um.TrainUmAssembler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Classe gérant les requêtes et réponses de l'API et les conversions avec MarkerDataStandardized
 *
 * Author: Ynryo
 */
public class FetchingManager {
    private static final String TAG = "FetchingManager";
    private static final String BASE_URL_BUS_TRACKER = "https://bus-tracker.fr/api/";
    private static final String BASE_URL_DL_YNRYO = "https://dl.ynryo.fr/api/ouestcefdpdetram/";
    private static final String BASE_URL_API_TCHOO = "https://api.tchoo.net/api/";

    private final MainActivity context;
    private static ApiService busTrackerService;
    private static ApiService dlYnryoService;
    private static ApiService apiTchooService;

    public FetchingManager(MainActivity context) {
        this.context = context;
        if (busTrackerService == null) {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL_BUS_TRACKER).addConverterFactory(GsonConverterFactory.create()).build();
            busTrackerService = retrofit.create(ApiService.class);
        }

        if (dlYnryoService == null) {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL_DL_YNRYO).addConverterFactory(GsonConverterFactory.create()).build();
            dlYnryoService = retrofit.create(ApiService.class);
        }

        if (apiTchooService == null) {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL_API_TCHOO).addConverterFactory(GsonConverterFactory.create()).build();
            apiTchooService = retrofit.create(ApiService.class);
        }
    }

    // ==================== LISTENERS ====================
    public interface OnMarkersListener {
        void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList);

        void onErrorMarkersListener(String error);
    }

    public interface OnVehicleDetailsListener {
        void onResponseVehicleDetailsListener(MarkerDataStandardized markerDataStandardized);

        void onErrorVehicleDetailsListener(String error);
    }

    public interface OnNetworkDataListener {
        void onResponseNetworkDataListener(BusTrackerNetworkData data);

        void onErrorNetworkDataListener(String error);
    }

    public interface OnRouteLineListener {
        void onResponseRouteLineListener(MarkerDataStandardized data);

        void onErrorRouteLineListener(String error);
    }

    public interface OnNetworkListener {
        void onResponseNetworkListener(List<BusTrackerNetworkData> data);

        void onErrorNetworkListener(String error);
    }

    public interface OnRegionsListener {
        void onResponseRegionsListener(List<BusTrackerRegionData> regions);

        void onErrorRegionsListener(String error);
    }

    public interface OnVersionListener {
        void onResponseVersionListener(YnryoVersionResponse version);

        void onErrorVersionListener(String error);
    }

    public interface OnVehicleAliveListener {
        void onResponseVehicleAliveListener(boolean isAlive);

        void onErrorVehicleAliveListener(String error);
    }

    public interface OnGuessPlatformListener {
        void onResponseGuessPlatformListener(List<CartoTchooGuessPlatform> cartoTchooGuessPlatform);

        void onErrorGuessPlatformListener(String error);
    }

    private ApiService getService(String baseUrl) {
        switch (baseUrl) {
            case BASE_URL_BUS_TRACKER:
                return busTrackerService;

            case BASE_URL_DL_YNRYO:
                return dlYnryoService;

            case BASE_URL_API_TCHOO:
                return apiTchooService;

            default:
                return null;
        }
    }

    // ==================== FETCH MARKERS (PRINCIPAL) ====================
    public void fetchMarkers(OnMarkersListener listener) {
        fetchMarkers(null, listener);
    }

    public void fetchMarkers(String lineId, OnMarkersListener listener) {
        if (context.getMap() == null) return;

        LatLngBounds bounds = context.getMap().getProjection().getVisibleRegion().latLngBounds;
        getService(BASE_URL_BUS_TRACKER).getVehicleMarkers(bounds.southwest.latitude, bounds.southwest.longitude, bounds.northeast.latitude, bounds.northeast.longitude, lineId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BusTrackerMarkersList> call, @NonNull Response<BusTrackerMarkersList> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Convertir BusTrackerMarkerData en MarkerDataStandardized
                    List<MarkerDataStandardized> standardizedMarkers = convertMarkerDataList(response.body().getItems());
                    listener.onResponseMarkersListener(standardizedMarkers);
                } else {
                    listener.onErrorMarkersListener("Erreur réponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<BusTrackerMarkersList> call, @NonNull Throwable t) {
                listener.onErrorMarkersListener(t.getMessage());
            }
        });
    }

    // ==================== FETCH VEHICLE DETAILS ====================
    public void fetchVehicleStopsInfo(MarkerDataStandardized markerDataStandardized, OnVehicleDetailsListener listener) {
        try {
            if (markerDataStandardized.isUm()) {
                OnVehicleDetailsListener umListener = new OnVehicleDetailsListener() {
                    private int responsesReceived = 0;

                    @Override
                    public void onResponseVehicleDetailsListener(MarkerDataStandardized data) {
                        responsesReceived++;
                        if (responsesReceived == 2) {
                            TrainUmAssembler.assembleUmStops(markerDataStandardized);
                            listener.onResponseVehicleDetailsListener(markerDataStandardized);
                        }
                    }

                    @Override
                    public void onErrorVehicleDetailsListener(String error) {
                        listener.onErrorVehicleDetailsListener(error);
                    }
                };
                fetchVehicleStopsInfo(markerDataStandardized.getUmA(), umListener);
                fetchVehicleStopsInfo(markerDataStandardized.getUmB(), umListener);
                return;
            }

            String encodedId = URLEncoder.encode(markerDataStandardized.getId(), "UTF-8");
            getService(BASE_URL_BUS_TRACKER).getVehicleDetails(encodedId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Response<BusTrackerVehicleDetails> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BusTrackerVehicleDetails vehicleData = response.body();
                        markerDataStandardized.setVehicleDetails(vehicleData);
                        Log.d(TAG, String.valueOf(markerDataStandardized));
                        listener.onResponseVehicleDetailsListener(markerDataStandardized);
                    } else {
                        listener.onErrorVehicleDetailsListener(String.valueOf(response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Throwable t) {
                    listener.onErrorVehicleDetailsListener(t.getMessage());
                }
            });

        } catch (Exception e) {
            listener.onErrorVehicleDetailsListener(e.getMessage());
        }
    }

    // ==================== FETCH NETWORK DATA ====================
    public void fetchNetworkData(int networkId, OnNetworkDataListener listener) {
        getService(BASE_URL_BUS_TRACKER).getNetworkData(networkId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BusTrackerNetworkData> call, @NonNull Response<BusTrackerNetworkData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onResponseNetworkDataListener(response.body());
                } else {
                    listener.onErrorNetworkDataListener("Erreur réponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<BusTrackerNetworkData> call, @NonNull Throwable t) {
                listener.onErrorNetworkDataListener(t.getMessage());
            }
        });
    }

    // ==================== FETCH ROUTE LINE ====================
    public void fetchBusLine(MarkerDataStandardized markerDataStandardized, OnRouteLineListener listener) {
        try {
            Log.d(TAG, markerDataStandardized.getPathRef());
            String encodedPathRef = URLEncoder.encode(markerDataStandardized.getPathRef(), "UTF-8");
            Log.d(TAG, encodedPathRef);
            getService(BASE_URL_BUS_TRACKER).getPath(encodedPathRef).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehiclePath> call, @NonNull Response<BusTrackerVehiclePath> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        markerDataStandardized.setMarkerDataRoute(response.body());
                        listener.onResponseRouteLineListener(markerDataStandardized);
                    } else {
                        listener.onErrorRouteLineListener("Erreur: " + response);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BusTrackerVehiclePath> call, @NonNull Throwable t) {
                    listener.onErrorRouteLineListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorRouteLineListener(e.getMessage());
        }
    }

    // ==================== FETCH NETWORKS ====================
    public void fetchNetworks(OnNetworkListener listener) {
        try {
            getService(BASE_URL_BUS_TRACKER).getNetworks().enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<BusTrackerNetworkData>> call, @NonNull Response<List<BusTrackerNetworkData>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseNetworkListener(response.body());
                    } else {
                        listener.onErrorNetworkListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<BusTrackerNetworkData>> call, @NonNull Throwable t) {
                    listener.onErrorNetworkListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorNetworkListener(e.getMessage());
        }
    }

    // ==================== FETCH REGIONS ====================
    public void fetchRegions(OnRegionsListener listener) {
        try {
            getService(BASE_URL_BUS_TRACKER).getRegions().enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<BusTrackerRegionData>> call, @NonNull Response<List<BusTrackerRegionData>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseRegionsListener(response.body());
                    } else {
                        listener.onErrorRegionsListener("Erreur régions: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<BusTrackerRegionData>> call, @NonNull Throwable t) {
                    listener.onErrorRegionsListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorRegionsListener(e.getMessage());
        }
    }

    // ==================== FETCH VERSION ====================
    public void fetchLatestVersion(OnVersionListener listener) {
        try {
            getService(BASE_URL_DL_YNRYO).getLatestVersion().enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<YnryoVersionResponse> call, @NonNull Response<YnryoVersionResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseVersionListener(response.body());
                    } else {
                        listener.onErrorVersionListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<YnryoVersionResponse> call, @NonNull Throwable t) {
                    listener.onErrorVersionListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorVersionListener(e.getMessage());
        }
    }

    // ==================== FETCH IS ALIVE VERSION ====================
    public void fetchVehicleAlive(String vehicleId, OnVehicleAliveListener listener) {
        try {
            getService(BASE_URL_BUS_TRACKER).getVehicleDetails(vehicleId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Response<BusTrackerVehicleDetails> response) {
                    listener.onResponseVehicleAliveListener(response.code() == 200);
                }

                @Override
                public void onFailure(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Throwable t) {
                    listener.onErrorVehicleAliveListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorVehicleAliveListener(e.getMessage());
        }
    }

    // ==================== CONVERSION ====================
    private List<MarkerDataStandardized> convertMarkerDataList(List<BusTrackerMarkerData> busTrackerMarkerDataList) {
        List<MarkerDataStandardized> result = new ArrayList<>();

        if (busTrackerMarkerDataList == null || busTrackerMarkerDataList.isEmpty()) return result;

        for (BusTrackerMarkerData busTrackerMarkerData : busTrackerMarkerDataList) {
            try {
                MarkerType type = MarkerType.guestFromMarkerId(busTrackerMarkerData.getId()); //determiner type
                MarkerDataStandardized standardized = MarkerDataStandardized.createNewMarkerFrom(busTrackerMarkerData, type); //on convert

                result.add(standardized);
            } catch (Exception e) {
                Log.e("FetchingManager", "Erreur conversion BusTrackerMarkerData -> MarkerDataStandardized: " + e.getMessage());
            }
        }

        return result;
    }

    // ==================== FETCH GUEST PLATFORM ====================
    public void fetchGuestPlatform(String uicCode, String trainNum, OnGuessPlatformListener listener) {
        try {
            getService(BASE_URL_API_TCHOO).getGuestPlatform(uicCode, trainNum).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<CartoTchooGuessPlatform>> call, @NonNull Response<List<CartoTchooGuessPlatform>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseGuessPlatformListener(response.body());
                    } else {
                        listener.onErrorGuessPlatformListener("Erreur réponse: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<CartoTchooGuessPlatform>> call, @NonNull Throwable t) {
                    listener.onErrorGuessPlatformListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorGuessPlatformListener(e.getMessage());
        }
    }
}