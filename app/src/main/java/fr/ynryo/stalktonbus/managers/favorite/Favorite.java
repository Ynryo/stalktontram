package fr.ynryo.stalktonbus.managers.favorite;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Favorite {
    @SerializedName("id")
    private final int ligneId;
    @SerializedName("lineText")
    private String lineText;
    @SerializedName("destination")
    private String destination;
    @SerializedName("fillColor")
    private String fillColor;
    @SerializedName("textColor")
    private String textColor;
    @SerializedName("city")
    private String city;

    public Favorite() {
        this.ligneId = 0;
        this.lineText = "";
        this.destination = "";
    }

    public Favorite(int ligneId) {
        this.ligneId = ligneId;
    }

    public Favorite(int ligneId, String lineText, String destination) {
        this(ligneId);
        this.lineText = lineText;
        this.destination = destination;
    }

    public Favorite(int ligneId, String lineText, String destination, String fillColor, String textColor, String city) {
        this(ligneId, lineText, destination);
        this.fillColor = fillColor;
        this.textColor = textColor;
        this.city = city;
    }

    public int getLigneId() {
        return ligneId;
    }

    public String getLineText() {
        return lineText;
    }

    public String getDestination() {
        return destination;
    }

    public String getFillColor() {
        return fillColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public String getCity() {
        return city;
    }

    @NonNull
    @Override
    public String toString() {
        return "Favorite{" +
                "ligneId=" + ligneId +
                ", lineText='" + lineText + '\'' +
                ", destination='" + destination + '\'' +
                ", fillColor='" + fillColor + '\'' +
                ", textColor='" + textColor + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
