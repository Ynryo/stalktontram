package fr.ynryo.spotted.apiResponsesPOJO.guessPlatform;

import androidx.annotation.NonNull;

public class CartoTchooGuessPlatform {
    private String platform;
    private int count;
    private float percentage;

    public String getPlatform() {
        return platform;
    }

    public int getCount() {
        return count;
    }

    public float getPercentage() {
        return percentage;
    }

    @NonNull
    @Override
    public String toString() {
        return "CartoTchooGuessPlatform{" +
                "platform='" + platform + '\'' +
                ", count=" + count +
                ", percentage=" + percentage +
                '}';
    }
}
