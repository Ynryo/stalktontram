package fr.ynryo.stalktonbus.services;

import fr.ynryo.stalktonbus.apiResponsesPOJO.version.YnryoVersionResponse;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Interface de l'API, gère les requêtes et les réponses
 */
public interface YnryoApiService {
    @GET("version/latest")
    Call<YnryoVersionResponse> getLatestVersion();
}
