package fr.ynryo.stalktonbus.managers.fetchers;

import androidx.annotation.NonNull;

import fr.ynryo.stalktonbus.apiResponsesPOJO.version.YnryoVersionResponse;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.services.YnryoApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fetcher dédié aux appels API de bus-tracker.fr
 */
public class YnryoFetcher {
    private static final String TAG = "YnryoFetcher";
    private static final String BASE_URL = "https://dl.ynryo.fr/api/stalktonbus/";

    private final YnryoApiService apiService;

    public YnryoFetcher() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.apiService = retrofit.create(YnryoApiService.class);
    }

    public void fetchLatestVersion(FetchingManager.OnVersionListener listener) {
        try {
            apiService.getLatestVersion().enqueue(new Callback<>() {
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
}
