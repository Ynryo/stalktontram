package fr.ynryo.stalktonbus.managers;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

import fr.ynryo.stalktonbus.MainActivity;
import fr.ynryo.stalktonbus.apiResponsesPOJO.guessPlatform.CartoTchooGuessPlatform;
import fr.ynryo.stalktonbus.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.stalktonbus.apiResponsesPOJO.version.YnryoVersionResponse;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.stalktonbus.managers.fetchers.BusTrackerFetcher;
import fr.ynryo.stalktonbus.managers.fetchers.TchooFetcher;
import fr.ynryo.stalktonbus.managers.fetchers.YnryoFetcher;

/**
 * Classe gérant les requêtes et réponses de l'API et les conversions avec MarkerStandardized
 * <p>
 * Author: Ynryo
 */
public class FetchingManager {
    private static final String TAG = "FetchingManager";
    private final MainActivity context;
    private final BusTrackerFetcher busTrackerFetcher;
    private final TchooFetcher tchooFetcher;
    private final YnryoFetcher ynryoFetcher;

    public FetchingManager(MainActivity context) {
        this.context = context;
        this.busTrackerFetcher = new BusTrackerFetcher();
        this.tchooFetcher = new TchooFetcher();
        this.ynryoFetcher = new YnryoFetcher();
    }

    // ==================== LISTENERS ====================
    public interface OnMarkersListener {
        void onResponseMarkersListener(List<MarkerStandardized> markerStandardizedList);

        void onErrorMarkersListener(String error);
    }

    public interface OnVehicleDetailsListener {
        void onResponseVehicleDetailsListener(MarkerStandardized markerStandardized);

        void onErrorVehicleDetailsListener(String error);
    }

    public interface OnNetworkDataListener {
        void onResponseNetworkDataListener(BusTrackerNetworkData data);

        void onErrorNetworkDataListener(String error);
    }

    public interface OnRouteLineListener {
        void onResponseRouteLineListener(MarkerStandardized data);

        void onErrorRouteLineListener(String error);
    }

    public interface OnNetworkListener {
        void onResponseNetworkListener(List<BusTrackerNetworkData> data);

        void onErrorNetworkListener(String error);
    }

    public interface OnRegionsListener {
        void onResponseRegionsListener(List<BusTrackerRegionData> regions);

        void onErrorRegionsListener(String error);
    }

    public interface OnVersionListener {
        void onResponseVersionListener(YnryoVersionResponse version);

        void onErrorVersionListener(String error);
    }

    public interface OnVehicleAliveListener {
        void onResponseVehicleAliveListener(boolean isAlive);

        void onErrorVehicleAliveListener(String error);
    }

    public interface OnGuessPlatformListener {
        void onResponseGuessPlatformListener(List<CartoTchooGuessPlatform> cartoTchooGuessPlatform);

        void onErrorGuessPlatformListener(String error);
    }

    // ==================== FETCH MARKERS (PRINCIPAL) ====================
    public void fetchMarkers(OnMarkersListener listener) {
        fetchMarkers(null, listener);
    }

    public void fetchMarkers(String lineId, OnMarkersListener listener) {
        if (context.getMap() == null) return;
        LatLngBounds bounds = context.getMap().getProjection().getVisibleRegion().latLngBounds;
        busTrackerFetcher.fetchMarkers(bounds, lineId, listener);
    }

    // ==================== FETCH VEHICLE DETAILS ====================
    public void fetchVehicleStopsInfo(MarkerStandardized markerStandardized, OnVehicleDetailsListener listener) {
        busTrackerFetcher.fetchVehicleStopsInfo(markerStandardized, listener);
    }

    // ==================== FETCH NETWORK DATA ====================
    public void fetchNetworkData(int networkId, OnNetworkDataListener listener) {
        busTrackerFetcher.fetchNetworkData(networkId, listener);
    }

    // ==================== FETCH ROUTE LINE ====================
    public void fetchBusLine(MarkerStandardized markerStandardized, OnRouteLineListener listener) {
        busTrackerFetcher.fetchBusLine(markerStandardized, listener);
    }

    // ==================== FETCH NETWORKS ====================
    public void fetchNetworks(OnNetworkListener listener) {
        busTrackerFetcher.fetchNetworks(listener);
    }

    // ==================== FETCH REGIONS ====================
    public void fetchRegions(OnRegionsListener listener) {
        busTrackerFetcher.fetchRegions(listener);
    }

    // ==================== FETCH VERSION ====================
    public void fetchLatestVersion(OnVersionListener listener) {
        ynryoFetcher.fetchLatestVersion(listener);
    }

    // ==================== FETCH IS ALIVE VERSION ====================
    public void fetchVehicleAlive(String vehicleId, OnVehicleAliveListener listener) {
        busTrackerFetcher.fetchVehicleAlive(vehicleId, listener);
    }

    // ==================== FETCH GUEST PLATFORM ====================
    public void fetchGuestPlatform(String uicCode, String trainNum, OnGuessPlatformListener listener) {
        tchooFetcher.fetchGuestPlatform(uicCode, trainNum, listener);
    }
}