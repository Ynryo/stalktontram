package fr.ynryo.spotted.services;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory centralisant la création et la configuration des instances Retrofit
 */
public class ApiClientFactory {
    private static final long TIMEOUT_SECONDS = 15;

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    /**
     * Crée une instance de service Retrofit configurée avec Gson et les timeouts standards
     *
     * @param baseUrl      L'URL racine de l'API
     * @param serviceClass L'interface du service Retrofit à instancier
     * @param <T>          Le type de l'interface
     * @return L'instance du service
     */
    public static <T> T createService(String baseUrl, Class<T> serviceClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OK_HTTP_CLIENT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(serviceClass);
    }
}
