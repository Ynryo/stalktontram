package fr.ynryo.stalktonbus.managers.fetchers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.stalktonbus.apiResponsesPOJO.bus.BusTrackerVehiclePath;
import fr.ynryo.stalktonbus.apiResponsesPOJO.markers.BusTrackerMarkerData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.markers.BusTrackerMarkersList;
import fr.ynryo.stalktonbus.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.vehicle.BusTrackerVehicleDetails;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerType;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.managers.um.TrainUmAssembler;
import fr.ynryo.stalktonbus.services.BusTrackerApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fetcher dédié aux appels API de bus-tracker.fr
 */
public class BusTrackerFetcher {
    private static final String TAG = "BusTrackerFetcher";
    private static final String BASE_URL = "https://bus-tracker.fr/api/";

    private final BusTrackerApiService apiService;

    public BusTrackerFetcher() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.apiService = retrofit.create(BusTrackerApiService.class);
    }

    /**
     * Récupère les marqueurs de véhicules dans la zone géographique visible
     */
    public void fetchMarkers(LatLngBounds bounds, String lineId, FetchingManager.OnMarkersListener listener) {
        if (bounds == null) {
            if (listener != null) {
                listener.onErrorMarkersListener("Bounds are null");
            }
            return;
        }

        apiService.getVehicleMarkers(
                bounds.southwest.latitude,
                bounds.southwest.longitude,
                bounds.northeast.latitude,
                bounds.northeast.longitude,
                lineId
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BusTrackerMarkersList> call, @NonNull Response<BusTrackerMarkersList> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MarkerStandardized> standardizedMarkers = convertMarkerDataList(response.body().getItems());
                    if (listener != null) {
                        listener.onResponseMarkersListener(standardizedMarkers);
                    }
                } else {
                    if (listener != null) {
                        listener.onErrorMarkersListener("Erreur réponse: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BusTrackerMarkersList> call, @NonNull Throwable t) {
                if (listener != null) {
                    listener.onErrorMarkersListener(t.getMessage());
                }
            }
        });
    }

    /**
     * Convertit les POJO BusTrackerMarkerData en objets métier MarkerStandardized
     */
    private List<MarkerStandardized> convertMarkerDataList(List<BusTrackerMarkerData> busTrackerMarkerDataList) {
        List<MarkerStandardized> result = new ArrayList<>();
        if (busTrackerMarkerDataList == null || busTrackerMarkerDataList.isEmpty()) {
            return result;
        }

        for (BusTrackerMarkerData busTrackerMarkerData : busTrackerMarkerDataList) {
            try {
                MarkerType type = MarkerType.guessFromMarkerId(busTrackerMarkerData.getId());
                MarkerStandardized standardized = MarkerStandardized.createNewMarkerFrom(busTrackerMarkerData, type);
                result.add(standardized);
            } catch (Exception e) {
                Log.e(TAG, "Erreur conversion BusTrackerMarkerData -> MarkerStandardized: " + e.getMessage());
            }
        }

        return result;
    }

    public void fetchVehicleStopsInfo(MarkerStandardized markerStandardized, FetchingManager.OnVehicleDetailsListener listener) {
        try {
            if (markerStandardized.isUm()) {
                FetchingManager.OnVehicleDetailsListener umListener = new FetchingManager.OnVehicleDetailsListener() {
                    private int responsesReceived = 0;

                    @Override
                    public void onResponseVehicleDetailsListener(MarkerStandardized data) {
                        responsesReceived++;
                        if (responsesReceived == 2) {
                            TrainUmAssembler.assembleUmStops(markerStandardized);
                            listener.onResponseVehicleDetailsListener(markerStandardized);
                        }
                    }

                    @Override
                    public void onErrorVehicleDetailsListener(String error) {
                        listener.onErrorVehicleDetailsListener(error);
                    }
                };
                fetchVehicleStopsInfo(markerStandardized.getUmA(), umListener);
                fetchVehicleStopsInfo(markerStandardized.getUmB(), umListener);
                return;
            }

            String encodedId = URLEncoder.encode(markerStandardized.getId(), "UTF-8");
            apiService.getVehicleDetails(encodedId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Response<BusTrackerVehicleDetails> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BusTrackerVehicleDetails vehicleData = response.body();
                        markerStandardized.setVehicleDetails(vehicleData);
                        listener.onResponseVehicleDetailsListener(markerStandardized);
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

    public void fetchNetworkData(int networkId, FetchingManager.OnNetworkDataListener listener) {
        apiService.getNetworkData(networkId).enqueue(new Callback<>() {
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

    public void fetchBusLine(MarkerStandardized markerStandardized, FetchingManager.OnRouteLineListener listener) {
        try {
            String encodedPathRef = URLEncoder.encode(markerStandardized.getPathRef(), "UTF-8");
            apiService.getPath(encodedPathRef).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehiclePath> call, @NonNull Response<BusTrackerVehiclePath> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        markerStandardized.setMarkerDataRoute(response.body());
                        listener.onResponseRouteLineListener(markerStandardized);
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

    public void fetchNetworks(FetchingManager.OnNetworkListener listener) {
        try {
            apiService.getNetworks().enqueue(new Callback<>() {
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

    public void fetchRegions(FetchingManager.OnRegionsListener listener) {
        try {
            apiService.getRegions().enqueue(new Callback<>() {
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

    public void fetchVehicleAlive(String vehicleId, FetchingManager.OnVehicleAliveListener listener) {
        try {
            apiService.getVehicleDetails(vehicleId).enqueue(new Callback<>() {
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
}
