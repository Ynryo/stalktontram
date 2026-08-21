package fr.ynryo.spotted.apiResponsesPOJO.version;

import androidx.annotation.NonNull;

public class YnryoVersionData {
    private String fileName;
    private String version;
    private int versionCode;
    private String type;
    private String downloadUrl;

    public String getFileName() {
        return fileName;
    }

    public String getVersion() {
        return version;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getType() {
        return type;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "YnryoVersionData{" +
                "fileName='" + fileName + '\'' +
                ", version='" + version + '\'' +
                ", versionCode=" + versionCode +
                ", type='" + type + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
