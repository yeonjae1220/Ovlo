package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

/**
 * 단일 대학 카탈로그 엔티티. global_universities 테이블 매핑 (약 10,150개).
 * 좌표(latitude/longitude)·현지어명(local_name)은 큐레이션된 일부 대학에만 존재 → nullable.
 */
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

    @Column(name = "local_name")
    private String localName;

    private Double latitude;
    private Double longitude;

    public GlobalUniversityJpaEntity() {}

    public Long getId()          { return id; }
    public String getNameEn()    { return nameEn; }
    public String getCountry()   { return country; }
    public String getCountryEn() { return countryEn; }
    public String getCountryCode(){ return countryCode; }
    public String getCity()      { return city; }
    public String getWebsite()   { return website; }
    public String getDomain()    { return domain; }
    public String getLocalName() { return localName; }
    public Double getLatitude()  { return latitude; }
    public Double getLongitude() { return longitude; }
}
