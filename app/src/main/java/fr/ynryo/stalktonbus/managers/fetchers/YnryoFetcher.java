package fr.ynryo.stalktonbus.managers.fetchers;

import android.util.Log;

import androidx.annotation.NonNull;

import fr.ynryo.stalktonbus.apiResponsesPOJO.version.YnryoVersionResponse;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.services.ApiClientFactory;
import fr.ynryo.stalktonbus.services.YnryoApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetcher dédié aux appels API de mise à jour et versioning (dl.ynryo.fr)
 */
public class YnryoFetcher {
    private static final String TAG = "YnryoFetcher";
    private static final String BASE_URL = "https://dl.ynryo.fr/api/stalktonbus/";

    private final YnryoApiService apiService;

    public YnryoFetcher() {
        this.apiService = ApiClientFactory.createService(BASE_URL, YnryoApiService.class);
    }

    /**
     * Récupère les métadonnées de la dernière version disponible de l'application (numéro de version, URL d'APK, changelog).
     *
     * @param listener Callback notifié avec les informations de version {@link YnryoVersionResponse} ou l'erreur
     */
    public void fetchLatestVersion(FetchingManager.OnVersionListener listener) {
        try {
            apiService.getLatestVersion().enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<YnryoVersionResponse> call, @NonNull Response<YnryoVersionResponse> response) {
                    if (listener == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseVersionListener(response.body());
                    } else {
                        Log.e(TAG, "fetchLatestVersion code erreur: " + response.code());
                        listener.onErrorVersionListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<YnryoVersionResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "fetchLatestVersion échec: " + t.getMessage(), t);
                    if (listener == null) return;
                    listener.onErrorVersionListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchLatestVersion exception: " + e.getMessage(), e);
            if (listener == null) return;
            listener.onErrorVersionListener(e.getMessage());
        }
    }
}
