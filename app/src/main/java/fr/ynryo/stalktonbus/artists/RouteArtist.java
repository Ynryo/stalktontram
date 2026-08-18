package fr.ynryo.stalktonbus.artists;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.stalktonbus.MainActivity;
import fr.ynryo.stalktonbus.R;
import fr.ynryo.stalktonbus.apiResponsesPOJO.bus.BusTrackerVehiclePath;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStop;
import fr.ynryo.stalktonbus.managers.FetchingManager;

public class RouteArtist {
    private final static String TAG = "RouteArtist";
    private final MainActivity context;
    private String currentMarkerId;
    private Polyline currentRoutePolyline;
    private final List<Marker> stopMarkers = new ArrayList<>();

    public RouteArtist(MainActivity context) {
        this.context = context;
    }

    public void drawVehicleRoute(MarkerStandardized mData) {
        if (mData == null) return;
        if (mData.getPathRef() != null) {
            context.getFetcher().fetchBusLine(mData, new FetchingManager.OnRouteLineListener() {
                @Override
                public void onResponseRouteLineListener(MarkerStandardized mData) {
                    if (mData.getMarkerDataRoute() != null) {
                        PolylineOptions options = new PolylineOptions()
                                .width(16f)
                                .color(Color.parseColor(mData.getFillColor() != null ? mData.getFillColor() : "#424242"))
                                .geodesic(true)
                                .zIndex(2.0f);

                        boolean pointsAdded = false;

                        remove();
                        try {
                            List<List<Double>> allPoints;
                            Object geometry = ((BusTrackerVehiclePath) mData.getMarkerDataRoute()).getGeometry();
                            if (geometry instanceof List) {
                                allPoints = (List<List<Double>>) geometry;
                                for (List<Double> point : allPoints) {
                                    options.add(new LatLng(point.get(0), point.get(1)));
                                    pointsAdded = true;
                                }
                            }
                        } catch (ClassCastException e) {
                            remove();
                            Log.e(TAG, "Format de coordonnées invalide pour LineString");
                        }
                        if (pointsAdded) {
                            currentMarkerId = mData.getId();
                            currentRoutePolyline = context.getMap().addPolyline(options);
                            drawStopCircles(mData);
                            updateClearButtonVisibility();
                        }
                    }
                }

                @Override
                public void onErrorRouteLineListener(String error) {
                    remove();
                    Log.e(TAG, "Erreur lors de la récuperation du tracé\n" + error);
                }
            });
        }
    }

    private void drawStopCircles(MarkerStandardized mData) {
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();

        List<MarkerStop> stops = mData.getStops();
        if (stops == null || stops.isEmpty()) return;

        for (MarkerStop stop : stops) {
            if (stop.getLatitude() == 0 && stop.getLongitude() == 0) continue;

            BitmapDescriptor icon = createStopIcon(stop.getStopName(), mData);

            Marker marker = context.getMap().addMarker(new MarkerOptions()
                    .position(new LatLng(stop.getLatitude(), stop.getLongitude()))
                    .icon(icon)
                    .anchor(0.0f, 0.5f) // label à droite du point
                    .flat(false)
                    .zIndex(3f));
            if (marker != null) stopMarkers.add(marker);
        }
    }

    private BitmapDescriptor createStopIcon(String stopName, MarkerStandardized mData) {
        View view = LayoutInflater.from(context).inflate(R.layout.stop_marker, null);

        //point
        View dot = view.findViewById(R.id.stop_dot);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(Color.WHITE);
        dotDrawable.setStroke((int) (3 * context.getResources().getDisplayMetrics().density), mData.getFillColor() != null ? Color.parseColor(mData.getFillColor()) : Color.parseColor("#424242"));
        dot.setBackground(dotDrawable);

        //background label
        TextView tvName = view.findViewById(R.id.stop_name);
        tvName.setText(stopName);
        tvName.setTextColor(mData.getTextColor() != null ? Color.parseColor(mData.getTextColor()) : Color.parseColor("#424242"));
        GradientDrawable labelDrawable = new GradientDrawable();
        labelDrawable.setShape(GradientDrawable.RECTANGLE);
        labelDrawable.setColor(mData.getFillColor() != null ? Color.parseColor(mData.getFillColor()) : Color.parseColor("#424242"));
        labelDrawable.setAlpha(220);
        labelDrawable.setCornerRadius(8);
        tvName.setBackground(labelDrawable);

        //to bitmap
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void remove() {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
            currentRoutePolyline = null;
        }
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();
        currentMarkerId = null;
        updateClearButtonVisibility();
    }

    private void updateClearButtonVisibility() {
        if (context != null) {
            View clearBtn = context.findViewById(R.id.fab_clear_route);
            if (clearBtn != null) {
                clearBtn.setVisibility(currentRoutePolyline != null ? View.VISIBLE : View.GONE);
            }
        }
    }

    public boolean hasRoute() {
        return currentRoutePolyline != null;
    }

    public boolean isDrew(String markerId) {
        return currentMarkerId != null && currentMarkerId.equals(markerId);
    }

    public String getCurrentMarkerId() {
        return currentMarkerId;
    }
}