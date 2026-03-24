package me.yeonjae.ovlo.domain.university.model;

public class ExchangeUniversity {

    private final ExchangeUniversityId id;
    private final String nameKo;
    private final String nameEn;
    private final String country;
    private final String city;
    private final String website;
    private final Long globalUnivId;

    private ExchangeUniversity(ExchangeUniversityId id, String nameKo, String nameEn,
                                String country, String city, String website, Long globalUnivId) {
        this.id = id;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.country = country;
        this.city = city;
        this.website = website;
        this.globalUnivId = globalUnivId;
    }

    public static ExchangeUniversity restore(Long id, String nameKo, String nameEn,
                                              String country, String city, String website, Long globalUnivId) {
        return new ExchangeUniversity(new ExchangeUniversityId(id), nameKo, nameEn, country, city, website, globalUnivId);
    }

    public ExchangeUniversityId getId() { return id; }
    public String getNameKo()          { return nameKo; }
    public String getNameEn()          { return nameEn; }
    public String getCountry()         { return country; }
    public String getCity()            { return city; }
    public String getWebsite()         { return website; }
    public Long getGlobalUnivId()      { return globalUnivId; }
}
