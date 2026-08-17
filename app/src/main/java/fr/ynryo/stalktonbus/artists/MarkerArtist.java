package fr.ynryo.stalktonbus.artists;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.ynryo.stalktonbus.LateralDrawerActivity;
import fr.ynryo.stalktonbus.MainActivity;
import fr.ynryo.stalktonbus.MarkerStopsDetailActivity;
import fr.ynryo.stalktonbus.R;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.managers.FollowManager;
import fr.ynryo.stalktonbus.managers.um.TrainUmDetector;

public class MarkerArtist {
    private static final String TAG = "MarkerArtist";
    private View cachedMarkerView;
    private View cachedUmMarkerView;
    private final MainActivity context;
    private GoogleMap googleMap;
    private final FollowManager followManager;
    private final LateralDrawerActivity lateralDrawerActivity;
    private final RouteArtist routeArtist;
    private final MarkerStopsDetailActivity markerStopsDetailActivity;
    private final Map<String, BitmapDescriptor> markerIconCache = new HashMap<>();
    private final Map<String, Marker> activeMarkers = new HashMap<>();
    private final Map<String, ValueAnimator> activeAnimators = new HashMap<>();

    private float oldMapRotation = 0;

    /**
     * Constructeur
     *
     * @param context               MainActivity
     * @param followManager         FollowManager
     * @param lateralDrawerActivity LateralDrawerActivity
     */
    public MarkerArtist(MainActivity context, FollowManager followManager, LateralDrawerActivity lateralDrawerActivity) {
        this.context = context;
        this.followManager = followManager;
        this.lateralDrawerActivity = lateralDrawerActivity;
        this.routeArtist = new RouteArtist(context);
        this.markerStopsDetailActivity = new MarkerStopsDetailActivity(context);
    }

    /**
     * Affiche les markers sur la carte
     *
     * @param markerStandardizedList List<MarkerStandardized>
     */
    public void showMarkers(List<MarkerStandardized> markerStandardizedList) {
        if (googleMap == null || markerStandardizedList == null) return;

        List<MarkerStandardized> displayList = TrainUmDetector.filterUm(markerStandardizedList);

        Set<String> fetchedMarkerIds = new HashSet<>();
        for (MarkerStandardized fetchedMarkerStandardized : displayList) { //on met tout les markers dans une liste
            fetchedMarkerIds.add(fetchedMarkerStandardized.getId());
        }

        //on nettoie les marker qui sont plus dans le fetch
        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator();
        while (iterator.hasNext()) { //pour chaque marker fetched
            Map.Entry<String, Marker> entry = iterator.next();
            String id = entry.getKey();
            Marker marker = entry.getValue();

            if (!fetchedMarkerIds.contains(id)) {
                //plus dans le fetch donc ça degage de l'affichage
                //mais on att d'être sur qu'il est vrmt mort avant de suppr le polyline et le follow
                marker.remove(); //remove map
                iterator.remove(); //remove list of active markers
                activeAnimators.remove(id); //remove animator
                if (id.equals(followManager.getFollowedMarkerId()) || id.equals(routeArtist.getCurrentMarkerId()) || id.equals(markerStopsDetailActivity.getCurrentVehicleId()))
                    checkVehicleAliveAndCleanup(id);
            }
        }

        //traitement des markers fetch
        for (MarkerStandardized fetchedMarkerStandardized : displayList) {
            String id = fetchedMarkerStandardized.getId();

            //on check par rapport au filtre réseau
            if (!lateralDrawerActivity.isNetworkVisible(fetchedMarkerStandardized.getNetworkRef())) {
                if (activeMarkers.containsKey(id)) {
                    // Filtré : on retire de l'affichage mais on garde follow/polyline car il existe
                    activeMarkers.get(id).remove();
                    activeMarkers.remove(id);
                    activeAnimators.remove(id);
                }
                continue;
            }

            float mapRotation = googleMap.getCameraPosition().bearing;
            LatLng position = new LatLng(fetchedMarkerStandardized.getLatitude(), fetchedMarkerStandardized.getLongitude());

            if (activeMarkers.containsKey(id)) {
                Marker existingMarker = activeMarkers.get(id);
                if (existingMarker != null) {
                    animateMarker(existingMarker, position, followManager.isFollowing(id));

                    MarkerStandardized oldData = (MarkerStandardized) existingMarker.getTag();
                    if (oldData == null || !Objects.equals(oldData.getFillColor(), fetchedMarkerStandardized.getFillColor()) || !Objects.equals(oldData.getLineId(), fetchedMarkerStandardized.getLineId()) || Math.abs(oldData.getBearing() - fetchedMarkerStandardized.getBearing()) > 5) {
                        existingMarker.setIcon(createMarkerBitmapDescriptor(fetchedMarkerStandardized, mapRotation, followManager.isFollowing(id)));
                    }

                    existingMarker.setTag(fetchedMarkerStandardized);
                }
            } else {
                Marker newMarker = googleMap.addMarker(new MarkerOptions().position(position).icon(createMarkerBitmapDescriptor(fetchedMarkerStandardized, mapRotation, followManager.isFollowing(id))).anchor(0.5f, 0.3f));

                if (newMarker != null) {
                    newMarker.setTag(fetchedMarkerStandardized);
                    activeMarkers.put(id, newMarker);
                }
            }
        }
    }

    private void checkVehicleAliveAndCleanup(String id) {
        context.getFetcher().fetchVehicleAlive(id, new FetchingManager.OnVehicleAliveListener() {
            @Override
            public void onResponseVehicleAliveListener(boolean isAlive) {
                if (!isAlive) {
                    //le vehicle est vrmt mort donc on supprime tout
                    if (id.equals(followManager.getFollowedMarkerId()))
                        followManager.disableFollow(false);
                    if (id.equals(routeArtist.getCurrentMarkerId())) routeArtist.remove();
                    if (id.equals(markerStopsDetailActivity.getCurrentVehicleId()))
                        markerStopsDetailActivity.close();
                }
            }

            @Override
            public void onErrorVehicleAliveListener(String error) {
                //if error
            }
        });
    }

    public Bitmap createMarker(MarkerStandardized markerStandardized, float mapRotation, boolean shouldFollow) {
        ImageView markerCircle = cachedMarkerView.findViewById(R.id.marker_circle);
        TextView lineNumberView = cachedMarkerView.findViewById(R.id.line_number);

        int fillColor = Color.parseColor(markerStandardized.getFillColor() != null ? markerStandardized.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerStandardized.getTextColor() != null ? markerStandardized.getTextColor() : "#FFFFFF");
        float bearing = markerStandardized.getBearing();

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.marker_circle);
        if (drawable != null) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();
            Drawable backgroundPart = layerDrawable.findDrawableByLayerId(R.id.marker_background);
            Drawable arrowPart = layerDrawable.findDrawableByLayerId(R.id.marker_arrow);

            if (backgroundPart != null) DrawableCompat.setTint(backgroundPart, fillColor);
            if (arrowPart != null) {
                DrawableCompat.setTint(arrowPart, fillColor);
                arrowPart.setAlpha(bearing == 0 ? 0 : 255);
            }
            markerCircle.setImageDrawable(layerDrawable);
        }

        lineNumberView.setText(markerStandardized.getLineNumber() != null ? markerStandardized.getLineNumber() : "BD");
        lineNumberView.setTextColor(textColor);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(10);
        lineNumberView.setBackground(gd);

        markerCircle.setRotation(shouldFollow ? 0 : bearing - mapRotation);

        cachedMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        cachedMarkerView.layout(0, 0, cachedMarkerView.getMeasuredWidth(), cachedMarkerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(cachedMarkerView.getMeasuredWidth(), cachedMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cachedMarkerView.draw(canvas);

        return bitmap;
    }

    public Bitmap createUmMarker(MarkerStandardized markerStandardized, float mapRotation, boolean shouldFollow) {
        ImageView markerCircleUm = cachedUmMarkerView.findViewById(R.id.marker_circle_um);
        TextView lineNumberView = cachedUmMarkerView.findViewById(R.id.line_number);

        int fillColor = Color.parseColor(markerStandardized.getFillColor() != null ? markerStandardized.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerStandardized.getTextColor() != null ? markerStandardized.getTextColor() : "#FFFFFF");
        float bearing = markerStandardized.getBearing();

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.marker_circle_um);
        if (drawable != null) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();
            Drawable backgroundPart = layerDrawable.findDrawableByLayerId(R.id.marker_background);
            Drawable arrowPart = layerDrawable.findDrawableByLayerId(R.id.marker_arrow);

            if (backgroundPart != null) DrawableCompat.setTint(backgroundPart, fillColor);
            if (arrowPart != null) {
                DrawableCompat.setTint(arrowPart, fillColor);
                arrowPart.setAlpha(bearing == 0 ? 0 : 255);
            }
            markerCircleUm.setImageDrawable(layerDrawable);
        }

        lineNumberView.setText(markerStandardized.getLineNumber() != null ? markerStandardized.getLineNumber() : "BD");
        lineNumberView.setTextColor(textColor);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(10);
        lineNumberView.setBackground(gd);

        markerCircleUm.setRotation(shouldFollow ? 0 : bearing - mapRotation);

        cachedUmMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        cachedUmMarkerView.layout(0, 0, cachedUmMarkerView.getMeasuredWidth(), cachedUmMarkerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(cachedUmMarkerView.getMeasuredWidth(), cachedUmMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cachedUmMarkerView.draw(canvas);

        return bitmap;
    }


    public BitmapDescriptor createMarkerBitmapDescriptor(MarkerStandardized markerStandardized, float mapRotation, boolean shouldFollow) {
        String cacheKey = (markerStandardized.isUm() ? "UM_" : "US_")
                + markerStandardized.getFillColor() + "_"
                + markerStandardized.getLineId() + "_"
                + (int) (markerStandardized.getBearing() - mapRotation) + "_"
                + markerStandardized.getId();
//        Log.d(TAG, "cacheKey=" + cacheKey + " isUm=" + markerStandardized.isUm() + " cacheHit=" + markerIconCache.containsKey(cacheKey));

        if (markerIconCache.containsKey(cacheKey)) {
            return markerIconCache.get(cacheKey);
        }

        Bitmap bitmap;
        if (markerStandardized.isUm()) {
            bitmap = createUmMarker(markerStandardized, mapRotation, shouldFollow);
        } else {
            bitmap = createMarker(markerStandardized, mapRotation, shouldFollow);
        }

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerIconCache.put(cacheKey, descriptor);

        return descriptor;
    }

    public void updateMarkerRotations() {
        if (googleMap == null) return;

        float mapRotation = googleMap.getCameraPosition().bearing;
        if (mapRotation == oldMapRotation) return;
        oldMapRotation = mapRotation;

        for (Map.Entry<String, Marker> entry : activeMarkers.entrySet()) {
            Marker marker = entry.getValue();
            MarkerStandardized data = (MarkerStandardized) marker.getTag();
            if (data != null) {
                marker.setIcon(createMarkerBitmapDescriptor(data, mapRotation, followManager.isFollowing(data.getId())));
            }
        }
    }

    public void onMarkerClick(@NonNull Marker marker) {
        MarkerStandardized mData = (MarkerStandardized) marker.getTag();
        if (mData != null) {
            markerStopsDetailActivity.open(mData);
        }
    }

    public void animateMarker(final Marker marker, final LatLng toPosition, boolean shouldFollow) {
        final LatLng startPosition = marker.getPosition();
        String markerId = marker.getTag() != null ? ((MarkerStandardized) marker.getTag()).getId() : "";

        ValueAnimator existing = activeAnimators.get(markerId);
        if (existing != null && existing.isRunning()) {
            existing.cancel();
        }

        if (shouldFollow && googleMap != null) {
            MarkerStandardized data = (MarkerStandardized) marker.getTag();
            float bearing = data != null ? data.getBearing() : 0f;

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(toPosition).bearing(bearing).tilt(60f).zoom(17f).build()), 2000, null);
        }

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(2000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();

            double lng = v * toPosition.longitude + (1 - v) * startPosition.longitude;
            double lat = v * toPosition.latitude + (1 - v) * startPosition.latitude;
            LatLng newPos = new LatLng(lat, lng);
            marker.setPosition(newPos);
        });

        activeAnimators.put(markerId, valueAnimator);
        valueAnimator.start();
    }

    public void setCachedMarkerView(View cachedMarkerView) {
        this.cachedMarkerView = cachedMarkerView;
    }

    public void setCachedUmMarkerView(View cachedUmMarkerView) {
        this.cachedUmMarkerView = cachedUmMarkerView;
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public RouteArtist getRouteArtist() {
        return routeArtist;
    }

    public Map<String, Marker> getActiveMarkers() {
        return activeMarkers;
    }

    public Map<String, BitmapDescriptor> getMarkerIconCache() {
        return markerIconCache;
    }

    public MarkerStopsDetailActivity getMarkerStopsDetailActivity() {
        return markerStopsDetailActivity;
    }
}
