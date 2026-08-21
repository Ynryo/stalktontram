package fr.ynryo.spotted.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import fr.ynryo.spotted.managers.favorite.Favorite;

/**
 * Classe gérant le stockage des préférences de l'utilisateur
 */
public class SaveManager {
    private final static String TAG = "SaveManager";
    private static final String PREFS_NAME = "ouestcefdpdetramPrefs";
    private static final String KEY_PREFIX_NETWORK = "network_";
    private static final String KEY_FAVORITE = "favorite";
    private final SharedPreferences prefs;
    private final Gson gson;

    public SaveManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveNetworkFilter(String networkRef, boolean isVisible) {
        String key = KEY_PREFIX_NETWORK + networkRef;
        prefs.edit().putBoolean(key, isVisible).apply();
    }

    public boolean loadNetworkFilter(String networkRef) {
        String key = KEY_PREFIX_NETWORK + networkRef;
        return prefs.getBoolean(key, true);
    }

    public void saveAllNetworksVisibility(List<String> networkRefs, boolean isVisible) {
        SharedPreferences.Editor editor = prefs.edit();
        for (String networkRef : networkRefs) {
            String key = KEY_PREFIX_NETWORK + networkRef;
            editor.putBoolean(key, isVisible);
        }
        editor.apply();
    }

    public boolean isAllNetworksVisible() {
        Map<String, ?> entries = prefs.getAll();
        if (entries.isEmpty()) return true;
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            if (entry.getKey().startsWith(KEY_PREFIX_NETWORK) && entry.getValue().equals(false)) {
                return false;
            }
        }
        return true;
    }

    public void saveFavoriteLines(List<Favorite> favoriteLines) {
        favoriteLines.sort(Comparator
                .comparing(Favorite::getLineText, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Favorite::getDestination, Comparator.nullsLast(String::compareToIgnoreCase))
        );
        String json = gson.toJson(favoriteLines);
        prefs.edit().putString(KEY_FAVORITE, json).apply();
    }

    public List<Favorite> loadFavoriteLines() {
        String json = prefs.getString(KEY_FAVORITE, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();

        Type type = new TypeToken<List<Favorite>>() {}.getType();
        List<Favorite> list = null;
        try {
            list = gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            prefs.edit().remove(KEY_FAVORITE).apply();
            e.printStackTrace();
        }

        return list != null ? list : new ArrayList<>();
    }
}