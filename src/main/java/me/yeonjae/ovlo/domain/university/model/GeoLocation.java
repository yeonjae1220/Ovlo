package me.yeonjae.ovlo.domain.university.model;

public record GeoLocation(double latitude, double longitude) {

    public GeoLocation {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("위도는 -90 ~ 90 사이여야 합니다: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("경도는 -180 ~ 180 사이여야 합니다: " + longitude);
        }
    }
}
