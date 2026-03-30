package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "global_universities")
public class GlobalUniversityJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_en")
    private String nameEn;

    private String country;

    @Column(name = "country_en")
    private String countryEn;

    @Column(name = "country_code")
    private String countryCode;

    private String city;
    private String website;
    private String domain;

    public GlobalUniversityJpaEntity() {}

    public Long getId()          { return id; }
    public String getNameEn()    { return nameEn; }
    public String getCountry()   { return country; }
    public String getCountryEn() { return countryEn; }
    public String getCountryCode(){ return countryCode; }
    public String getCity()      { return city; }
    public String getWebsite()   { return website; }
    public String getDomain()    { return domain; }
}
