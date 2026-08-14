package fr.ynryo.stalktonbus.apiResponsesPOJO.version;

import androidx.annotation.NonNull;

public class VersionData {
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
        return "VersionData{" +
                "fileName='" + fileName + '\'' +
                ", version='" + version + '\'' +
                ", versionCode=" + versionCode +
                ", type='" + type + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
