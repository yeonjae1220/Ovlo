package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "university_report")
public class UniversityReportJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_univ_id")
    private Long globalUnivId;

    @Column(name = "exchange_univ_id")
    private Long exchangeUnivId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "source_video_count", nullable = false)
    private int sourceVideoCount;

    @Column(name = "source_web_count", nullable = false)
    private int sourceWebCount;

    @Column(name = "source_video_ids", columnDefinition = "bigint[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 500)
    private Long[] sourceVideoIds;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "avg_cost_monthly")
    private String avgCostMonthly;

    @Column(name = "cost_currency")
    private String costCurrency;

    @Column(name = "aggregate_stats", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String aggregateStats;

    @Column(name = "supported_langs", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 20)
    private String[] supportedLangs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UniversityReportJpaEntity() {}

    public Long getId()               { return id; }
    public Long getGlobalUnivId()     { return globalUnivId; }
    public Long getExchangeUnivId()   { return exchangeUnivId; }
    public String getStatus()         { return status; }
    public int getSourceVideoCount()  { return sourceVideoCount; }
    public int getSourceWebCount()    { return sourceWebCount; }
    public String[] getSupportedLangs(){ return supportedLangs; }
    public BigDecimal getAvgRating()  { return avgRating; }
    public String getAvgCostMonthly() { return avgCostMonthly; }
    public String getCostCurrency()   { return costCurrency; }
}
