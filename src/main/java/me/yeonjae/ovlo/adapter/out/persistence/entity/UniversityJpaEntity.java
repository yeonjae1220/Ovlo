package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "university")
public class UniversityJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(name = "local_name") private String localName;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode;
    @Column(nullable = false) private String city;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    @Column(name = "website_url") private String websiteUrl;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public UniversityJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getLocalName() { return localName; } public void setLocalName(String localName) { this.localName = localName; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public double getLatitude() { return latitude; } public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; } public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getWebsiteUrl() { return websiteUrl; } public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
}
