package fr.ynryo.stalktonbus;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.MaterialColors;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.stalktonbus.apiResponsesPOJO.guessPlatform.CartoTchooGuessPlatform;
import fr.ynryo.stalktonbus.apiResponsesPOJO.network.BusTrackerNetworkData;
import fr.ynryo.stalktonbus.artists.MarkerArtist;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStop;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerStopPlatform;
import fr.ynryo.stalktonbus.managers.FetchingManager;
import fr.ynryo.stalktonbus.utils.Time;

public class MarkerStopsDetailActivity {
    private static final String TAG = "MarkerStopsDetailActivity";
    private static final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private final MainActivity context;
    private View bottomSheetView;
    private BottomSheetBehavior<View> behavior;
    private String vehicleId;

    public MarkerStopsDetailActivity(MainActivity context) {
        WeakReference<MainActivity> contextRef = new WeakReference<>(context);
        this.context = contextRef.get();
        initBottomSheet();
    }

    private void initBottomSheet() {
        if (this.context == null) return;
        bottomSheetView = context.findViewById(R.id.vehicle_bottom_sheet);
        if (bottomSheetView != null) {
            behavior = BottomSheetBehavior.from(bottomSheetView);
            behavior.setHideable(true);
            behavior.setFitToContents(false);
            behavior.setExpandedOffset(context.dpToPx(84));
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            behavior.setPeekHeight(calculatePeekHeight());

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        vehicleId = null;
                        if (context != null && context.getMarkerArtist() != null && context.getMarkerArtist().getRouteArtist() != null) {
                            context.getMarkerArtist().getRouteArtist().remove();
                        }
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });

            View closeBtn = bottomSheetView.findViewById(R.id.closeButton);
            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> close());
            }

            ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                View nsvContent = bottomSheetView.findViewById(R.id.nsvContent);
                if (nsvContent != null) {
                    nsvContent.setPadding(nsvContent.getPaddingLeft(), nsvContent.getPaddingTop(), nsvContent.getPaddingRight(), insets.bottom + context.dpToPx(16));
                    if (nsvContent instanceof NestedScrollView) {
                        ((NestedScrollView) nsvContent).setClipToPadding(false);
                    }
                }
                return windowInsets;
            });
        }
    }

    public void open(MarkerStandardized markerStandardized) {
        if (this.context == null) return;
        if (bottomSheetView == null || behavior == null) initBottomSheet();
        if (bottomSheetView == null || behavior == null) return;

        vehicleId = markerStandardized.getId();

        setupLineHeader(bottomSheetView, markerStandardized);
        setupLoader(bottomSheetView, markerStandardized);

        behavior.setPeekHeight(calculatePeekHeight());
        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

        fetchVehicleData(markerStandardized, bottomSheetView);
    }

    public void close() {
        if (behavior != null) {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        vehicleId = null;
        
        MarkerArtist markerArtist = context.getMarkerArtist();
        if (markerArtist != null && markerArtist.getRouteArtist() != null) {
            markerArtist.getRouteArtist().remove();
        }
    }

    private int calculatePeekHeight() {
        if (context == null) return 400;
        return context.dpToPx(130);
    }

    private void setupLineHeader(View view, MarkerStandardized markerStandardized) {
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);

        String lineNumber = markerStandardized.getLineNumber();
        tvLigne.setText(lineNumber);

        int fillColor = Color.parseColor(markerStandardized.getFillColor() != null ? markerStandardized.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerStandardized.getTextColor() != null ? markerStandardized.getTextColor() : "#FFFFFF");

        tvLigne.setBackgroundColor(fillColor);
        tvLigne.setTextColor(textColor);
    }

    private void setupLoader(View view, MarkerStandardized markerStandardized) {
        ProgressBar loader = view.findViewById(R.id.loader);
        int fillColor = Color.parseColor(markerStandardized.getFillColor() != null ? markerStandardized.getFillColor() : "#424242");

        loader.setVisibility(View.VISIBLE);
        loader.setIndeterminateTintList(ColorStateList.valueOf(fillColor));

        view.findViewById(R.id.llStopsContent).setVisibility(View.INVISIBLE);
    }

    // ==================== DATA FETCHING ====================

    /**
     * Fetch data from API
     *
     * @param markerStandardized the marker data
     * @param view               the view
     */
    private void fetchVehicleData(MarkerStandardized markerStandardized, View view) {
        context.getFetcher().fetchVehicleStopsInfo(markerStandardized, new FetchingManager.OnVehicleDetailsListener() {
            @Override
            public void onResponseVehicleDetailsListener(MarkerStandardized markerStandardized) {
                hideLoader(view);

                if (context.getMarkerArtist() != null) {
                    context.getMarkerArtist().getRouteArtist().drawVehicleRoute(markerStandardized);
                }

                showVehicleDetails(markerStandardized, view);
                fetchNetworkLogo(markerStandardized, view);
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
     *
     * @param markerStandardized the marker data
     * @param view               the view
     */
    private void fetchNetworkLogo(MarkerStandardized markerStandardized, View view) {
        if (markerStandardized.getNetworkId() == 0) return;

        context.getFetcher().fetchNetworkData(markerStandardized.getNetworkId(), new FetchingManager.OnNetworkDataListener() {
            @Override
            public void onResponseNetworkDataListener(BusTrackerNetworkData nData) {
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
     *
     * @param view   the view
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

        Glide.with(context).as(PictureDrawable.class).load(imgURI.toString()).diskCacheStrategy(DiskCacheStrategy.DATA).override(100, 100).into(ivLogo);
    }

    /**
     * Hide loader from view
     *
     * @param view the view
     */
    private void hideLoader(View view) {
        view.findViewById(R.id.loader).setVisibility(View.GONE);
    }

    /**
     * Show error from view
     *
     * @param view the view
     */
    private void showError(View view) {
        TextView tvDest = view.findViewById(R.id.tvDestination);
        tvDest.setText(R.string.network_error);
    }

    // ==================== DISPLAY ====================

    /**
     * Show vehicle details from marker data
     *
     * @param markerStandardized the marker data
     * @param view               the view
     */
    private void showVehicleDetails(MarkerStandardized markerStandardized, View view) {
        context.getFollowManager().setFollowButton(view.findViewById(R.id.followButton), markerStandardized.getId());
        context.getFavoriteManager().setFavoriteButton(view.findViewById(R.id.favoriteButton), markerStandardized);

        setupDestinationText(view, markerStandardized);
        StopsAdapter adapter = setupStopsList(view, markerStandardized);
        fetchGuessPlatforms(markerStandardized, adapter);
    }

    /**
     * Setup destination text from marker data
     *
     * @param view               the view
     * @param markerStandardized the marker data
     */
    private void setupDestinationText(View view, MarkerStandardized markerStandardized) {
        TextView tvDestination = view.findViewById(R.id.tvDestination);
        tvDestination.setText(markerStandardized.getDestination());
        tvDestination.setSingleLine(true);
        tvDestination.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvDestination.setMarqueeRepeatLimit(-1);
        tvDestination.setHorizontallyScrolling(true);
        tvDestination.setSelected(true);
    }

    private StopsAdapter setupStopsList(View view, MarkerStandardized markerStandardized) {
        RecyclerView rvStops = view.findViewById(R.id.rvStops);
        rvStops.setLayoutManager(new LinearLayoutManager(context));

//        if (markerStandardized.isUm()) {
//            List<TrainUmTimelineRow> rows = TrainUmAssembler.assembleUmStops(markerStandardized);
//            rvStops.setAdapter(new UmStopsAdapter(markerStandardized, rows));
//        } else {
        List<MarkerStop> stops = markerStandardized.getStops() != null ? markerStandardized.getStops() : new ArrayList<>();
        StopsAdapter adapter = new StopsAdapter(stops);
        rvStops.setAdapter(adapter);
//        }

        view.findViewById(R.id.llStopsContent).setVisibility(View.VISIBLE);
        return adapter;
    }

    private void fetchGuessPlatforms(MarkerStandardized markerStandardized, StopsAdapter adapter) {
        if (!markerStandardized.isTrain()) return;

        String trainNum = markerStandardized.getLineNumber();
        if (trainNum == null || trainNum.isEmpty()) return;

        List<MarkerStop> stops = markerStandardized.getStops();
        for (int i = 0; i < stops.size(); i++) {
            MarkerStop stop = stops.get(i);
            // Si l'arrêt a déjà un quai officiel renseigné (100%), on ne fait pas de requête CartoTchoo
            if (stop.getPlatform() != null && stop.getPlatform().getPlatformName() != null && !stop.getPlatform().getPlatformName().isEmpty()) {
                continue;
            }
            final int position = i;
            final String uic = stop.getStopRef();
            if (uic == null || uic.isEmpty()) continue;

            Log.d(TAG, "Fetching guess platform: uic=" + uic + ", trainNum=" + trainNum);
            context.getFetcher().fetchGuestPlatform(uic, trainNum, new FetchingManager.OnGuessPlatformListener() {
                @Override
                public void onResponseGuessPlatformListener(List<CartoTchooGuessPlatform> cartoTchooGuessPlatform) {
                    Log.d(TAG, "Réponse guess platform pour UIC " + uic + ": " + cartoTchooGuessPlatform);
                    if (cartoTchooGuessPlatform != null && !cartoTchooGuessPlatform.isEmpty()) {
                        markerStandardized.setGuessStopPlatform(uic, cartoTchooGuessPlatform);
                        if (adapter != null) {
                            adapter.notifyItemChanged(position);
                        }
                    }
                }

                @Override
                public void onErrorGuessPlatformListener(String error) {
                    Log.d(TAG, "Pas d'estimation de quai pour UIC " + uic + " : " + error);
                }
            });
        }
    }

    public String getCurrentVehicleId() {
        return vehicleId;
    }

    private static int getTimelineLayout(MarkerStop stop, int position, int itemCount) {
        boolean isFirstStop = stop.isDepartureStop();
        boolean isLastStop = position == itemCount - 1 || stop.isDestinationStop();
        if (isFirstStop) {
            return R.layout.timeline_first_stop;
        } else if (isLastStop) {
            return R.layout.timeline_last_stop;
        } else {
            return R.layout.timeline_intermediate_stop;
        }
    }

    // ==================== ADAPTER ====================

    private static class StopViewHolder extends RecyclerView.ViewHolder {
        final View sllPlatformContainer;
        final TextView tvPlatform, tvPlatformLabel, tvStopName, tvDepartureTime, tvAtStopTime, tvArrivingTime, tvDelay;
        final ImageView ivArrivingTimeIcon, ivDepartureTimeIcon;
        final FrameLayout flTimeline;

        StopViewHolder(View itemView) {
            super(itemView);
            sllPlatformContainer = itemView.findViewById(R.id.llPlatformContainer);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
            tvPlatformLabel = itemView.findViewById(R.id.tvPlatformLabel);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvAtStopTime = itemView.findViewById(R.id.tvAtStopTime);
            tvArrivingTime = itemView.findViewById(R.id.tvArrivingTime);
            tvDelay = itemView.findViewById(R.id.tvDelay);
            ivArrivingTimeIcon = itemView.findViewById(R.id.ivArrivingTimeIcon);
            ivDepartureTimeIcon = itemView.findViewById(R.id.ivDepartureTimeIcon);
            flTimeline = itemView.findViewById(R.id.flTimeline);
        }
    }

    /**
     * Stops adapter for recycler view
     */
    private class StopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_STOP = 0;
        private static final int TYPE_EMPTY = 1;

        private final List<MarkerStop> stops;

        /**
         * Constructor
         *
         * @param stops the list of stops
         */
        StopsAdapter(List<MarkerStop> stops) {
            this.stops = stops;
        }

        /**
         * Get item view type
         *
         * @param position position to query
         * @return the item view type
         */
        @Override
        public int getItemViewType(int position) {
            return stops.isEmpty() ? TYPE_EMPTY : TYPE_STOP;
        }

        /**
         * Create a view holder
         *
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
         *
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

            MarkerStop stop = stops.get(position);
            bindStopViewHolder((StopViewHolder) holder, stop, position, stops.size());
        }

        /**
         * Get item count
         *
         * @return the item count
         */
        @Override
        public int getItemCount() {
            return stops.isEmpty() ? 1 : stops.size();
        }

        // ========== NO DATA ==========

        /**
         * Create empty view holder
         *
         * @param parent The ViewGroup into which the new View will be added after it is bound to
         *               an adapter position.
         * @return A new ViewHolder that holds a View of the given view type.
         */
        private RecyclerView.ViewHolder createEmptyViewHolder(ViewGroup parent) {
            TextView tvEmpty = new TextView(parent.getContext());
            tvEmpty.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvEmpty.setPadding(0, 32, 0, 32);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            return new RecyclerView.ViewHolder(tvEmpty) {
            };
        }

        /**
         * Bind empty view holder
         *
         * @param holder The ViewHolder which should be updated to represent the contents of the
         *               item at the given position in the data set.
         */
        private void bindEmptyViewHolder(RecyclerView.ViewHolder holder) {
            TextView tvEmpty = (TextView) holder.itemView;
            tvEmpty.setText(R.string.no_data);
            tvEmpty.setTextColor(MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurface));
        }

        // ========== STOP ITEM ==========
        private RecyclerView.ViewHolder createStopViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_stop_details, parent, false);
            return new StopViewHolder(view);
        } //inflate item stop

        private void bindStopViewHolder(StopViewHolder vh, MarkerStop stop, int position, int itemCount) { //distribute data
            bindTimeline(vh, stop, position, itemCount);
            bindPlatform(vh, stop);
            bindStopName(vh, stop);
            bindArrivalTime(vh, stop);
            bindAtStopTime(vh, stop);
            bindDepartureTime(vh, stop);
            bindDelay(vh, stop);
        }

        public void bindTimeline(StopViewHolder vh, MarkerStop stop, int position, int itemCount) {
            MarkerStandardized vehicle = stop.getVehicle();

            vh.flTimeline.setVisibility(View.VISIBLE);
            vh.flTimeline.removeAllViews();

            // Inflate le layout dedans
            View timelineView = LayoutInflater.from(context).inflate(getTimelineLayout(stop, position, itemCount), vh.flTimeline, true);

            // Tinte la barre avec la couleur du train
            int fillColor = Color.parseColor(vehicle.getFillColor() != null ? vehicle.getFillColor() : "#424242");

            View lineView = timelineView.findViewById(R.id.vLineBottom);
            if (lineView == null) lineView = timelineView.findViewById(R.id.vLineTop);
            if (lineView == null) lineView = timelineView.findViewById(R.id.vLineFull);
            if (lineView != null)
                ((GradientDrawable) lineView.getBackground().mutate()).setColor(fillColor);
        }

        private void bindPlatform(StopViewHolder vh, MarkerStop stop) {
            MarkerStopPlatform platform = stop.getPlatform();
            if (platform != null && platform.getPlatformName() != null) {
                vh.tvPlatform.setText(platform.getPlatformName());
                vh.sllPlatformContainer.setVisibility(View.VISIBLE);

                GradientDrawable gd = (GradientDrawable) vh.sllPlatformContainer.getBackground().mutate();
                boolean isPlatformGuessed = platform.getPercentage() != 100;
                if (isPlatformGuessed) {
                    vh.tvPlatform.setTextColor(Color.GRAY);
                    vh.tvPlatformLabel.setTextColor(Color.GRAY);
                    gd.setStroke(2, Color.GRAY, 8, 8);
                }
            } else {
                vh.sllPlatformContainer.setVisibility(View.GONE);
            }
        }

        private void bindStopName(StopViewHolder vh, MarkerStop stop) {
            SpannableStringBuilder builder = new SpannableStringBuilder(stop.getStopName());

            int iconRes = getStopIconResource(stop);
            if (iconRes != 0) {
                appendStopIcon(vh, builder, iconRes);
            }

            vh.tvStopName.setText(builder);
            vh.tvStopName.setSelected(true);
        }

        private int getStopIconResource(MarkerStop stop) {
            if (stop.cantPickup()) return R.drawable.icon_logout;
            if (stop.cantDropoff()) return R.drawable.icon_login;
            return 0;
        }

        private void appendStopIcon(StopViewHolder vh, SpannableStringBuilder builder, int iconRes) {
            builder.append("  ");
            Drawable d = ContextCompat.getDrawable(context, iconRes);
            if (d != null) {
                d.mutate();
                d.setColorFilter(new PorterDuffColorFilter(MaterialColors.getColor(vh.tvStopName, com.google.android.material.R.attr.colorOnSurface), PorterDuff.Mode.SRC_IN));
                int size = (int) (vh.tvStopName.getTextSize() * 1.2f);
                d.setBounds(0, 0, size, size);
                builder.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        private void bindArrivalTime(StopViewHolder vh, MarkerStop stop) {
            MarkerStandardized vehicle = stop.getVehicle();
            if (stop.isDepartureStop()) {
                vh.tvArrivingTime.setVisibility(View.GONE);
                return;
            }

            Time arrivalTime = stop.getArrivalTime() != null ? stop.getArrivalTime() : (stop.isDestinationStop() ? stop.getDepartureTime() : null);
            if (arrivalTime != null && (vehicle.isTrain() || stop.isDestinationStop())) {
                vh.tvArrivingTime.setVisibility(View.VISIBLE);
                vh.tvArrivingTime.setText(Time.formatHHmm(arrivalTime));
                vh.tvArrivingTime.setTextColor(stop.isOnLive() ? COLOR_GREEN : getDefaultTextColor(vh));
                bindOnLive(vh.ivArrivingTimeIcon, stop);
            } else {
                vh.tvArrivingTime.setVisibility(View.GONE);
            }
        }

        private void bindAtStopTime(StopViewHolder vh, MarkerStop stop) {
            MarkerStandardized vehicle = stop.getVehicle();
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
                vh.tvAtStopTime.setVisibility(View.GONE);
            }
        }

        private void bindDepartureTime(StopViewHolder vh, MarkerStop stop) {
            if (stop.isDestinationStop()) {
                vh.tvDepartureTime.setVisibility(View.GONE);
                return;
            }

            Time departureTime = stop.getDepartureTime();
            if (departureTime != null) {
                vh.tvDepartureTime.setVisibility(View.VISIBLE);
                vh.tvDepartureTime.setText(Time.formatHHmm(departureTime));
                vh.tvDepartureTime.setTextColor(stop.isOnLive() ? COLOR_GREEN : getDefaultTextColor(vh));
                bindOnLive(vh.ivDepartureTimeIcon, stop);
            } else {
                vh.tvDepartureTime.setText("??:??");
            }
        }

        private void bindOnLive(ImageView ivTimeIcon, MarkerStop stop) {
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

        private void bindDelay(StopViewHolder vh, MarkerStop stop) {
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

//    private class UmStopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//        private static final int TYPE_STOP = 0;
//        private static final int TYPE_MERGE = 1;
//        private static final int TYPE_SPLIT = 2;
//
//        private final MarkerStandardized umMarker;
//        private final List<TrainUmTimelineRow> rows;
//
//        UmStopsAdapter(MarkerStandardized umMarker, List<TrainUmTimelineRow> rows) {
//            this.umMarker = umMarker;
//            this.rows = rows;
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            TrainUmTimelineRow row = rows.get(position);
//            if (row.getType() == TimelineRowType.MERGE_GRAPHIC) {
//                return TYPE_MERGE;
//            } else if (row.getType() == TimelineRowType.SPLIT_GRAPHIC) {
//                return TYPE_SPLIT;
//            } else {
//                return TYPE_STOP;
//            }
//        }
//
//        @NonNull
//        @Override
//        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            if (viewType == TYPE_MERGE) {
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_um_merge, parent, false);
//                return new GraphicViewHolder(view);
//            } else if (viewType == TYPE_SPLIT) {
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_um_split, parent, false);
//                return new GraphicViewHolder(view);
//            } else {
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_stop_details, parent, false);
//                return new UmStopViewHolder(view);
//            }
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//            TrainUmTimelineRow row = rows.get(position);
//            int viewType = getItemViewType(position);
//
//            MarkerStandardized trainA = umMarker.getUmA();
//            MarkerStandardized trainB = umMarker.getUmB();
//            int colorA = Color.parseColor(trainA.getFillColor() != null ? trainA.getFillColor() : "#424242");
//            int colorB = Color.parseColor(trainB.getFillColor() != null ? trainB.getFillColor() : "#424242");
//
//            if (viewType == TYPE_MERGE || viewType == TYPE_SPLIT) {
//                tintGraphicView(holder.itemView, colorA, colorB);
//            } else {
//                UmStopViewHolder vh = (UmStopViewHolder) holder;
//                bindStopRow(vh, row, colorA, colorB);
//            }
//        }
//
//        @Override
//        public int getItemCount() {
//            return rows.size();
//        }
//
//        private void tintGraphicView(View itemView, int colorA, int colorB) {
//            ImageView lineMain = itemView.findViewById(R.id.line_main);
//            ImageView curveTop = itemView.findViewById(R.id.curve_top);
//            View lineHorizontal = itemView.findViewById(R.id.line_horizontal);
//            ImageView dirArrowUp = itemView.findViewById(R.id.direction_arrow_up);
//            ImageView dirArrow = itemView.findViewById(R.id.direction_arrow);
//            ImageView curveBottom = itemView.findViewById(R.id.curve_bottom);
//            ImageView lineSecondaryDown = itemView.findViewById(R.id.line_secondary_down);
//
//            if (lineMain != null) lineMain.setColorFilter(colorA, PorterDuff.Mode.SRC_IN);
//            if (dirArrow != null) dirArrow.setColorFilter(colorA, PorterDuff.Mode.SRC_IN);
//
//            if (curveTop != null) curveTop.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
//            if (lineHorizontal != null) {
//                if (lineHorizontal instanceof ImageView) {
//                    ((ImageView) lineHorizontal).setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
//                } else if (lineHorizontal.getBackground() != null) {
//                    ((GradientDrawable) lineHorizontal.getBackground().mutate()).setColor(colorB);
//                }
//            }
//            if (dirArrowUp != null) dirArrowUp.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
//            if (curveBottom != null) curveBottom.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
//            if (lineSecondaryDown != null)
//                lineSecondaryDown.setColorFilter(colorB, PorterDuff.Mode.SRC_IN);
//        }
//
//        private void bindStopRow(UmStopViewHolder vh, TrainUmTimelineRow row, int colorA, int colorB) {
//            MarkerStop stopA = row.getStopA();
//            MarkerStop stopB = row.getStopB();
//
//            // 1. Bind Timeline
//            bindTimeline(vh, row);
//
//            // 2. Determine Stop Name & Icon
//            String stopName = "";
//            int iconRes = 0;
//            String nameA = stopA != null ? stopA.getStopName() : null;
//            String nameB = stopB != null ? stopB.getStopName() : null;
//
//            if (nameA != null && nameB != null) {
//                if (nameA.equals(nameB)) {
//                    stopName = nameA;
//                } else {
//                    stopName = nameA + " / " + nameB;
//                }
//            } else if (nameA != null) {
//                stopName = nameA;
//            } else if (nameB != null) {
//                stopName = nameB;
//            }
//
//            if (stopA != null) {
//                iconRes = getStopIconResource(stopA);
//            }
//            if (iconRes == 0 && stopB != null) {
//                iconRes = getStopIconResource(stopB);
//            }
//            bindStopName(vh.tvStopName, stopName, iconRes);
//
//            // 3. Determine Platform
//            MarkerStopPlatform platform = null;
//            if (stopA != null && stopB != null) {
//                MarkerStopPlatform platA = stopA.getPlatform();
//                MarkerStopPlatform platB = stopB.getPlatform();
//                if (platA != null && platB != null) {
//                    if (platA.equals(platB)) {
//                        platform = platA;
//                    } else {
//                        platform = new MarkerStopPlatform(platA + "/" + platB);
//                    }
//                } else if (platA != null) {
//                    platform = platA;
//                } else if (platB != null) {
//                    platform = platB;
//                }
//            } else if (stopA != null) {
//                platform = stopA.getPlatform();
//            } else if (stopB != null) {
//                platform = stopB.getPlatform();
//            }
//            bindPlatform(vh.tvPlatform, vh.spacerPlatform, platform);
//
//            // 4. Bind Columns
//            if (row.getType() == TimelineRowType.COMMON) {
//                bindTrainColumn(stopA, (LinearLayout) vh.llTrainAData, vh.tvArrivingTime, vh.ivArrivingTimeIcon, vh.tvAtStopTime, vh.tvDepartureTime, vh.ivDepartureTimeIcon, vh.tvDelay);
//                vh.llTrainBData.setVisibility(View.GONE);
//                vh.vSplitSeparator.setVisibility(View.GONE);
//            } else {
//                // SIDE_BY_SIDE stop
//                bindTrainColumn(stopA, (LinearLayout) vh.llTrainAData, vh.tvArrivingTime, vh.ivArrivingTimeIcon, vh.tvAtStopTime, vh.tvDepartureTime, vh.ivDepartureTimeIcon, vh.tvDelay);
//                bindTrainColumn(stopB, (LinearLayout) vh.llTrainBData, vh.tvArrivingTimeB, vh.ivArrivingTimeIconB, vh.tvAtStopTimeB, vh.tvDepartureTimeB, vh.ivDepartureTimeIconB, vh.tvDelayB);
//
//                boolean showSep = (stopA != null && stopB != null);
//                vh.vSplitSeparator.setVisibility(showSep ? View.VISIBLE : View.GONE);
//            }
//        }
//
//        private void bindTimeline(UmStopViewHolder vh, TrainUmTimelineRow row) {
//            vh.flTimeline.setVisibility(View.VISIBLE);
//            vh.flTimeline.removeAllViews();
//
//            View timelineView = LayoutInflater.from(context).inflate(R.layout.timeline_um_stop, vh.flTimeline, true);
//
//            View vLineLeft = timelineView.findViewById(R.id.vLineLeft);
//            View vStopDotLeft = timelineView.findViewById(R.id.vStopDotLeft);
//            View vLineRight = timelineView.findViewById(R.id.vLineRight);
//            View vStopDotRight = timelineView.findViewById(R.id.vStopDotRight);
//
//            MarkerStandardized trainA = umMarker.getUmA();
//            MarkerStandardized trainB = umMarker.getUmB();
//
//            int colorA = Color.parseColor(trainA.getFillColor() != null ? trainA.getFillColor() : "#424242");
//            int colorB = Color.parseColor(trainB.getFillColor() != null ? trainB.getFillColor() : "#424242");
//
//            boolean isFirst = row.isFirstPosition();
//            boolean isLast = row.isLastPosition();
//            int margin = (int) (8 * context.getResources().getDisplayMetrics().density);
//
//            configureLine(vLineLeft, isFirst, isLast, margin, colorA);
//            configureLine(vLineRight, isFirst, isLast, margin, colorB);
//
//            if (row.getType() == TimelineRowType.COMMON) {
//                vLineRight.setVisibility(View.INVISIBLE);
//                vStopDotRight.setVisibility(View.INVISIBLE);
//                vStopDotLeft.setVisibility(View.VISIBLE);
//            } else {
//                vLineRight.setVisibility(View.VISIBLE);
//                vStopDotLeft.setVisibility(row.getStopA() != null ? View.VISIBLE : View.GONE);
//                vStopDotRight.setVisibility(row.getStopB() != null ? View.VISIBLE : View.GONE);
//            }
//        }
//
//        private void configureLine(View vLine, boolean isFirst, boolean isLast, int margin, int color) {
//            if (isFirst) {
//                vLine.setBackgroundResource(R.drawable.timeline_bar_top_round);
//                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vLine.getLayoutParams();
//                lp.topMargin = margin;
//                lp.bottomMargin = 0;
//                vLine.setLayoutParams(lp);
//            } else if (isLast) {
//                vLine.setBackgroundResource(R.drawable.timeline_bar_bottom_round);
//                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vLine.getLayoutParams();
//                lp.topMargin = 0;
//                lp.bottomMargin = margin;
//                vLine.setLayoutParams(lp);
//            } else {
//                vLine.setBackgroundResource(R.drawable.timeline_bar_straight);
//                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vLine.getLayoutParams();
//                lp.topMargin = 0;
//                lp.bottomMargin = 0;
//                vLine.setLayoutParams(lp);
//            }
//
//            if (vLine.getBackground() != null) {
//                ((GradientDrawable) vLine.getBackground().mutate()).setColor(color);
//            }
//        }
//
//        private void bindTrainColumn(MarkerStop stop, LinearLayout container, TextView tvArrival, ImageView ivArrivalIcon, TextView tvAtStop, TextView tvDeparture, ImageView ivDepartureIcon, TextView tvDelay) {
//            if (stop == null) {
//                container.setVisibility(View.GONE);
//                return;
//            }
//            container.setVisibility(View.VISIBLE);
//
//            boolean isDepStop = stop.isDepartureStop();
//            Time arrivalTime = stop.getArrivalTime() != null ? stop.getArrivalTime() : (stop.isDestinationStop() ? stop.getDepartureTime() : null);
//            if (arrivalTime != null && !isDepStop) {
//                tvArrival.setVisibility(View.VISIBLE);
//                tvArrival.setText(Time.formatHHmm(arrivalTime));
//                tvArrival.setTextColor(stop.isOnLive() ? COLOR_GREEN : MaterialColors.getColor(tvDeparture, com.google.android.material.R.attr.colorOnSurface));
//                if (stop.isOnLive()) {
//                    ivArrivalIcon.setImageResource(R.drawable.icon_sensors);
//                    ivArrivalIcon.setColorFilter(COLOR_GREEN);
//                    ivArrivalIcon.setVisibility(View.VISIBLE);
//                } else {
//                    ivArrivalIcon.setVisibility(View.GONE);
//                }
//            } else {
//                tvArrival.setVisibility(View.GONE);
//                ivArrivalIcon.setVisibility(View.GONE);
//            }
//
//            if (isDepStop || stop.isDestinationStop()) {
//                tvAtStop.setVisibility(View.GONE);
//            } else {
//                Long atStopMinutes = stop.getAtStopTime();
//                if (atStopMinutes != null && atStopMinutes >= 0) {
//                    tvAtStop.setVisibility(View.VISIBLE);
//                    tvAtStop.setText(atStopMinutes + "min d'arrêt");
//                    tvAtStop.setTextColor(Color.GRAY);
//                } else {
//                    tvAtStop.setVisibility(View.GONE);
//                }
//            }
//
//            if (stop.isDestinationStop()) {
//                tvDeparture.setVisibility(View.GONE);
//                ivDepartureIcon.setVisibility(View.GONE);
//            } else {
//                Time departureTime = stop.getDepartureTime();
//                if (departureTime != null) {
//                    tvDeparture.setVisibility(View.VISIBLE);
//                    tvDeparture.setText(Time.formatHHmm(departureTime));
//                    tvDeparture.setTextColor(stop.isOnLive() ? COLOR_GREEN : MaterialColors.getColor(tvDeparture, com.google.android.material.R.attr.colorOnSurface));
//                    if (stop.isOnLive()) {
//                        ivDepartureIcon.setImageResource(R.drawable.icon_sensors);
//                        ivDepartureIcon.setColorFilter(COLOR_GREEN);
//                        ivDepartureIcon.setVisibility(View.VISIBLE);
//                    } else {
//                        ivDepartureIcon.setVisibility(View.GONE);
//                    }
//                } else {
//                    tvDeparture.setVisibility(View.VISIBLE);
//                    tvDeparture.setText("??:??");
//                    ivDepartureIcon.setVisibility(View.GONE);
//                }
//            }
//
//            if (stop.getDelay() == null || stop.getDelay() == 0) {
//                tvDelay.setVisibility(View.GONE);
//            } else {
//                tvDelay.setVisibility(View.VISIBLE);
//                tvDelay.setText(stop.getDelayText());
//                tvDelay.setTextColor(stop.getDelayColor());
//            }
//        }
//
//        private void bindPlatform(TextView tvPlatform, View spacerPlatform, MarkerStopPlatform platform) {
//            if (platform != null) {
//                tvPlatform.setText(platform.getPlatformName());
//                tvPlatform.setVisibility(View.VISIBLE);
//                tvPlatform.setTextColor(Color.WHITE);
//                spacerPlatform.setVisibility(View.VISIBLE);
//
//                GradientDrawable gd = new GradientDrawable();
//                gd.setStroke(2, Color.WHITE, 8, 8);
//                gd.setShape(GradientDrawable.RECTANGLE);
//                gd.setCornerRadius(10);
//                tvPlatform.setBackground(gd);
//            } else {
//                tvPlatform.setVisibility(View.GONE);
//                spacerPlatform.setVisibility(View.GONE);
//            }
//        }
//
//        private void bindStopName(TextView tvStopName, String stopName, int iconRes) {
//            SpannableStringBuilder builder = new SpannableStringBuilder(stopName != null ? stopName : "");
//            if (iconRes != 0) {
//                appendStopIcon(tvStopName, builder, iconRes);
//            }
//            tvStopName.setText(builder);
//            tvStopName.setSelected(true);
//        }
//
//        private void appendStopIcon(TextView tvStopName, SpannableStringBuilder builder, int iconRes) {
//            builder.append("  ");
//            Drawable d = ContextCompat.getDrawable(context, iconRes);
//            if (d != null) {
//                d.mutate();
//                d.setColorFilter(new PorterDuffColorFilter(MaterialColors.getColor(tvStopName, com.google.android.material.R.attr.colorOnSurface), PorterDuff.Mode.SRC_IN));
//                int size = (int) (tvStopName.getTextSize() * 1.2f);
//                d.setBounds(0, 0, size, size);
//                builder.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            }
//        }
//
//        private int getStopIconResource(MarkerStop stop) {
//            if (stop.cantPickup()) return R.drawable.icon_logout;
//            if (stop.cantDropoff()) return R.drawable.icon_login;
//            return 0;
//        }
//    }
}