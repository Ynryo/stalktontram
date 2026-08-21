package fr.ynryo.spotted;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;

import fr.ynryo.spotted.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.spotted.apiResponsesPOJO.region.BusTrackerRegionData;
import fr.ynryo.spotted.apiResponsesPOJO.version.YnryoVersionResponse;
import fr.ynryo.spotted.artists.MarkerArtist;
import fr.ynryo.spotted.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.spotted.managers.CompassManager;
import fr.ynryo.spotted.managers.FetchingManager;
import fr.ynryo.spotted.managers.FollowManager;
import fr.ynryo.spotted.managers.LayoutManager;
import fr.ynryo.spotted.managers.SaveManager;
import fr.ynryo.spotted.managers.favorite.FavoriteManager;

/**
 * Classe principale, gère la vue et les managers
 *
 * @author Ynryo
 * @version 1.2.4
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener {
    private static final String TAG = "MainActivity";
    private static final float DEFAULT_ZOOM = 13f;
    private static final LatLng PARIS = new LatLng(48.8566, 2.3522);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, 5000);
            fetchMarkers();
        }
    };

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    if (this.googleMap != null) {
                        try {
                            this.googleMap.setMyLocationEnabled(true);
                        } catch (SecurityException e) {
                            Log.e(TAG, "SecurityException: " + e.getMessage());
                        }
                    }
                    centerMapOnUserLocation();
                } else {
                    Log.d(TAG, "Permissions de localisation refusées");
                }
            });

    private LateralDrawerActivity lateralDrawerActivity;
    private FetchingManager fetcher;
    private MarkerArtist markerArtist;
    private CompassManager compassManager;
    private FollowManager followManager;
    private FavoriteManager favoriteManager;

    private boolean isMapReady = false;
    private boolean isDataReady = false;
    private List<BusTrackerRegionData> pendingRegions;
    private List<BusTrackerNetworkData> pendingNetworks;

    private boolean isFetching = false;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutManager layoutManager = new LayoutManager(this);
        layoutManager.setupWindowInsets();

        SaveManager saveManager = new SaveManager(this);
        fetcher = new FetchingManager(this);
        lateralDrawerActivity = new LateralDrawerActivity(this, saveManager);
        compassManager = new CompassManager(this);
        followManager = new FollowManager(this);
        favoriteManager = new FavoriteManager(this, saveManager);
        markerArtist = new MarkerArtist(this, followManager, lateralDrawerActivity);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        markerArtist.setCachedMarkerView(LayoutInflater.from(this).inflate(R.layout.marker, null));
        markerArtist.setCachedUmMarkerView(LayoutInflater.from(this).inflate(R.layout.marker_um, null));

        fetcher.fetchLatestVersion(new FetchingManager.OnVersionListener() {
            @Override
            public void onResponseVersionListener(YnryoVersionResponse version) {
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    int latestVersionCode = version.getVersion().getVersionCode();
                    long localVersionCode = pInfo.getLongVersionCode();
                    Log.d(TAG, "Version locale: " + localVersionCode + ", version réseau: " + latestVersionCode);
                    if (latestVersionCode > localVersionCode) {
                        Toast.makeText(MainActivity.this, "Une nouvelle version est disponible", Toast.LENGTH_LONG).show();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("VersionChecker", e.toString());
                }
            }

            @Override
            public void onErrorVersionListener(String error) {
                Log.e(TAG, "Erreur de l'API: " + error);
            }
        });

        fetcher.fetchRegions(new FetchingManager.OnRegionsListener() {
            @Override
            public void onResponseRegionsListener(List<BusTrackerRegionData> regions) {
                fetcher.fetchNetworks(new FetchingManager.OnNetworkListener() {
                    @Override
                    public void onResponseNetworkListener(List<BusTrackerNetworkData> data) {
                        pendingRegions = regions;
                        pendingNetworks = data;
                        isDataReady = true;
                        onEverythingReady();
                    }

                    @Override
                    public void onErrorNetworkListener(String error) {
                        Log.e(TAG, "Erreur réseaux: " + error);
                    }
                });
            }

            @Override
            public void onErrorRegionsListener(String error) {
                Log.e(TAG, "Erreur régions: " + error);
            }
        });

        findViewById(R.id.btn_open_menu).setOnClickListener(view -> lateralDrawerActivity.open());
        findViewById(R.id.fab_center_location).setOnClickListener(view -> centerMapOnUserLocation());
    }

    private void onEverythingReady() {
        if (!isMapReady || !isDataReady) return;

        lateralDrawerActivity.populateNetworks(pendingRegions, pendingNetworks);
        centerMapOnUserLocation();
        fetchMarkers();
        handler.post(vehicleUpdateRunnable);
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        markerArtist.setGoogleMap(this.googleMap);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.setBuildingsEnabled(true);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PARIS, DEFAULT_ZOOM));
        this.googleMap.setOnCameraIdleListener(this);
        this.googleMap.setOnMarkerClickListener(this);
        this.googleMap.setOnMapClickListener(this);
        this.googleMap.setOnCameraMoveListener(this);
        this.googleMap.setOnCameraMoveStartedListener(this);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.getUiSettings().setCompassEnabled(false);
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (!hasLocationPermission()) {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            try {
                this.googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException onMapReady: " + e.getMessage());
            }
        }

        isMapReady = true;
        onEverythingReady();
    }

    public int dpToPx(int dp) {
        return LayoutManager.dpToPx(this, dp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMapReady && isDataReady) {
            handler.post(vehicleUpdateRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        followManager.disableFollow(false);
        handler.removeCallbacks(vehicleUpdateRunnable);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        markerArtist.onMarkerClick(marker);
        return true;
    }

    @Override
    public void onMapClick(@NonNull LatLng clickLatLng) {
        if (googleMap == null || markerArtist == null) return;

        Point clickScreenPoint = googleMap.getProjection().toScreenLocation(clickLatLng);
        int touchRadiusPx = dpToPx(36);

        Marker closestMarker = null;
        double minDistance = Double.MAX_VALUE;

        for (Marker marker : markerArtist.getActiveMarkers().values()) {
            if (marker == null || !marker.isVisible()) continue;
            Point markerScreenPoint = googleMap.getProjection().toScreenLocation(marker.getPosition());
            double dist = Math.hypot(clickScreenPoint.x - markerScreenPoint.x, clickScreenPoint.y - markerScreenPoint.y);
            if (dist <= touchRadiusPx && dist < minDistance) {
                minDistance = dist;
                closestMarker = marker;
            }
        }

        if (closestMarker != null) {
            markerArtist.onMarkerClick(closestMarker);
        }
    }

    @Override
    public void onCameraIdle() {
        fetchMarkers();
        markerArtist.updateMarkerRotations();
    }

    @Override
    public void onCameraMove() {
        compassManager.updateAzimuth(googleMap.getCameraPosition().bearing);
    }

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && followManager.getFollowedMarkerId() != null) {
            followManager.disableFollow(true);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void centerMapOnUserLocation() {
        if (googleMap == null) return;

        if (!hasLocationPermission()) {
            Log.d(TAG, "Pas de permission - reste à Paris");
            return;
        }

        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Log.d(TAG, "Position trouvée: " + location.getLatitude() + ", " + location.getLongitude());
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(userLocation)
                                    .zoom(15f)
                                    .tilt(0)
                                    .build()
                    ), 1000, null);
                } else {
                    Log.d(TAG, "getLastLocation() retourne null - reste à Paris");
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Erreur getLastLocation: " + e.getMessage()));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException centerMapOnUserLocation: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception centerMapOnUserLocation: " + e.getMessage());
        }
    }

    public void centerOnMarker(@NonNull String markerId, boolean isTilted, boolean isRotated) {
        Marker marker = markerArtist.getActiveMarkers().get(markerId);
        if (marker == null) return;
        MarkerStandardized markerStandardized = (MarkerStandardized) marker.getTag();
        if (markerStandardized == null) return;

        centerOnMarker(markerStandardized, isTilted, isRotated);
    }

    public void centerOnMarker(@NonNull MarkerStandardized markerStandardized, boolean isTilted, boolean isRotated) {
        float bearing = markerStandardized.getBearing();
        if (googleMap == null) return;
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(new LatLng(markerStandardized.getLatitude(), markerStandardized.getLongitude()))
                        .bearing(isRotated ? bearing : 0)
                        .tilt(isTilted ? 75f : 0f)
                        .zoom(17f)
                        .build()
        ), 2000, null);
    }

    private void fetchMarkers() {
        if (isFetching) return; //pour eviter le double fetching qui corromps les données
        isFetching = true;

        fetcher.fetchMarkers(new FetchingManager.OnMarkersListener() {
            @Override
            public void onResponseMarkersListener(List<MarkerStandardized> markerStandardizedList) {
                isFetching = false;
                markerArtist.showMarkers(markerStandardizedList);
                if (markerArtist.getMarkerIconCache().size() > 200)
                    markerArtist.getMarkerIconCache().clear();
            }

            @Override
            public void onErrorMarkersListener(String error) {
                isFetching = false;
                Log.e(TAG, "Erreur markers: " + error);
            }
        });
    }

    public GoogleMap getMap() {
        return googleMap;
    }

    public FetchingManager getFetcher() {
        return fetcher;
    }

    public FollowManager getFollowManager() {
        return followManager;
    }

    public MarkerArtist getMarkerArtist() {
        return markerArtist;
    }

    public FavoriteManager getFavoriteManager() {
        return favoriteManager;
    }
}