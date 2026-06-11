package me.yeonjae.ovlo.domain.university.model;

/**
 * 대학 (단일 카탈로그). global_universities(약 10,150개)를 백킹으로 한다.
 * 카탈로그 데이터는 외부에서 적재되므로 도메인에서 생성하지 않고 {@link #restore} 로만 복원한다.
 * 좌표(geoLocation)는 큐레이션된 일부 대학에만 존재하므로 nullable.
 */
public class University {

    private UniversityId id;
    private String name;          // global.name_en
    private String localName;     // 현지어/약칭 (nullable)
    private String country;       // 한국어 국가명 (nullable)
    private String countryEn;     // 영문 국가명 (nullable)
    private CountryCode countryCode;
    private String city;          // nullable
    private GeoLocation geoLocation; // nullable (좌표 보유 대학만)
    private String websiteUrl;    // global.website
    private String domain;        // 학교 이메일 도메인 (검증용, nullable)

    private University() {}

    public static University restore(
            UniversityId id,
            String name,
            String localName,
            String country,
            String countryEn,
            CountryCode countryCode,
            String city,
            GeoLocation geoLocation,
            String websiteUrl,
            String domain) {

        University u = new University();
        u.id = id;
        u.name = name;
        u.localName = localName;
        u.country = country;
        u.countryEn = countryEn;
        u.countryCode = countryCode;
        u.city = city;
        u.geoLocation = geoLocation;
        u.websiteUrl = websiteUrl;
        u.domain = domain;
        return u;
    }

    public boolean hasCoordinates() {
        return geoLocation != null;
    }

    public UniversityId getId()       { return id; }
    public String getName()           { return name; }
    public String getLocalName()      { return localName; }
    public String getCountry()        { return country; }
    public String getCountryEn()      { return countryEn; }
    public CountryCode getCountryCode() { return countryCode; }
    public String getCity()           { return city; }
    public GeoLocation getGeoLocation() { return geoLocation; }
    public String getWebsiteUrl()     { return websiteUrl; }
    public String getDomain()         { return domain; }
}
