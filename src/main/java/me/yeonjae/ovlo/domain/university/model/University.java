package me.yeonjae.ovlo.domain.university.model;

import java.util.Objects;

public class University {

    private UniversityId id;
    private String name;
    private String localName;
    private CountryCode countryCode;
    private String city;
    private GeoLocation geoLocation;
    private String websiteUrl;

    private University() {}

    public static University create(
            String name,
            String localName,
            CountryCode countryCode,
            String city,
            GeoLocation geoLocation,
            String websiteUrl) {

        Objects.requireNonNull(name, "대학명은 필수입니다");
        if (name.isBlank()) throw new IllegalArgumentException("대학명은 빈 값일 수 없습니다");
        Objects.requireNonNull(countryCode, "국가 코드는 필수입니다");
        Objects.requireNonNull(city, "도시는 필수입니다");
        if (city.isBlank()) throw new IllegalArgumentException("도시는 빈 값일 수 없습니다");
        Objects.requireNonNull(geoLocation, "좌표는 필수입니다");

        University university = new University();
        university.name = name;
        university.localName = localName;
        university.countryCode = countryCode;
        university.city = city;
        university.geoLocation = geoLocation;
        university.websiteUrl = websiteUrl;
        return university;
    }

    public static University restore(
            UniversityId id,
            String name,
            String localName,
            CountryCode countryCode,
            String city,
            GeoLocation geoLocation,
            String websiteUrl) {

        University university = create(name, localName, countryCode, city, geoLocation, websiteUrl);
        university.id = id;
        return university;
    }

    public void updateGeoLocation(GeoLocation newLocation) {
        Objects.requireNonNull(newLocation, "좌표는 필수입니다");
        this.geoLocation = newLocation;
    }

    public void updateWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public UniversityId getId() { return id; }
    public String getName() { return name; }
    public String getLocalName() { return localName; }
    public CountryCode getCountryCode() { return countryCode; }
    public String getCity() { return city; }
    public GeoLocation getGeoLocation() { return geoLocation; }
    public String getWebsiteUrl() { return websiteUrl; }
}
