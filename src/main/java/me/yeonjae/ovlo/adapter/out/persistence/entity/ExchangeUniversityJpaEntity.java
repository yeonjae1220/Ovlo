package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exchange_universities")
public class ExchangeUniversityJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_ko")
    private String nameKo;

    @Column(name = "name_en", nullable = false, unique = true)
    private String nameEn;

    private String country;
    private String city;
    private String website;

    @Column(name = "global_univ_id")
    private Long globalUnivId;

    public ExchangeUniversityJpaEntity() {}

    public Long getId()           { return id; }
    public String getNameKo()     { return nameKo; }
    public String getNameEn()     { return nameEn; }
    public String getCountry()    { return country; }
    public String getCity()       { return city; }
    public String getWebsite()    { return website; }
    public Long getGlobalUnivId() { return globalUnivId; }
}
