package fr.ynryo.stalktonbus.apiResponsesPOJO.version;

import androidx.annotation.NonNull;

public class VersionResponse {
    private boolean success;
    private VersionData version;

    public boolean isSuccess() {
        return success;
    }

    public VersionData getVersion() {
        return version;
    }

    @NonNull
    @Override
    public String toString() {
        return "VersionResponse{" +
                "success=" + success +
                ", version=" + version +
                '}';
    }
}
