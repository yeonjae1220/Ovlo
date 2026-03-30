package me.yeonjae.ovlo.domain.university.model;

public class GlobalUniversity {

    private final GlobalUniversityId id;
    private final String nameEn;
    private final String country;
    private final String countryEn;
    private final String countryCode;
    private final String city;
    private final String website;
    private final String domain;

    private GlobalUniversity(GlobalUniversityId id, String nameEn, String country,
                              String countryEn, String countryCode, String city,
                              String website, String domain) {
        this.id = id;
        this.nameEn = nameEn;
        this.country = country;
        this.countryEn = countryEn;
        this.countryCode = countryCode;
        this.city = city;
        this.website = website;
        this.domain = domain;
    }

    public static GlobalUniversity restore(Long id, String nameEn, String country,
                                           String countryEn, String countryCode,
                                           String city, String website, String domain) {
        return new GlobalUniversity(new GlobalUniversityId(id), nameEn, country,
                countryEn, countryCode, city, website, domain);
    }

    public GlobalUniversityId getId()  { return id; }
    public String getNameEn()          { return nameEn; }
    public String getCountry()         { return country; }
    public String getCountryEn()       { return countryEn; }
    public String getCountryCode()     { return countryCode; }
    public String getCity()            { return city; }
    public String getWebsite()         { return website; }
    public String getDomain()          { return domain; }
}
