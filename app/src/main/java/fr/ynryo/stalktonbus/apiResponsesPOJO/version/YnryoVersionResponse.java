package fr.ynryo.stalktonbus.apiResponsesPOJO.version;

import androidx.annotation.NonNull;

public class YnryoVersionResponse {
    private boolean success;
    private YnryoVersionData version;

    public boolean isSuccess() {
        return success;
    }

    public YnryoVersionData getVersion() {
        return version;
    }

    @NonNull
    @Override
    public String toString() {
        return "YnryoVersionResponse{" +
                "success=" + success +
                ", version=" + version +
                '}';
    }
}
