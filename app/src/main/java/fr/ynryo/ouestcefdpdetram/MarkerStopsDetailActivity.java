package fr.ynryo.ouestcefdpdetram;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PictureDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerDataStop;
import fr.ynryo.ouestcefdpdetram.managers.FetchingManager;
import fr.ynryo.ouestcefdpdetram.managers.um.TimelineRowType;
import fr.ynryo.ouestcefdpdetram.managers.um.TrainUmAssembler;
import fr.ynryo.ouestcefdpdetram.managers.um.TrainUmTimelineRow;

public class MarkerStopsDetailActivity {
    private static final String TAG = "MarkerStopsDetailActivity";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private final MainActivity context;
    private BottomSheetDialog bottomSheetDialog;
    private String vehicleId;

    public MarkerStopsDetailActivity(MainActivity context) {
        WeakReference<MainActivity> contextRef = new WeakReference<>(context);
        this.context = contextRef.get();
    }

    public void open(MarkerDataStandardized markerDataStandardized) {
        if (this.context == null) return;
        close();

        bottomSheetDialog = new BottomSheetDialog(context);
        vehicleId = markerDataStandardized.getId();

        View view = LayoutInflater.from(context).inflate(R.layout.vehicle_details, null);
        bottomSheetDialog.setContentView(view);
        setupBottomSheetAppearance(view);
        setupLineHeader(view, markerDataStandardized);
        setupLoader(view, markerDataStandardized);

        bottomSheetDialog.show();
        fetchVehicleData(markerDataStandardized, view);
    }

    public void close() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
            bottomSheetDialog = null;
        }
    }

    // ==================== SETUP ====================
    private void setupBottomSheetAppearance(View view) {
        if (bottomSheetDialog.getWindow() != null) {
            WindowCompat.setDecorFitsSystemWindows(bottomSheetDialog.getWindow(), false);
        }

        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> insets);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                int peekHeight = calculatePeekHeight();
                behavior.setPeekHeight(peekHeight);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            View nsvContent = view.findViewById(R.id.nsvContent);
            if (nsvContent != null && context != null) {
                nsvContent.setPadding(
                        nsvContent.getPaddingLeft(),
                        nsvContent.getPaddingTop(),
                        nsvContent.getPaddingRight(),
                        insets.bottom + context.dpToPx(16)
                );
                if (nsvContent instanceof androidx.core.widget.NestedScrollView) {
                    ((androidx.core.widget.NestedScrollView) nsvContent).setClipToPadding(false);
                }
            }
            return windowInsets;
        });
    }

    private int calculatePeekHeight() {
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        return (int) (screenHeight * 0.5);
    }

    private void setupLineHeader(View view, MarkerDataStandardized markerDataStandardized) {
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);

        String lineNumber = markerDataStandardized.getLineNumber();
        tvLigne.setText(lineNumber);

        int fillColor = Color.parseColor(markerDataStandardized.getFillColor() != null ? markerDataStandardized.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerDataStandardized.getTextColor() != null ? markerDataStandardized.getTextColor() : "#FFFFFF");

        tvLigne.setBackgroundColor(fillColor);
        tvLigne.setTextColor(textColor);
    }

    private void setupLoader(View view, MarkerDataStandardized markerDataStandardized) {
        ProgressBar loader = view.findViewById(R.id.loader);
        int fillColor = Color.parseColor(markerDataStandardized.getFillColor() != null ? markerDataStandardized.getFillColor() : "#424242");

        loader.setVisibility(View.VISIBLE);
        loader.setIndeterminateTintList(ColorStateList.valueOf(fillColor));

        view.findViewById(R.id.llStopsContent).setVisibility(View.INVISIBLE);
    }

    // ==================== DATA FETCHING ====================

    /**
     * Fetch data from API
     * @param markerDataStandardized the marker data
     * @param view the view
     */
    private void fetchVehicleData(MarkerDataStandardized markerDataStandardized, View view) {
        context.getFetcher().fetchVehicleStopsInfo(markerDataStandardized, new FetchingManager.OnVehicleDetailsListener() {
            @Override
            public void onResponseVehicleDetailsListener(MarkerDataStandardized markerDataStandardized) {
                hideLoader(view);

                if (context.getMarkerArtist() != null) {
                    context.getMarkerArtist().getRouteArtist().drawVehicleRoute(markerDataStandardized);
                }

                showVehicleDetails(markerDataStandardized, view);
                fetchNetworkLogo(markerDataStandardized, view);
            }

            @Override
            public void onErrorVehicleDetailsListener(String error) {
                hideLoader(view);
                showError(view);
            }
        });
    }

    /**
     * Fetch network logo from API
     * @param markerDataStandardized the marker data
     * @param view the view
     */
    private void fetchNetworkLogo(MarkerDataStandardized markerDataStandardized, View view) {
        if (markerDataStandardized.getNetworkId() == 0) return;

        context.getFetcher().fetchNetworkData(markerDataStandardized.getNetworkId(), new FetchingManager.OnNetworkDataListener() {
            @Override
            public void onResponseNetworkDataListener(NetworkData nData) {
                loadNetworkLogo(view, nData.getLogoHref());
            }

            @Override
            public void onErrorNetworkDataListener(String error) {
                Log.w(TAG, "Erreur lors de la récuperation du logo");
            }
        });
    }

    /**
     * Load network logo from API
     * @param view the view
     * @param imgURI the URI of the logo
     */
    private void loadNetworkLogo(View view, URI imgURI) {
        ImageView ivLogo = view.findViewById(R.id.ivNetworkLogo);
        if (imgURI == null) {
            ivLogo.setVisibility(View.GONE);
            return;
        }

        ivLogo.setVisibility(View.VISIBLE);
        ivLogo.setBackgroundResource(R.color.surface_light);
        ivLogo.setAdjustViewBounds(true);
        ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(context)
                .as(PictureDrawable.class)
                .load(imgURI.toString())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .override(100, 100)
                .into(ivLogo);
    }

    /**
     * Hide loader from view
     * @param view the view
     */
    private void hideLoader(View view) {
        view.findViewById(R.id.loader).setVisibility(View.GONE);
    }

    /**
     * Show error from view
     * @param view the view
     */
    private void showError(View view) {
        TextView tvDest = view.findViewById(R.id.tvDestination);
        tvDest.setText(R.string.network_error);
    }

    // ==================== DISPLAY ====================

    /**
     * Show vehicle details from marker data
     * @param markerDataStandardized the marker data
     * @param view the view
     */
    private void showVehicleDetails(MarkerDataStandardized markerDataStandardized, View view) {
        context.getFollowManager().setFollowButton(view.findViewById(R.id.followButton), markerDataStandardized.getId());
        context.getFavoriteManager().setFavoriteButton(view.findViewById(R.id.favoriteButton), markerDataStandardized);

        setupDestinationText(view, markerDataStandardized);
        setupStopsList(view, markerDataStandardized);
    }

    /**
     * Setup destination text from marker data
     * @param view the view
     * @param markerDataStandardized the marker data
     */
    private void setupDestinationText(View view, MarkerDataStandardized markerDataStandardized) {
        TextView tvDestination = view.findViewById(R.id.tvDestination);
        tvDestination.setText(markerDataStandardized.getDestination());
        tvDestination.setSingleLine(true);
        tvDestination.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvDestination.setMarqueeRepeatLimit(-1);
        tvDestination.setHorizontallyScrolling(true);
        tvDestination.setSelected(true);
    }

    private void setupStopsList(View view, MarkerDataStandardized markerDataStandardized) {
        RecyclerView rvStops = view.findViewById(R.id.rvStops);
        rvStops.setLayoutManager(new LinearLayoutManager(context));

        if (markerDataStandardized.isUm()) {
            List<TrainUmTimelineRow> rows = TrainUmAssembler.assembleUmStops(markerDataStandardized);
            rvStops.setAdapter(new UmStopsAdapter(markerDataStandardized, rows));
        } else {
            List<MarkerDataStop> stops = markerDataStandardized.getStops() != null ? markerDataStandardized.getStops() : new ArrayList<>();
            rvStops.setAdapter(new StopsAdapter(stops));
        }

        view.findViewById(R.id.llStopsContent).setVisibility(View.VISIBLE);
    }

    public String getCurrentVehicleId() {
        return vehicleId;
    }

    // ==================== ADAPTER ====================

    private static class StopViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPlatform, tvStopName, tvDepartureTime, tvAtStopTime, tvArrivingTime, tvDelay;
        final View spacerPlatform;
        final ImageView ivArrivingTimeIcon, ivDepartureTimeIcon;
        final FrameLayout flTimeline;

        StopViewHolder(View itemView) {
            super(itemView);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvAtStopTime = itemView.findViewById(R.id.tvAtStopTime);
            tvArrivingTime = itemView.findViewById(R.id.tvArrivingTime);
            tvDelay = itemView.findViewById(R.id.tvDelay);
            spacerPlatform = itemView.findViewById(R.id.spacerPlatform);
            ivArrivingTimeIcon = itemView.findViewById(R.id.ivArrivingTimeIcon);
            ivDepartureTimeIcon = itemView.findViewById(R.id.ivDepartureTimeIcon);
            flTimeline = itemView.findViewById(R.id.flTimeline);
        }
    }

    private static int getTimelineLayout(MarkerDataStop stop, int position, int itemCount) {
        Log.d(TAG, "isBusAtDeparture" + stop.getVehicle().isVehicle() + " " + stop.getVehicle().getDistanceTraveled() + " " + stop.isDepartureStop());
        boolean isTrainAtDeparture = stop.getVehicle().isTrain() && position == 0;
        boolean isBusAtDeparture = stop.getVehicle().isVehicle() && (stop.getVehicle().getDistanceTraveled() == 0) && stop.isDepartureStop();
        boolean isVehicleAtArrival = stop.isDestinationStop() || position == itemCount - 1;
        if (isTrainAtDeparture || isBusAtDeparture) {
            return R.layout.timeline_first_stop;
        } else if (isVehicleAtArrival) {
            return R.layout.timeline_last_stop;
        } else {
            return R.layout.timeline_intermediate_stop;
        }
    }

    /**
     * Stops adapter for recycler view
     */
    private class StopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_STOP = 0;
        private static final int TYPE_EMPTY = 1;

        private final List<MarkerDataStop> stops;

        /**
         * Constructor
         * @param stops the list of stops
         */
        StopsAdapter(List<MarkerDataStop> stops) {
            this.stops = stops;
        }

        /**
         * Get item view type
         * @param position position to query
         * @return the item view type
         */
        @Override
        public int getItemViewType(int position) {
            return stops.isEmpty() ? TYPE_EMPTY : TYPE_STOP;
        }

        /**
         * Create a view holder
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @param viewType The view type of the new View.
         * @return A new ViewHolder that holds a View of the given view type.
         */
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_EMPTY) {
                return createEmptyViewHolder(parent);
            }
            return createStopViewHolder(parent);
        }

        /**
         * Bind view holder
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_EMPTY) {
                bindEmptyViewHolder(holder);
                return;
            }

            MarkerDataStop stop = stops.get(position);
            bindStopViewHolder((StopViewHolder) holder, stop, position, stops.size());
        }

        /**
         * Get item count
         * @return the item count
         */
        @Override
        public int getItemCount() {
            return stops.isEmpty() ? 1 : stops.size();
        }

        // ========== NO DATA ==========

        /**
         * Create empty view holder
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @return A new ViewHolder that holds a View of the given view type.
         */
        private RecyclerView.ViewHolder createEmptyViewHolder(ViewGroup parent) {
            TextView tvEmpty = new TextView(parent.getContext());
            tvEmpty.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tvEmpty.setPadding(0, 32, 0, 32);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            return new RecyclerView.ViewHolder(tvEmpty) {};
        }

        /**
         * Bind empty view holder
         * @param holder The ViewHolder which should be updated to represent the contents of the
         *               item at the given position in the data set.
         */
        private void bindEmptyViewHolder(RecyclerView.ViewHolder holder) {
            TextView tvEmpty = (TextView) holder.itemView;
            tvEmpty.setText(R.string.no_data);
            tvEmpty.setTextColor(MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnSurface
            ));
        }

        // ========== STOP ITEM ==========
        private RecyclerView.ViewHolder createStopViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_stop_details, parent, false);
            return new StopViewHolder(view);
        } //inflate item stop

        private void bindStopViewHolder(StopViewHolder vh, MarkerDataStop stop, int position, int itemCount) { //distribute data
            bindTimeline(vh, stop, position, itemCount);
            bindPlatform(vh, stop);
            bindStopName(vh, stop);
            bindArrivalTime(vh, stop);
            bindAtStopTime(vh, stop);
            bindDepartureTime(vh, stop);
            bindDelay(vh, stop);
        }

        public void bindTimeline(StopViewHolder vh, MarkerDataStop stop, int position, int itemCount) {
            MarkerDataStandardized vehicle = stop.getVehicle();

            vh.flTimeline.setVisibility(View.VISIBLE);
            vh.flTimeline.removeAllViews();

            // Inflate le layout dedans
            View timelineView = LayoutInflater.from(context).inflate(getTimelineLayout(stop, position, itemCount), vh.flTimeline, true);

            // Tinte la barre avec la couleur du train
            int fillColor = Color.parseColor(vehicle.getFillColor() != null ? vehicle.getFillColor() : "#424242");

            View lineView = timelineView.findViewById(R.id.vLineBottom);
            if (lineView == null) lineView = timelineView.findViewById(R.id.vLineTop);
            if (lineView == null) lineView = timelineView.findViewById(R.id.vLineFull);

            if (lineView != null) {
                ((GradientDrawable) lineView.getBackground().mutate()).setColor(fillColor);
            }

        }

        private void bindPlatform(StopViewHolder vh, MarkerDataStop stop) {
            String platformName = stop.getPlatformName();
            if (platformName != null && !platformName.isEmpty()) {
                vh.tvPlatform.setText(platformName);
                vh.tvPlatform.setVisibility(View.VISIBLE);
                vh.tvPlatform.setTextColor(Color.WHITE);
                vh.spacerPlatform.setVisibility(View.VISIBLE);

                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setStroke(2, Color.WHITE);
                gd.setCornerRadius(10);
                vh.tvPlatform.setBackground(gd);
            } else {
                vh.tvPlatform.setVisibility(View.GONE);
                vh.spacerPlatform.setVisibility(View.GONE);
            }
        }

        private void bindStopName(StopViewHolder vh, MarkerDataStop stop) {
            SpannableStringBuilder builder = new SpannableStringBuilder(stop.getStopName());

            int iconRes = getStopIconResource(stop);
            if (iconRes != 0) {
                appendStopIcon(vh, builder, iconRes);
            }

            vh.tvStopName.setText(builder);
            vh.tvStopName.setSelected(true);
        }

        private int getStopIconResource(MarkerDataStop stop) {
            if (stop.cantPickup()) return R.drawable.icon_logout;
            if (stop.cantDropoff()) return R.drawable.icon_login;
            return 0;
        }

        private void appendStopIcon(StopViewHolder vh, SpannableStringBuilder builder, int iconRes) {
            builder.append("  ");
            Drawable d = ContextCompat.getDrawable(context, iconRes);
            if (d != null) {
                d.mutate();
                d.setColorFilter(new PorterDuffColorFilter(
                        MaterialColors.getColor(vh.tvStopName, com.google.android.material.R.attr.colorOnSurface),
                        PorterDuff.Mode.SRC_IN
                ));
                int size = (int) (vh.tvStopName.getTextSize() * 1.2f);
                d.setBounds(0, 0, size, size);
                builder.setSpan(
                        new ImageSpan(d, ImageSpan.ALIGN_BOTTOM),
                        builder.length() - 1,
                        builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        private void bindArrivalTime(StopViewHolder vh, MarkerDataStop stop) {
            MarkerDataStandardized vehicle = stop.getVehicle();
            if (!vehicle.isTrain() || stop.isDepartureStop()) {
                vh.tvArrivingTime.setVisibility(View.GONE);
                return;
            }

            LocalTime arrivalTime = stop.getArrivalTime();
            if (arrivalTime != null) {
                vh.tvArrivingTime.setVisibility(View.VISIBLE);
                vh.tvArrivingTime.setText(arrivalTime.format(TIME_FORMATTER));
                vh.tvArrivingTime.setTextColor(stop.isOnLive() ? COLOR_GREEN : getDefaultTextColor(vh));
                bindOnLive(vh.ivArrivingTimeIcon, stop);
            } else {
                vh.tvArrivingTime.setVisibility(View.GONE);
            }
        }

        private void bindAtStopTime(StopViewHolder vh, MarkerDataStop stop) {
            MarkerDataStandardized vehicle = stop.getVehicle();
            if (!vehicle.isTrain() || stop.isDestinationStop() || stop.isDepartureStop()) {
                vh.tvAtStopTime.setVisibility(View.GONE);
                return;
            }
            Long atStopMinutes = stop.getAtStopTime();
            if (atStopMinutes != null && atStopMinutes >= 0) {
                vh.tvAtStopTime.setVisibility(View.VISIBLE);
                vh.tvAtStopTime.setText(atStopMinutes + "min d'arrêt");
                vh.tvAtStopTime.setTextColor(Color.GRAY);
            } else {
                vh.tvAtStopTime.setText("—");
            }
        }

        private void bindDepartureTime(StopViewHolder vh, MarkerDataStop stop) {
            if (stop.isDestinationStop()) {
                vh.tvDepartureTime.setVisibility(View.GONE);
                return;
            }

            LocalTime departureTime = stop.getDepartureTime();
            if (departureTime != null) {
                vh.tvDepartureTime.setVisibility(View.VISIBLE);
                vh.tvDepartureTime.setText(departureTime.format(TIME_FORMATTER));
                vh.tvDepartureTime.setTextColor(stop.isOnLive() ? COLOR_GREEN : getDefaultTextColor(vh));
                bindOnLive(vh.ivDepartureTimeIcon, stop);
            } else {
                vh.tvDepartureTime.setText("??:??");
            }
        }

        private void bindOnLive(ImageView ivTimeIcon, MarkerDataStop stop) {
            if (stop.isOnLive()) {
                ivTimeIcon.setImageResource(R.drawable.icon_sensors);
                ivTimeIcon.setColorFilter(COLOR_GREEN);
                ivTimeIcon.setVisibility(View.VISIBLE);
//                vh.tvAtStopTime.setLayoutParams(new LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                ));
            } else {
                ivTimeIcon.setVisibility(View.GONE);
            }
        }

        private void bindDelay(StopViewHolder vh, MarkerDataStop stop) {
            vh.tvDelay.setVisibility(View.GONE);

            if (stop.getDelay() == null || stop.getDelay() == 0) return;

            vh.tvDelay.setVisibility(View.VISIBLE);
            vh.tvDelay.setText(stop.getDelayText());
            vh.tvDelay.setTextColor(stop.getDelayColor());
        }

        private int getDefaultTextColor(StopViewHolder vh) {
            return MaterialColors.getColor(vh.tvDepartureTime, com.google.android.material.R.attr.colorOnSurface);
        }
    }

    // ==================== UM ADAPTER ====================

    private static class UmStopViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPlatform, tvStopName;
        final View spacerPlatform;
        final FrameLayout flTimeline;

        // Train A columns
        final View llTrainAData;
        final TextView tvArrivingTime, tvAtStopTime, tvDepartureTime, tvDelay;
        final ImageView ivArrivingTimeIcon, ivDepartureTimeIcon;

        // Separator
        final View vSplitSeparator;

        // Train B columns
        final View llTrainBData;
        final TextView tvArrivingTimeB, tvAtStopTimeB, tvDepartureTimeB, tvDelayB;
        final ImageView ivArrivingTimeIconB, ivDepartureTimeIconB;

        UmStopViewHolder(View itemView) {
            super(itemView);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            spacerPlatform = itemView.findViewById(R.id.spacerPlatform);
            flTimeline = itemView.findViewById(R.id.flTimeline);

            llTrainAData = itemView.findViewById(R.id.llTrainAData);
            tvArrivingTime = itemView.findViewById(R.id.tvArrivingTime);
            tvAtStopTime = itemView.findViewById(R.id.tvAtStopTime);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvDelay = itemView.findViewById(R.id.tvDelay);
            ivArrivingTimeIcon = itemView.findViewById(R.id.ivArrivingTimeIcon);
            ivDepartureTimeIcon = itemView.findViewById(R.id.ivDepartureTimeIcon);

            vSplitSeparator = itemView.findViewById(R.id.vSplitSeparator);

            llTrainBData = itemView.findViewById(R.id.llTrainBData);
            tvArrivingTimeB = itemView.findViewById(R.id.tvArrivingTimeB);
            tvAtStopTimeB = itemView.findViewById(R.id.tvAtStopTimeB);
            tvDepartureTimeB = itemView.findViewById(R.id.tvDepartureTimeB);
            tvDelayB = itemView.findViewById(R.id.tvDelayB);
            ivArrivingTimeIconB = itemView.findViewById(R.id.ivArrivingTimeIconB);
            ivDepartureTimeIconB = itemView.findViewById(R.id.ivDepartureTimeIconB);
        }
    }

    private static class GraphicViewHolder extends RecyclerView.ViewHolder {
        GraphicViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class UmStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_STOP = 0;
        private static final int TYPE_MERGE = 1;
        private static final int TYPE_SPLIT = 2;

        private final MarkerDataStandardized umMarker;
        private final List<TrainUmTimelineRow> rows;

        UmStopsAdapter(MarkerDataStandardized umMarker, List<TrainUmTimelineRow> rows) {
            this.umMarker = umMarker;
            this.rows = rows;
        }

        @Override
        public int getItemViewType(int position) {
            TrainUmTimelineRow row = rows.get(position);
            if (row.getType() == TimelineRowType.MERGE_GRAPHIC) {
                return TYPE_MERGE;
            } else if (row.getType() == TimelineRowType.SPLIT_GRAPHIC) {
                return TYPE_SPLIT;
            } else {
                return TYPE_STOP;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_MERGE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_um_merge, parent, false);
                return new GraphicViewHolder(view);
            } else if (viewType == TYPE_SPLIT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_um_split, parent, false);
                return new GraphicViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_stop_details, parent, false);
                return new UmStopViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TrainUmTimelineRow row = rows.get(position);
            int viewType = getItemViewType(position);

            MarkerDataStandardized trainA = umMarker.getUmA();
            MarkerDataStandardized trainB = umMarker.getUmB();
            int colorA = Color.parseColor(trainA.getFillColor() != null ? trainA.getFillColor() : "#424242");
            int colorB = Color.parseColor(trainB.getFillColor() != null ? trainB.getFillColor() : "#424242");

            if (viewType == TYPE_MERGE || viewType == TYPE_SPLIT) {
                tintGraphicView(holder.itemView, colorA, colorB);
            } else {
                UmStopViewHolder vh = (UmStopViewHolder) holder;
                bindStopRow(vh, row, colorA, colorB);
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        private void tintGraphicView(View itemView, int colorA, int colorB) {
            ImageView lineMain = itemView.findViewById(R.id.line_main);
            ImageView curveTop = itemView.findViewById(R.id.curve_top);
            View lineHorizontal = itemView.findViewById(R.id.line_horizontal);
            ImageView dirArrowUp = itemView.findViewById(R.id.direction_arrow_up);
            ImageView dirArrow = itemView.findViewById(R.id.direction_arrow);
            ImageView curveBottom = itemView.findViewById(R.id.curve_bottom);
            ImageView lineSecondaryDown = itemView.findViewById(R.id.line_secondary_down);

            if (lineMain != null) lineMain.setColorFilter(colorA, PorterDuff.Mode.SRC_IN);
            if (dirArrow != null) dirArrow.setColorFilter(colorA, PorterDuff.Mode.SRC_IN);

            if (curveTop != null) curveTop.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
            if (lineHorizontal != null) {
                if (lineHorizontal instanceof ImageView) {
                    ((ImageView) lineHorizontal).setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
                } else if (lineHorizontal.getBackground() != null) {
                    ((GradientDrawable) lineHorizontal.getBackground().mutate()).setColor(colorB);
                }
            }
            if (dirArrowUp != null) dirArrowUp.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
            if (curveBottom != null) curveBottom.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
            if (lineSecondaryDown != null)
                lineSecondaryDown.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
        }

        private void bindStopRow(UmStopViewHolder vh, TrainUmTimelineRow row, int colorA, int colorB) {
            MarkerDataStop stopA = row.getStopA();
            MarkerDataStop stopB = row.getStopB();

            // 1. Bind Timeline
            bindTimeline(vh, row);

            // 2. Determine Stop Name & Icon
            String stopName = "";
            int iconRes = 0;
            String nameA = stopA != null ? stopA.getStopName() : null;
            String nameB = stopB != null ? stopB.getStopName() : null;

            if (nameA != null && nameB != null) {
                if (nameA.equals(nameB)) {
                    stopName = nameA;
                } else {
                    stopName = nameA + " / " + nameB;
                }
            } else if (nameA != null) {
                stopName = nameA;
            } else if (nameB != null) {
                stopName = nameB;
            }

            if (stopA != null) {
                iconRes = getStopIconResource(stopA);
            }
            if (iconRes == 0 && stopB != null) {
                iconRes = getStopIconResource(stopB);
            }
            bindStopName(vh.tvStopName, stopName, iconRes);

            // 3. Determine Platform
            String platform = "";
            if (stopA != null && stopB != null) {
                String platA = stopA.getPlatformName();
                String platB = stopB.getPlatformName();
                if (platA != null && platB != null) {
                    if (platA.equals(platB)) {
                        platform = platA;
                    } else {
                        platform = platA + "/" + platB;
                    }
                } else if (platA != null) {
                    platform = platA;
                } else if (platB != null) {
                    platform = platB;
                }
            } else if (stopA != null) {
                platform = stopA.getPlatformName();
            } else if (stopB != null) {
                platform = stopB.getPlatformName();
            }
            bindPlatform(vh.tvPlatform, vh.spacerPlatform, platform);

            // 4. Bind Columns
            if (row.getType() == TimelineRowType.COMMON) {
                bindTrainColumn(stopA, (LinearLayout) vh.llTrainAData,
                        vh.tvArrivingTime, vh.ivArrivingTimeIcon,
                        vh.tvAtStopTime,
                        vh.tvDepartureTime, vh.ivDepartureTimeIcon,
                        vh.tvDelay);
                vh.llTrainBData.setVisibility(View.GONE);
                vh.vSplitSeparator.setVisibility(View.GONE);
            } else {
                // SIDE_BY_SIDE stop
                bindTrainColumn(stopA, (LinearLayout) vh.llTrainAData,
                        vh.tvArrivingTime, vh.ivArrivingTimeIcon,
                        vh.tvAtStopTime,
                        vh.tvDepartureTime, vh.ivDepartureTimeIcon,
                        vh.tvDelay);
                bindTrainColumn(stopB, (LinearLayout) vh.llTrainBData,
                        vh.tvArrivingTimeB, vh.ivArrivingTimeIconB,
                        vh.tvAtStopTimeB,
                        vh.tvDepartureTimeB, vh.ivDepartureTimeIconB,
                        vh.tvDelayB);

                boolean showSep = (stopA != null && stopB != null);
                vh.vSplitSeparator.setVisibility(showSep ? View.VISIBLE : View.GONE);
            }
        }

        private void bindTimeline(UmStopViewHolder vh, TrainUmTimelineRow row) {
            vh.flTimeline.setVisibility(View.VISIBLE);
            vh.flTimeline.removeAllViews();

            View timelineView = LayoutInflater.from(context).inflate(R.layout.timeline_um_stop, vh.flTimeline, true);

            View vLineLeft = timelineView.findViewById(R.id.vLineLeft);
            View vStopDotLeft = timelineView.findViewById(R.id.vStopDotLeft);
            View vLineRight = timelineView.findViewById(R.id.vLineRight);
            View vStopDotRight = timelineView.findViewById(R.id.vStopDotRight);

            MarkerDataStandardized trainA = umMarker.getUmA();
            MarkerDataStandardized trainB = umMarker.getUmB();

            int colorA = Color.parseColor(trainA.getFillColor() != null ? trainA.getFillColor() : "#424242");
            int colorB = Color.parseColor(trainB.getFillColor() != null ? trainB.getFillColor() : "#424242");

            boolean isFirst = row.isFirstPosition();
            boolean isLast = row.isLastPosition();
            int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

            configureLine(vLineLeft, isFirst, isLast, margin, colorA);
            configureLine(vLineRight, isFirst, isLast, margin, colorB);

            if (row.getType() == TimelineRowType.COMMON) {
                vLineRight.setVisibility(View.INVISIBLE);
                vStopDotRight.setVisibility(View.INVISIBLE);
                vStopDotLeft.setVisibility(View.VISIBLE);
            } else {
                vLineRight.setVisibility(View.VISIBLE);
                vStopDotLeft.setVisibility(row.getStopA() != null ? View.VISIBLE : View.GONE);
                vStopDotRight.setVisibility(row.getStopB() != null ? View.VISIBLE : View.GONE);
            }
        }

        private void configureLine(View vLine, boolean isFirst, boolean isLast, int margin, int color) {
            if (isFirst) {
                vLine.setBackgroundResource(R.drawable.timeline_bar_top_round);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vLine.getLayoutParams();
                lp.topMargin = margin;
                lp.bottomMargin = 0;
                vLine.setLayoutParams(lp);
            } else if (isLast) {
                vLine.setBackgroundResource(R.drawable.timeline_bar_bottom_round);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vLine.getLayoutParams();
                lp.topMargin = 0;
                lp.bottomMargin = margin;
                vLine.setLayoutParams(lp);
            } else {
                vLine.setBackgroundResource(R.drawable.timeline_bar_straight);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vLine.getLayoutParams();
                lp.topMargin = 0;
                lp.bottomMargin = 0;
                vLine.setLayoutParams(lp);
            }

            if (vLine.getBackground() != null) {
                ((GradientDrawable) vLine.getBackground().mutate()).setColor(color);
            }
        }

        private void bindTrainColumn(
                MarkerDataStop stop,
                LinearLayout container,
                TextView tvArrival, ImageView ivArrivalIcon,
                TextView tvAtStop,
                TextView tvDeparture, ImageView ivDepartureIcon,
                TextView tvDelay
        ) {
            if (stop == null) {
                container.setVisibility(View.GONE);
                return;
            }
            container.setVisibility(View.VISIBLE);

            boolean isDepStop = stop.isDepartureStop();
            LocalTime arrivalTime = stop.getArrivalTime();
            if (arrivalTime != null && !isDepStop) {
                tvArrival.setVisibility(View.VISIBLE);
                tvArrival.setText(arrivalTime.format(TIME_FORMATTER));
                tvArrival.setTextColor(stop.isOnLive() ? COLOR_GREEN : MaterialColors.getColor(tvDeparture, com.google.android.material.R.attr.colorOnSurface));
                if (stop.isOnLive()) {
                    ivArrivalIcon.setImageResource(R.drawable.icon_sensors);
                    ivArrivalIcon.setColorFilter(COLOR_GREEN);
                    ivArrivalIcon.setVisibility(View.VISIBLE);
                } else {
                    ivArrivalIcon.setVisibility(View.GONE);
                }
            } else {
                tvArrival.setVisibility(View.GONE);
                ivArrivalIcon.setVisibility(View.GONE);
            }

            if (isDepStop || stop.isDestinationStop()) {
                tvAtStop.setVisibility(View.GONE);
            } else {
                Long atStopMinutes = stop.getAtStopTime();
                if (atStopMinutes != null && atStopMinutes >= 0) {
                    tvAtStop.setVisibility(View.VISIBLE);
                    tvAtStop.setText(atStopMinutes + "min d'arrêt");
                    tvAtStop.setTextColor(Color.GRAY);
                } else {
                    tvAtStop.setText("—");
                    tvAtStop.setVisibility(View.VISIBLE);
                }
            }

            if (stop.isDestinationStop()) {
                tvDeparture.setVisibility(View.GONE);
                ivDepartureIcon.setVisibility(View.GONE);
            } else {
                LocalTime departureTime = stop.getDepartureTime();
                if (departureTime != null) {
                    tvDeparture.setVisibility(View.VISIBLE);
                    tvDeparture.setText(departureTime.format(TIME_FORMATTER));
                    tvDeparture.setTextColor(stop.isOnLive() ? COLOR_GREEN : MaterialColors.getColor(tvDeparture, com.google.android.material.R.attr.colorOnSurface));
                    if (stop.isOnLive()) {
                        ivDepartureIcon.setImageResource(R.drawable.icon_sensors);
                        ivDepartureIcon.setColorFilter(COLOR_GREEN);
                        ivDepartureIcon.setVisibility(View.VISIBLE);
                    } else {
                        ivDepartureIcon.setVisibility(View.GONE);
                    }
                } else {
                    tvDeparture.setVisibility(View.VISIBLE);
                    tvDeparture.setText("??:??");
                    ivDepartureIcon.setVisibility(View.GONE);
                }
            }

            if (stop.getDelay() == null || stop.getDelay() == 0) {
                tvDelay.setVisibility(View.GONE);
            } else {
                tvDelay.setVisibility(View.VISIBLE);
                tvDelay.setText(stop.getDelayText());
                tvDelay.setTextColor(stop.getDelayColor());
            }
        }

        private void bindPlatform(TextView tvPlatform, View spacerPlatform, String platformName) {
            if (platformName != null && !platformName.isEmpty()) {
                tvPlatform.setText(platformName);
                tvPlatform.setVisibility(View.VISIBLE);
                tvPlatform.setTextColor(Color.WHITE);
                spacerPlatform.setVisibility(View.VISIBLE);

                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setStroke(2, Color.WHITE);
                gd.setCornerRadius(10);
                tvPlatform.setBackground(gd);
            } else {
                tvPlatform.setVisibility(View.GONE);
                spacerPlatform.setVisibility(View.GONE);
            }
        }

        private void bindStopName(TextView tvStopName, String stopName, int iconRes) {
            SpannableStringBuilder builder = new SpannableStringBuilder(stopName != null ? stopName : "");
            if (iconRes != 0) {
                appendStopIcon(tvStopName, builder, iconRes);
            }
            tvStopName.setText(builder);
            tvStopName.setSelected(true);
        }

        private void appendStopIcon(TextView tvStopName, SpannableStringBuilder builder, int iconRes) {
            builder.append("  ");
            Drawable d = ContextCompat.getDrawable(context, iconRes);
            if (d != null) {
                d.mutate();
                d.setColorFilter(new PorterDuffColorFilter(
                        MaterialColors.getColor(tvStopName, com.google.android.material.R.attr.colorOnSurface),
                        PorterDuff.Mode.SRC_IN
                ));
                int size = (int) (tvStopName.getTextSize() * 1.2f);
                d.setBounds(0, 0, size, size);
                builder.setSpan(
                        new ImageSpan(d, ImageSpan.ALIGN_BOTTOM),
                        builder.length() - 1,
                        builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        private int getStopIconResource(MarkerDataStop stop) {
            if (stop.cantPickup()) return R.drawable.icon_logout;
            if (stop.cantDropoff()) return R.drawable.icon_login;
            return 0;
        }
    }
}