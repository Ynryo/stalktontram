package fr.ynryo.spotted.managers;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.ynryo.spotted.MainActivity;
import fr.ynryo.spotted.R;

/**
 * Classe gérant le suivi d'un marqueur en survol du véhicule sur la carte
 */
public class FollowManager {
    private final static String TAG = "FollowManager";
    private final MainActivity context;
    private boolean isFollowing;
    private String followedMarkerId;
    private FloatingActionButton followButton;

    public FollowManager(MainActivity context) {
        this.context = context;
        this.isFollowing = false;
    }

    public void toggleFollow(String followedMarkerId) {
        if (followedMarkerId == null) return;
        if (isFollowing(followedMarkerId)) {
            disableFollow(false);
        } else {
            disableFollow(true);
            enableFollow(followedMarkerId);
        }
    }

    public void enableFollow(String followedMarkerId) {
        if (!isFollowing && followedMarkerId != null) {
            isFollowing = true;
            this.followedMarkerId = followedMarkerId;
            centerOnFollowed();
            followButton.setImageResource(R.drawable.icon_distance_fill);
        }
    }

    public void disableFollow(boolean isGesture) {
        if (isFollowing) {
            isFollowing = false;
            if (followButton != null) {
                followButton.setImageResource(R.drawable.icon_distance);
            }

            if (!isGesture && context.getMap() != null) {
                context.getMap().animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(context.getMap().getCameraPosition().target)
                                .bearing(0)
                                .tilt(0)
                                .zoom(13f)
                                .build()
                ), 1000, null);
            }

            followedMarkerId = null;
        }
    }

    public boolean isFollowing(String markerId) {
        return isFollowing && followedMarkerId != null && followedMarkerId.equals(markerId);
    }

    public void centerOnFollowed() {
        if (followedMarkerId == null || context.getMap() == null) return;
        context.centerOnMarker(followedMarkerId, true, true);
    }
    
    public String getFollowedMarkerId() {
        return followedMarkerId;
    }

    public void setFollowButton(FloatingActionButton followButton, String followedMarkerId) {
        this.followButton = followButton;
        if (this.followButton == null) return;
        if (isFollowing(followedMarkerId)) this.followButton.setImageResource(R.drawable.icon_distance_fill);
        this.followButton.setOnClickListener(view -> toggleFollow(followedMarkerId));
    }
}
