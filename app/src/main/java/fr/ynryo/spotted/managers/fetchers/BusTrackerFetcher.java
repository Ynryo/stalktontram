package fr.ynryo.spotted.managers.fetchers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.spotted.apiResponsesPOJO.bus.BusTrackerVehiclePath;
import fr.ynryo.spotted.apiResponsesPOJO.markers.BusTrackerMarkerData;
import fr.ynryo.spotted.apiResponsesPOJO.markers.BusTrackerMarkersList;
import fr.ynryo.spotted.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.spotted.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.spotted.apiResponsesPOJO.vehicle.BusTrackerVehicleDetails;
import fr.ynryo.spotted.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.spotted.genericMarkerDatas.MarkerType;
import fr.ynryo.spotted.managers.FetchingManager;
import fr.ynryo.spotted.managers.um.TrainUmAssembler;
import fr.ynryo.spotted.services.ApiClientFactory;
import fr.ynryo.spotted.services.BusTrackerApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetcher dédié aux appels API du fournisseur bus-tracker.fr
 */
public class BusTrackerFetcher {
    private static final String TAG = "BusTrackerFetcher";
    private static final String BASE_URL = "https://bus-tracker.fr/api/";

    private final BusTrackerApiService apiService;

    public BusTrackerFetcher() {
        this.apiService = ApiClientFactory.createService(BASE_URL, BusTrackerApiService.class);
    }

    /**
     * Récupère la liste des véhicules visibles dans une zone géographique délimitée (bounds)
     * et les convertit en objets standardisés {@link MarkerStandardized}.
     *
     * @param bounds   Les limites géographiques de la vue courante de la carte (sud-ouest et nord-est)
     * @param lineId   L'identifiant optionnel d'une ligne pour filtrer les résultats (ou null pour tous)
     * @param listener Callback notifié avec la liste des marqueurs standardisés ou l'erreur survenue
     */
    public void fetchMarkers(LatLngBounds bounds, String lineId, FetchingManager.OnMarkersListener listener) {
        if (bounds == null) {
            Log.w(TAG, "fetchMarkers: bounds sont null");
            if (listener == null) return;
            listener.onErrorMarkersListener("Bounds are null");
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
                if (listener == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<MarkerStandardized> standardizedMarkers = convertMarkerDataList(response.body().getItems());
                    listener.onResponseMarkersListener(standardizedMarkers);
                } else {
                    Log.e(TAG, "fetchMarkers: code erreur HTTP " + response.code());
                    listener.onErrorMarkersListener("Erreur réponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<BusTrackerMarkersList> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchMarkers échec: " + t.getMessage(), t);
                if (listener == null) return;
                listener.onErrorMarkersListener(t.getMessage());
            }
        });
    }

    /**
     * Convertit une liste de POJO {@link BusTrackerMarkerData} en une liste d'objets métier {@link MarkerStandardized}.
     *
     * @param busTrackerMarkerDataList Liste brute des marqueurs issue de la réponse API
     * @return Liste d'objets {@link MarkerStandardized} typés
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

    /**
     * Récupère les détails d'un véhicule (arrêts, horaires, retards, etc.).
     * Si le véhicule est une unité multiple (UM), récupère récursivement les détails de chaque rame
     * et les assemble via {@link TrainUmAssembler}.
     *
     * @param markerStandardized Le marqueur du véhicule dont on souhaite charger les détails
     * @param listener           Callback notifié avec le marqueur enrichi ou l'erreur survenue
     */
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
                            if (listener == null) return;
                            listener.onResponseVehicleDetailsListener(markerStandardized);
                        }
                    }

                    @Override
                    public void onErrorVehicleDetailsListener(String error) {
                        Log.e(TAG, "fetchVehicleStopsInfo UM erreur: " + error);
                        if (listener == null) return;
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
                    if (listener == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        BusTrackerVehicleDetails vehicleData = response.body();
                        markerStandardized.setVehicleDetails(vehicleData);
                        listener.onResponseVehicleDetailsListener(markerStandardized);
                    } else {
                        Log.e(TAG, "fetchVehicleStopsInfo code erreur: " + response.code());
                        listener.onErrorVehicleDetailsListener(String.valueOf(response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Throwable t) {
                    Log.e(TAG, "fetchVehicleStopsInfo échec: " + t.getMessage(), t);
                    if (listener == null) return;
                    listener.onErrorVehicleDetailsListener(t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "fetchVehicleStopsInfo exception: " + e.getMessage(), e);
            if (listener == null) return;
            listener.onErrorVehicleDetailsListener(e.getMessage());
        }
    }

    /**
     * Récupère les données complètes et détaillées d'un réseau de transport.
     *
     * @param networkId Identifiant unique du réseau
     * @param listener  Callback notifié avec les données du réseau {@link BusTrackerNetworkData}
     */
    public void fetchNetworkData(int networkId, FetchingManager.OnNetworkDataListener listener) {
        apiService.getNetworkData(networkId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BusTrackerNetworkData> call, @NonNull Response<BusTrackerNetworkData> response) {
                if (listener == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    listener.onResponseNetworkDataListener(response.body());
                } else {
                    Log.e(TAG, "fetchNetworkData code erreur: " + response.code());
                    listener.onErrorNetworkDataListener("Erreur réponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<BusTrackerNetworkData> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchNetworkData échec: " + t.getMessage(), t);
                if (listener == null) return;
                listener.onErrorNetworkDataListener(t.getMessage());
            }
        });
    }

    /**
     * Récupère le tracé géographique (path / polyline) correspondant à un trajet de véhicule.
     *
     * @param markerStandardized Marqueur contenant la référence de chemin (pathRef)
     * @param listener           Callback notifié avec le marqueur enrichi de son tracé
     */
    public void fetchBusLine(MarkerStandardized markerStandardized, FetchingManager.OnRouteLineListener listener) {
        try {
            String encodedPathRef = URLEncoder.encode(markerStandardized.getPathRef(), "UTF-8");
            apiService.getPath(encodedPathRef).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehiclePath> call, @NonNull Response<BusTrackerVehiclePath> response) {
                    if (listener == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        markerStandardized.setMarkerDataRoute(response.body());
                        listener.onResponseRouteLineListener(markerStandardized);
                    } else {
                        Log.e(TAG, "fetchBusLine code erreur: " + response.code());
                        listener.onErrorRouteLineListener("Erreur: " + response);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BusTrackerVehiclePath> call, @NonNull Throwable t) {
                    Log.e(TAG, "fetchBusLine échec: " + t.getMessage(), t);
                    if (listener == null) return;
                    listener.onErrorRouteLineListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchBusLine exception: " + e.getMessage(), e);
            if (listener == null) return;
            listener.onErrorRouteLineListener(e.getMessage());
        }
    }

    /**
     * Récupère la liste de l'ensemble des réseaux de transport disponibles.
     *
     * @param listener Callback notifié avec la liste des {@link BusTrackerNetworkData}
     */
    public void fetchNetworks(FetchingManager.OnNetworkListener listener) {
        try {
            apiService.getNetworks().enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<BusTrackerNetworkData>> call, @NonNull Response<List<BusTrackerNetworkData>> response) {
                    if (listener == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseNetworkListener(response.body());
                    } else {
                        Log.e(TAG, "fetchNetworks code erreur: " + response.code());
                        listener.onErrorNetworkListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<BusTrackerNetworkData>> call, @NonNull Throwable t) {
                    Log.e(TAG, "fetchNetworks échec: " + t.getMessage(), t);
                    if (listener == null) return;
                    listener.onErrorNetworkListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchNetworks exception: " + e.getMessage(), e);
            if (listener == null) return;
            listener.onErrorNetworkListener(e.getMessage());
        }
    }

    /**
     * Récupère la liste des régions disponibles sur Bus-Tracker.
     *
     * @param listener Callback notifié avec la liste des {@link BusTrackerRegionData}
     */
    public void fetchRegions(FetchingManager.OnRegionsListener listener) {
        try {
            apiService.getRegions().enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<BusTrackerRegionData>> call, @NonNull Response<List<BusTrackerRegionData>> response) {
                    if (listener == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseRegionsListener(response.body());
                    } else {
                        Log.e(TAG, "fetchRegions code erreur: " + response.code());
                        listener.onErrorRegionsListener("Erreur régions: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<BusTrackerRegionData>> call, @NonNull Throwable t) {
                    Log.e(TAG, "fetchRegions échec: " + t.getMessage(), t);
                    if (listener == null) return;
                    listener.onErrorRegionsListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchRegions exception: " + e.getMessage(), e);
            if (listener == null) return;
            listener.onErrorRegionsListener(e.getMessage());
        }
    }

    /**
     * Vérifie si un véhicule est toujours actif / en circulation sur l'API (réponse HTTP 200).
     *
     * @param vehicleId L'identifiant du véhicule à vérifier
     * @param listener  Callback notifié avec un booléen (true si le véhicule est toujours actif)
     */
    public void fetchVehicleAlive(String vehicleId, FetchingManager.OnVehicleAliveListener listener) {
        try {
            apiService.getVehicleDetails(vehicleId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Response<BusTrackerVehicleDetails> response) {
                    if (listener == null) return;
                    listener.onResponseVehicleAliveListener(response.code() == 200);
                }

                @Override
                public void onFailure(@NonNull Call<BusTrackerVehicleDetails> call, @NonNull Throwable t) {
                    Log.e(TAG, "fetchVehicleAlive échec: " + t.getMessage(), t);
                    if (listener == null) return;
                    listener.onErrorVehicleAliveListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchVehicleAlive exception: " + e.getMessage(), e);
            if (listener == null) return;
            listener.onErrorVehicleAliveListener(e.getMessage());
        }
    }
}
