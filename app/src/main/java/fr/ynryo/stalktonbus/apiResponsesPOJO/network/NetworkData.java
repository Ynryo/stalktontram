package fr.ynryo.stalktonbus.apiResponsesPOJO.network;

import androidx.annotation.NonNull;

import java.net.URI;

public class NetworkData {
    private int id;
    private String ref;
    private String name;
    private String authority;
    private URI logoHref;
    private int regionId;

    public int getId() {
        return id;
    }

    public String getRef() {
        return ref;
    }

    public String getName() {
        return name;
    }

    public String getAuthority() {
        return authority;
    }

    public URI getLogoHref() {
        return logoHref;
    }

    public int getRegionId() {
        return regionId;
    }

    @NonNull
    @Override
    public String toString() {
        return "NetworkData{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", logoHref=" + logoHref +
                ", regionId=" + regionId +
                '}';
    }
}
