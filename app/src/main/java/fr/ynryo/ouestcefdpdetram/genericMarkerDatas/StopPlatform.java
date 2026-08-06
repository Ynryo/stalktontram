package fr.ynryo.ouestcefdpdetram.genericMarkerDatas;

public class StopPlatform {
    private final String platformName;
    private final String stopUIC;
    private final float percentage;

    public StopPlatform(String platformName) {
        this(platformName, null, 0);
    }

    public StopPlatform(String platformName, String stopUIC, float percentage) {
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
}
