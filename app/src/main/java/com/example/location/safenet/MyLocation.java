package com.example.location.safenet;

import android.location.Location;

public class MyLocation {
    Double latitude;
    Double longitude;
    Long when;

    public MyLocation() {

    }

    public MyLocation(Double latitude, Double longitude, Long when) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.when = when;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Long getWhen() {
        return when;
    }

    static MyLocation create(Location location) {
        return new MyLocation(location.getLatitude(), location.getLongitude(), location.getTime());
    }
}
