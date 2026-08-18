package fr.ynryo.stalktonbus.services;

import java.util.List;

import fr.ynryo.stalktonbus.apiResponsesPOJO.guessPlatform.CartoTchooGuessPlatform;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface de l'API, gère les requêtes et les réponses
 */
public interface TchooApiService {
    @GET("guess_my_platform.php")
    Call<List<CartoTchooGuessPlatform>> getGuestPlatform(
            @Query(value = "uic", encoded = true) String uicCode,
            @Query(value = "num", encoded = true) String trainNum
    );
}
