package fr.ynryo.stalktonbus.managers.fetchers;

import androidx.annotation.NonNull;

import java.util.List;

import fr.ynryo.stalktonbus.apiResponsesPOJO.guessPlatform.CartoTchooGuessPlatform;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.services.TchooApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fetcher dédié aux appels API du fournisseur CartoTchoo (api.tchoo.net)
 */
public class TchooFetcher {
    private static final String TAG = "TchooFetcher";
    private static final String BASE_URL = "https://api.tchoo.net/api/";

    private final TchooApiService apiService;

    public TchooFetcher() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.apiService = retrofit.create(TchooApiService.class);
    }

    /**
     * Récupère la prédiction / estimation de la voie de quai d'un train pour une gare donnée.
     *
     * @param uicCode  Code UIC de la gare concernée (ex: 87391003)
     * @param trainNum Numéro de circulation du train (ex: 864120)
     * @param listener Callback notifié avec la liste des prédictions {@link CartoTchooGuessPlatform} ou l'erreur
     */
    public void fetchGuestPlatform(String uicCode, String trainNum, FetchingManager.OnGuessPlatformListener listener) {
        try {
            apiService.getGuestPlatform(uicCode, trainNum).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<CartoTchooGuessPlatform>> call, @NonNull Response<List<CartoTchooGuessPlatform>> response) {
                    if (listener == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseGuessPlatformListener(response.body());
                    } else {
                        listener.onErrorGuessPlatformListener("Erreur réponse: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<CartoTchooGuessPlatform>> call, @NonNull Throwable t) {
                    if (listener == null) return;
                    listener.onErrorGuessPlatformListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            if (listener == null) return;
            listener.onErrorGuessPlatformListener(e.getMessage());
        }
    }
}
