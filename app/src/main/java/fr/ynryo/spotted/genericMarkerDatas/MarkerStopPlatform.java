package fr.ynryo.spotted.genericMarkerDatas;

import androidx.annotation.NonNull;

public class MarkerStopPlatform {
    private final String platformName;
    private final String stopUIC;
    private final float percentage;

    public MarkerStopPlatform(String platformName) {
        this(platformName, null, 0);
    }

    public MarkerStopPlatform(String platformName, String stopUIC, float percentage) {
        this.platformName = platformName;
        this.stopUIC = stopUIC;
        this.percentage = percentage;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getStopUIC() {
        return stopUIC;
    }

    public float getPercentage() {
        return percentage;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerStopPlatform{" +
                "platformName='" + platformName + '\'' +
                ", stopUIC='" + stopUIC + '\'' +
                ", percentage=" + percentage +
                '}';
    }
}
