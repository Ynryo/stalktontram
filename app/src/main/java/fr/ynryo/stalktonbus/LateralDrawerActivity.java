package fr.ynryo.stalktonbus;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ynryo.stalktonbus.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.managers.SaveManager;
import fr.ynryo.stalktonbus.managers.favorite.Favorite;

public class LateralDrawerActivity {
    private static final String TAG = "LateralDrawerActivity";
    private final MainActivity context;
    private final SaveManager saveManager;
    private final Map<String, Boolean> filters = new HashMap<>(); //ref réseau <> isShowed ?
    private final List<MaterialSwitch> switches = new ArrayList<>();
    private boolean isBulkUpdate = false;
    private boolean isUpdatingMasterSwitch = false;
    private boolean isNetworksFiltersFetch = false;

    private DrawerLayout drawerLayout;

    private View mainMenuContainer;
    private View filtersPageContainer;
    private View favoritesLinesContainer;
    private View favoritesStopsContainer;
    private View favoritesTrainsContainer;
    private View creditsPageContainer;

    /**
     * Constructeur de la classe LateralDrawerActivity
     * @param context context
     * @param saveManager saveManager
     */
    public LateralDrawerActivity(MainActivity context, SaveManager saveManager) {
        this.context = context;
        this.saveManager = saveManager;
        initMenu();
    }

    // ===== Set up menu =====
    private void initMenu() {
        drawerLayout = context.findViewById(R.id.drawer_layout);
        mainMenuContainer = context.findViewById(R.id.main_menu_container);
        filtersPageContainer = context.findViewById(R.id.filters_page_container);
        favoritesLinesContainer = context.findViewById(R.id.favorite_lines_container);
        favoritesStopsContainer = context.findViewById(R.id.favorite_stops_container);
        favoritesTrainsContainer = context.findViewById(R.id.favorite_trains_container);
        creditsPageContainer = context.findViewById(R.id.credits_page_container);

        View btnFilters = context.findViewById(R.id.btn_menu_filters);
        View btnFavoritesLines = context.findViewById(R.id.btn_menu_favorites_lines);
        View btnFavoritesStops = context.findViewById(R.id.btn_menu_favorites_stops);
        View btnFavoritesTrains = context.findViewById(R.id.btn_menu_favorites_trains);
        View btnCredits = context.findViewById(R.id.btn_menu_credits);

        View btnBackFilters = context.findViewById(R.id.btn_back_to_menu_filters);
        View btnBackFavoritesLines = context.findViewById(R.id.btn_back_to_menu_favorites_lines);
        View btnBackFavoritesStops = context.findViewById(R.id.btn_back_to_menu_favorites_stops);
        View btnBackFavoritesTrains = context.findViewById(R.id.btn_back_to_menu_favorites_trains);
        View btnBackCredits = context.findViewById(R.id.btn_back_to_menu_credits);

        if (btnFilters != null) btnFilters.setOnClickListener(v -> showFiltersPage());
        if (btnFavoritesLines != null) btnFavoritesLines.setOnClickListener(v -> showFavoriteStopsPage());
        if (btnFavoritesStops != null) btnFavoritesStops.setOnClickListener(v -> showFavoriteStopsPage());
        if (btnFavoritesTrains != null) btnFavoritesTrains.setOnClickListener(v -> showFavoriteStopsPage());
        if (btnCredits != null) btnCredits.setOnClickListener(v -> showCreditsPage());
        
        if (btnBackFilters != null) btnBackFilters.setOnClickListener(v -> showMainMenu());
        if (btnBackFavoritesLines != null) btnBackFavoritesLines.setOnClickListener(v -> showMainMenu());
        if (btnBackFavoritesStops != null) btnBackFavoritesStops.setOnClickListener(v -> showMainMenu());
        if (btnBackFavoritesTrains != null) btnBackFavoritesTrains.setOnClickListener(v -> showMainMenu());
        if (btnBackCredits != null) btnBackCredits.setOnClickListener(v -> showMainMenu());
    }

    private void showMainMenu() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.VISIBLE);
        if (filtersPageContainer != null) filtersPageContainer.setVisibility(View.GONE);
        if (favoritesLinesContainer != null) favoritesLinesContainer.setVisibility(View.GONE);
        if (favoritesStopsContainer != null) favoritesStopsContainer.setVisibility(View.GONE);
        if (favoritesTrainsContainer != null) favoritesTrainsContainer.setVisibility(View.GONE);
        if (creditsPageContainer != null) creditsPageContainer.setVisibility(View.GONE);
    }

    private void showFiltersPage() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.GONE);
        if (filtersPageContainer != null) filtersPageContainer.setVisibility(View.VISIBLE);

        if (!isNetworksFiltersFetch) fetchAndPopulateNetworks();
    }

    private void showFavoriteStopsPage() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.GONE);
        if (favoritesLinesContainer != null) favoritesLinesContainer.setVisibility(View.VISIBLE);
        populateFavoriteLines();
    }

    private void showCreditsPage() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.GONE);
        if (creditsPageContainer != null) creditsPageContainer.setVisibility(View.VISIBLE);
    }

    public void open() {
        drawerLayout = context.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            showMainMenu();
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /**
     * Récupère les données des réseaux depuis l'API et les popule dans la liste des réseaux
     */
    private void fetchAndPopulateNetworks() {
        LinearLayout networksContainer = context.findViewById(R.id.networks_container);
        if (networksContainer == null) return;

        networksContainer.removeAllViews();
        ProgressBar loader = new ProgressBar(context);
        loader.setIndeterminateTintList(
                android.content.res.ColorStateList.valueOf(
                        context.getResources().getColor(R.color.blue_primary, context.getTheme())
                )
        );
        networksContainer.addView(loader);

        context.getFetcher().fetchRegions(new FetchingManager.OnRegionsListener() {
            @Override
            public void onResponseRegionsListener(List<BusTrackerRegionData> regions) {
                context.getFetcher().fetchNetworks(new FetchingManager.OnNetworkListener() {
                    @Override
                    public void onResponseNetworkListener(List<BusTrackerNetworkData> networks) {
                        isNetworksFiltersFetch = true;
                        populateNetworks(regions, networks);
                    }

                    @Override
                    public void onErrorNetworkListener(String error) {
                        Log.e(TAG, "Erreur réseaux: " + error);
                        showNetworkError(networksContainer);
                    }
                });
            }

            @Override
            public void onErrorRegionsListener(String error) {
                Log.e(TAG, "Erreur régions: " + error);
                showNetworkError(networksContainer);
            }
        });
    }

    /**
     * Affiche un message d'erreur lorsque la récupération des données réseau échoue
     * @param networksContainer networksContainer
     */
    private void showNetworkError(LinearLayout networksContainer) {
        networksContainer.removeAllViews();
        TextView tvError = new TextView(context);
        tvError.setText(R.string.network_error);
        tvError.setPadding(32, 32, 32, 32);
        tvError.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        networksContainer.addView(tvError);
    }

    /**
     * Popule la liste des réseaux avec les données fournies
     * @param regions regions
     * @param networks networks
     */
    public void populateNetworks(List<BusTrackerRegionData> regions, List<BusTrackerNetworkData> networks) {
        if (saveManager == null) return;

        LinearLayout networksContainer = context.findViewById(R.id.networks_container);
        if (networksContainer == null) return;
        
        networksContainer.removeAllViews();
        filters.clear();
        switches.clear();

        View allShowRow = LayoutInflater.from(context).inflate(R.layout.network_item, networksContainer, false);
        TextView tvShowName = allShowRow.findViewById(R.id.network_name);
        MaterialSwitch msShowToggle = allShowRow.findViewById(R.id.network_switch);

        tvShowName.setText(R.string.show_all);
        msShowToggle.setChecked(saveManager.isAllNetworksVisible());
        msShowToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingMasterSwitch) return;
            isBulkUpdate = true;

            ArrayList<String> networksName = new ArrayList<>();
            for (MaterialSwitch ms : switches) {
                ms.setChecked(isChecked);
                String networkRef = ms.getTag().toString();
                filters.put(networkRef, isChecked);
                networksName.add(networkRef);
            }

            isBulkUpdate = false;
            saveManager.saveAllNetworksVisibility(networksName, isChecked);

            context.getFetcher().fetchMarkers(new FetchingManager.OnMarkersListener() {
                @Override
                public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                    context.getMarkerArtist().showMarkers(markerDataStandardizedList);
                }

                @Override
                public void onErrorMarkersListener(String error) {
                    Log.e(TAG, "Erreur lors de la récupération des données markers" + error);
                }
            });
        });
        networksContainer.addView(allShowRow);


        // Grouper les réseaux par région
        Map<Integer, List<BusTrackerNetworkData>> networksByRegion = new HashMap<>();
        Map<Integer, BusTrackerRegionData> regionMap = new HashMap<>();

        // Créer une map des régions par ID
        boolean hasNational = false;
        for (BusTrackerRegionData r : regions) {
            if (r.getId() == 0) {
                hasNational = true;
                break;
            }
        }
        if (!hasNational) {
            regions.add(new BusTrackerRegionData(0, "National"));
        }

        for (BusTrackerRegionData region : regions) {
            Log.d(TAG, "Region: " + region.getName() + " with ID: " + region.getId());
            regionMap.put(region.getId(), region);
        }

        // Grouper les réseaux par région
        for (BusTrackerNetworkData network : networks) {
            int regionId = network.getRegionId();
            if (!networksByRegion.containsKey(regionId)) {
                String regionName = regionMap.containsKey(regionId) ? regionMap.get(regionId).getName() : "Inconnue";
                Log.d(TAG, "Ajouté à la map: " + network.getName() + " pour la région: " + regionName + " avec l'ID: " + regionId);
                networksByRegion.put(regionId, new ArrayList<>());
            }
            networksByRegion.get(regionId).add(network);
        }

        // Ajouter chaque région et ses réseaux
        for (BusTrackerRegionData region : regions) {
            List<BusTrackerNetworkData> regionNetworks = networksByRegion.get(region.getId());
            if (regionNetworks == null || regionNetworks.isEmpty()) {
                continue; // Pas de réseaux dans cette région
            }

            // Header de région (pliable)
            View regionHeader = LayoutInflater.from(context).inflate(R.layout.region_header, networksContainer, false);
            TextView tvRegionTitle = regionHeader.findViewById(R.id.region_title);
            ImageView ivArrow = regionHeader.findViewById(R.id.region_arrow);

            tvRegionTitle.setText(region.getName());

            // Container pour les réseaux de cette région
            LinearLayout regionNetworksContainer = new LinearLayout(context);
            regionNetworksContainer.setOrientation(LinearLayout.VERTICAL);
            regionNetworksContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            final boolean[] isExpanded = {false};
            regionNetworksContainer.setVisibility(View.GONE);
            regionHeader.setOnClickListener(v -> {
                isExpanded[0] = !isExpanded[0];
                regionNetworksContainer.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
                ivArrow.setRotation(isExpanded[0] ? 0 : 180);
            });

            networksContainer.addView(regionHeader);
            networksContainer.addView(regionNetworksContainer);

            // Ajouter les réseaux de cette région
            for (BusTrackerNetworkData network : regionNetworks) {
                String networkRef = network.getRef();
                boolean isVisible = saveManager.loadNetworkFilter(networkRef);
                filters.put(networkRef, isVisible);

                View row = LayoutInflater.from(context).inflate(R.layout.network_item, regionNetworksContainer, false);
                TextView tvName = row.findViewById(R.id.network_name);
                TextView tvCity = row.findViewById(R.id.network_city);
                MaterialSwitch visibilitySwitch = row.findViewById(R.id.network_switch);

                tvName.setText(network.getName());
                tvCity.setText(network.getAuthority());
                tvCity.setSingleLine(true);
                tvCity.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                tvCity.setMarqueeRepeatLimit(-1);
                tvCity.setHorizontallyScrolling(true);
                tvCity.setSelected(true);

                visibilitySwitch.setChecked(isVisible);
                visibilitySwitch.setTag(networkRef);
                switches.add(visibilitySwitch);

                visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isBulkUpdate) return;

                    filters.put(networkRef, isChecked);
                    saveManager.saveNetworkFilter(networkRef, isChecked);

                    isUpdatingMasterSwitch = true;

                    if (!isChecked) {
                        msShowToggle.setChecked(false);
                    } else {
                        boolean areAllChecked = true;
                        for (MaterialSwitch s : switches) {
                            if (!s.isChecked()) {
                                areAllChecked = false;
                                break;
                            }
                        }
                        msShowToggle.setChecked(areAllChecked);
                    }
                    isUpdatingMasterSwitch = false;

                    context.getFetcher().fetchMarkers(new FetchingManager.OnMarkersListener() {
                        @Override
                        public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                            context.getMarkerArtist().showMarkers(markerDataStandardizedList);
                        }

                        @Override
                        public void onErrorMarkersListener(String error) {
                            Log.e("LateralDrawerActivity", "Erreur markers: " + error);
                        }
                    });
                });

                regionNetworksContainer.addView(row);
            }
        }
    }

    /**
     * Popule la liste des lignes favorites avec les données fournies
     */
    private void populateFavoriteLines() {
        LinearLayout favoritesContainer = context.findViewById(R.id.favorites_lines_container);
        if (favoritesContainer == null) return;

        favoritesContainer.removeAllViews();

        List<Favorite> favoriteLines = saveManager.loadFavoriteLines();

        // si y'a pas de favoris
        if (favoriteLines.isEmpty()) {
            TextView noFavoritesText = new TextView(context);
            noFavoritesText.setText(R.string.no_favorites_message);
            noFavoritesText.setTextSize(16);
            noFavoritesText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            noFavoritesText.setPadding(16, 16, 16, 16);
            favoritesContainer.addView(noFavoritesText);
            return;
        }

        for (Favorite f : favoriteLines) {
            String lineIdStr = String.valueOf(f.getLigneId());

            View lineHeaderView = LayoutInflater.from(context).inflate(R.layout.favorite_line_header, favoritesContainer, false);
            TextView tvLineNumber = lineHeaderView.findViewById(R.id.tv_line_number);
            TextView tvDestinationHeader = lineHeaderView.findViewById(R.id.tv_destination);
            TextView tvCityHeader = lineHeaderView.findViewById(R.id.tv_city);
            ImageView ivArrow = lineHeaderView.findViewById(R.id.iv_arrow);

            int fillColor = Color.parseColor(f.getFillColor() != null ? f.getFillColor() : "#424242");
            int textColor = Color.parseColor(f.getTextColor() != null ? f.getTextColor() : "#FFFFFF");

            tvLineNumber.setText(f.getLineText());
            tvLineNumber.setBackgroundColor(fillColor);
            tvLineNumber.setTextColor(textColor);
            tvDestinationHeader.setText(f.getDestination());
            tvDestinationHeader.setSingleLine(true);
            tvDestinationHeader.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            tvDestinationHeader.setMarqueeRepeatLimit(-1);
            tvDestinationHeader.setHorizontallyScrolling(true);
            tvDestinationHeader.setSelected(true);

            tvCityHeader.setVisibility(View.GONE);
//            String city = f.getCity() != null ? f.getCity() : context.getString(R.string.no_data);
//            tvCityHeader.setText(city);

            favoritesContainer.addView(lineHeaderView);

            LinearLayout lineVehiclesContainer = new LinearLayout(context);
            lineVehiclesContainer.setOrientation(LinearLayout.VERTICAL);
            lineVehiclesContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            final boolean[] isExpanded = {false};
            lineVehiclesContainer.setVisibility(View.GONE);
            lineHeaderView.setOnClickListener(v -> {
                isExpanded[0] = !isExpanded[0];
                lineVehiclesContainer.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
                ivArrow.setRotation(isExpanded[0] ? 0 : 180);
                ivArrow.setContentDescription(context.getString(isExpanded[0] ? R.string.collapse_desc : R.string.expand_desc));
            });

            favoritesContainer.addView(lineVehiclesContainer);

            // fetch markers for favorite line
            context.getFetcher().fetchMarkers(lineIdStr, new FetchingManager.OnMarkersListener() {
                @Override
                public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                    // filtrer uniquement les véhicules de la ligne
                    for (MarkerDataStandardized vehicle : markerDataStandardizedList) {
                        context.getFetcher().fetchVehicleStopsInfo(vehicle, new FetchingManager.OnVehicleDetailsListener() {
                            @Override
                            public void onResponseVehicleDetailsListener(MarkerDataStandardized markerDetails) {
                                if (f.getDestination().equals(markerDetails.getDestination())) {
                                    View vehicleView = LayoutInflater.from(context).inflate(R.layout.favorite_line_item, lineVehiclesContainer, false);
                                    ImageView ivMarker = vehicleView.findViewById(R.id.iv_marker);
                                    TextView tvNextStop = vehicleView.findViewById(R.id.tv_next_stop);
                                    TextView tvTime = vehicleView.findViewById(R.id.tv_time);

                                    ivMarker.setImageBitmap(context.getMarkerArtist().createMarker(markerDetails, 0, false));
                                    tvNextStop.setText(markerDetails.getNextStop() != null ? markerDetails.getNextStop().getStopName() : context.getString(R.string.no_data));
                                    tvTime.setText(markerDetails.getNextStop() != null && markerDetails.getNextStop().getDepartureTime() != null ? markerDetails.getNextStop().getDepartureTime().formatHHmm() : context.getString(R.string.no_data));
                                    vehicleView.setOnClickListener(v -> {
                                        if (drawerLayout != null) {
                                            drawerLayout.closeDrawer(GravityCompat.START);
                                        }
                                        context.getMarkerArtist().getMarkerStopsDetailActivity().open(markerDetails);
                                        context.centerOnMarker(markerDetails, false, true);
                                    });
                                    lineVehiclesContainer.addView(vehicleView);
                                }
                            }

                            @Override
                            public void onErrorVehicleDetailsListener(String error) {
                                Log.e(TAG, "Error fetching details for favorite " + lineIdStr + ": " + error);
                            }
                        });
                    }
                }

                @Override
                public void onErrorMarkersListener(String error) {
                    Log.e(TAG, "Error fetching markers for favorite " + lineIdStr + ": " + error);
                }
            });
        }
    }

    /**
     * Vérifie si un réseau est visible
     * @param networkRef réseau
     * @return true si le réseau est visible, false sinon
     */
    public boolean isNetworkVisible(String networkRef) {
        if (filters.isEmpty()) return true;
        Boolean networkVisibility = filters.get(networkRef);
        return networkVisibility != null && networkVisibility;
    }
}