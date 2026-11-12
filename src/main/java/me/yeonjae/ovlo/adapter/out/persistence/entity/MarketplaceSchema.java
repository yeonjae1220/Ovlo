package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.io.Serializable;

/** Hibernate schema metadata for H2 local/test profiles. Production DDL is owned by Flyway V43/V45.
 * MarketplacePersistenceAdapter performs explicit, version-conditional writes through JDBC. */
@Entity @Table(name="marketplace_listing")
class MarketplaceListingSchema {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="seller_id",nullable=false) long seller;
    @Column(length=120,nullable=false) String title;
    @Column(columnDefinition="text",nullable=false) String description;
    @Column(name="source_language",length=2,nullable=false) String sourceLanguage;
    @Column(length=20,nullable=false) String category;
    @Column(name="item_condition",length=20,nullable=false) String condition;
    boolean free; boolean bundle;
    @Column(name="partial_pickup") boolean partialPickup;
    @Column(precision=20,scale=8,nullable=false) BigDecimal amount;
    @Column(length=3,nullable=false) String currency;
    @Column(name="country_code",length=2,nullable=false) String countryCode;
    @Column(length=100,nullable=false) String city;
    @Column(name="university_id") Long university;
    @Column(name="meeting_point",length=200,nullable=false) String meetingPoint;
    @Column(name="pickup_from") LocalDate pickupFrom;
    @Column(name="pickup_until") LocalDate pickupUntil;
    @Column(length=100,nullable=false) String tags;
    @Column(length=30,nullable=false) String status;
    boolean hidden; boolean deleted;
    @Column(columnDefinition="bigint default 0") long version;
    @Column(name="created_at",columnDefinition="timestamp with time zone default current_timestamp") Instant createdAt;
}
@Entity @Table(name="marketplace_photo")
class MarketplacePhotoSchema {
    @Id UUID id;
    @Column(name="owner_id",nullable=false) long owner;
    @Column(name="storage_path",columnDefinition="text",nullable=false) String path;
    @Column(name="storage_type",length=20,nullable=false) String type;
    @Column(name="created_at",columnDefinition="timestamp with time zone default current_timestamp") Instant createdAt;
    @Column(name="orphaned_at",columnDefinition="timestamp with time zone default current_timestamp") Instant orphanedAt;
    @Column(name="deletion_pending",nullable=false,columnDefinition="boolean default false") boolean deletionPending;
    @Column(name="cleanup_retry_at") Instant cleanupRetryAt;
}
@Entity @Table(name="marketplace_listing_photo",uniqueConstraints=@UniqueConstraint(columnNames={"listing_id","position"}))
class MarketplaceListingPhotoSchema {
    @Id @Column(name="photo_id") UUID photo;
    @Column(name="listing_id",nullable=false) long listing;
    @Column(nullable=false) int position;
}
@Entity @Table(name="marketplace_report",uniqueConstraints=@UniqueConstraint(columnNames={"listing_id","reporter_id"}))
class MarketplaceReportSchema {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="listing_id",nullable=false) long listing;
    @Column(name="reporter_id",nullable=false) long reporter;
    @Column(length=1000,nullable=false) String reason;
    @Column(name="created_at",columnDefinition="timestamp with time zone default current_timestamp") Instant createdAt;
}
@Entity @Table(name="marketplace_audit")
class MarketplaceAuditSchema {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="listing_id",nullable=false) long listing;
    @Column(nullable=false) String actor;
    @Column(length=30,nullable=false) String action;
    @Column(name="created_at",columnDefinition="timestamp with time zone default current_timestamp") Instant createdAt;
}
@Entity @Table(name="marketplace_translation") @IdClass(MarketplaceTranslationSchema.Key.class)
class MarketplaceTranslationSchema {
    @Id @Column(name="listing_id") Long listing;
    @Id Long version;
    @Id @Column(length=2) String language;
    @Column(columnDefinition="text",nullable=false) String title;
    @Column(columnDefinition="text",nullable=false) String description;
    public record Key(Long listing,Long version,String language) implements Serializable {}
}
@Entity @Table(name="marketplace_translation_lease") @IdClass(MarketplaceTranslationLeaseSchema.Key.class)
class MarketplaceTranslationLeaseSchema {
    @Id @Column(name="listing_id") Long listing;
    @Id Long version;
    @Id @Column(length=2) String language;
    UUID owner;
    @Column(name="expires_at") Instant expiresAt;
    public record Key(Long listing,Long version,String language) implements Serializable {}
}
