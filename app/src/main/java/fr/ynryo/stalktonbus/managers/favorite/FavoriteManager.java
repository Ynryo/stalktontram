package fr.ynryo.stalktonbus.managers.favorite;

import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.stalktonbus.MainActivity;
import fr.ynryo.stalktonbus.R;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.stalktonbus.managers.SaveManager;

/**
 * Classe gérant les lignes favorites
 */
public class FavoriteManager {
    private final static String TAG = "FavoriteManager";
    private final List<Favorite> favoriteLines = new ArrayList<>();
    private final MainActivity context;
    private final SaveManager saveManager;
    private MarkerDataStandardized showedVehicleId;
    private FloatingActionButton favoriteButton;

    public FavoriteManager(MainActivity context, SaveManager saveManager) {
        this.context = context;
        this.saveManager = saveManager;
        this.favoriteLines.clear();
        this.favoriteLines.addAll(saveManager != null ? saveManager.loadFavoriteLines() : new ArrayList<>());
    }

    public void toggleFavorite(MarkerDataStandardized mData) {
        if (mData == null) return;
        if (isFavorite(mData)) {
            removeFavorite(mData);
        } else {
            addFavorite(mData);
        }
    }

    public void addFavorite(MarkerDataStandardized mData) {
        if (isFavorite(mData)) return;
        this.showedVehicleId = mData;
        favoriteLines.add(new Favorite(mData.getLineId(), mData.getLineNumber(), mData.getDestination(), mData.getFillColor(), mData.getTextColor(), mData.getNetworkRef()));
        favoriteButton.setImageResource(R.drawable.icon_favorite_fill);
        saveManager.saveFavoriteLines(favoriteLines);
    }

    public void removeFavorite(MarkerDataStandardized mData) {
        if (!isFavorite(mData)) return;
        this.showedVehicleId = null;
        favoriteLines.removeIf(f -> f.getLigneId() == mData.getLineId() && f.getDestination().equals(mData.getDestination()));
        favoriteButton.setImageResource(R.drawable.icon_favorite);
        saveManager.saveFavoriteLines(favoriteLines);
    }

    public boolean isFavorite(MarkerDataStandardized mData) {
        if (mData == null) return false;
        for (Favorite f : favoriteLines) {
            if (f.getLigneId() == mData.getLineId() && f.getDestination().equals(mData.getDestination())) {
                return true;
            }
        }
        return false;
    }

    public List<Favorite> getFavoriteLines() {
        return favoriteLines;
    }

    public void setFavoriteButton(FloatingActionButton favoriteButton, MarkerDataStandardized mData) {
        this.favoriteButton = favoriteButton;
        if (this.favoriteButton == null) return;
        if (mData.isTrain()) this.favoriteButton.setVisibility(View.GONE); //temp pour éviter les bugs
        if (isFavorite(mData)) this.favoriteButton.setImageResource(R.drawable.icon_favorite_fill);
        this.favoriteButton.setOnClickListener(view -> toggleFavorite(mData));
    }
}
