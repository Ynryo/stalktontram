package fr.ynryo.stalktonbus.genericMarkerDatas;

import androidx.annotation.NonNull;

public class MarkerStyle {
    private String fillColor; // Couleur de remplissage du marqueur
    private String textColor; // Couleur du texte (numéro de ligne)

    public MarkerStyle() {
        this.textColor = "#FFFFFF";
        this.fillColor = "#424242";
    }

    public MarkerStyle(String textColor, String fillColor) {
        this.textColor = textColor;
        this.fillColor = fillColor;
    }

    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerStyle{" +
                "textColor=" + textColor +
                ", fillColor=" + fillColor +
                '}';
    }
}
