package fr.ynryo.ouestcefdpdetram.managers;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import fr.ynryo.ouestcefdpdetram.MainActivity;
import fr.ynryo.ouestcefdpdetram.R;

/**
 * Manager dédié à la gestion des layouts, des WindowInsets et des paddings (carte, boutons, menu)
 *
 * @author Ynryo
 */
public class LayoutManager {
    private final MainActivity activity;

    public LayoutManager(MainActivity activity) {
        this.activity = activity;
    }

    public static int dpToPx(Context context, int dp) {
        if (context == null) return 0;
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public int dpToPx(int dp) {
        return dpToPx(activity, dp);
    }

    public void setupWindowInsets() {
        if (activity == null) return;

        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        View mainView = activity.findViewById(R.id.main);
        if (mainView == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            int margin16Dp = dpToPx(16);

            // 1. Top left menu FAB (btn_open_menu)
            View btnOpenMenu = activity.findViewById(R.id.btn_open_menu);
            if (btnOpenMenu != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btnOpenMenu.getLayoutParams();
                params.topMargin = systemBars.top + margin16Dp;
                btnOpenMenu.setLayoutParams(params);
            }

            // 2. Top right compass FAB container
            View compassNeedle = activity.findViewById(R.id.compass_needle);
            View compassContainer = (compassNeedle != null && compassNeedle.getParent() instanceof View) ? (View) compassNeedle.getParent() : null;
            if (compassContainer != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) compassContainer.getLayoutParams();
                params.topMargin = systemBars.top + margin16Dp;
                compassContainer.setLayoutParams(params);
            }

            // 3. Bottom right location FAB (fab_center_location)
            View fabCenterLocation = activity.findViewById(R.id.fab_center_location);
            if (fabCenterLocation != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fabCenterLocation.getLayoutParams();
                params.bottomMargin = systemBars.bottom + margin16Dp;
                fabCenterLocation.setLayoutParams(params);
            }

            // 4. Navigation View (Drawer content padding)
            View navigationView = activity.findViewById(R.id.navigation_view);
            if (navigationView != null) {
                navigationView.setPadding(navigationView.getPaddingLeft(), systemBars.top, navigationView.getPaddingRight(), systemBars.bottom);
            }

            return windowInsets;
        });
    }
}
