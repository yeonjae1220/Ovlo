package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "university_report")
public class UniversityReportJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_univ_id")
    private Long globalUnivId;

    @Column(name = "exchange_univ_id")
    private Long exchangeUnivId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "source_video_count", nullable = false)
    private int sourceVideoCount;

    @Column(name = "source_web_count", nullable = false)
    private int sourceWebCount;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "recommend_ratio", precision = 3, scale = 2)
    private BigDecimal recommendRatio;

    @Column(name = "avg_difficulty", precision = 3, scale = 2)
    private BigDecimal avgDifficulty;

    @Column(name = "cost_currency", length = 20)
    private String costCurrency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aggregate_stats", columnDefinition = "jsonb")
    private String aggregateStats;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "supported_langs", columnDefinition = "text[]")
    private String[] supportedLangs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UniversityReportJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGlobalUnivId() { return globalUnivId; }
    public void setGlobalUnivId(Long globalUnivId) { this.globalUnivId = globalUnivId; }
    public Long getExchangeUnivId() { return exchangeUnivId; }
    public void setExchangeUnivId(Long exchangeUnivId) { this.exchangeUnivId = exchangeUnivId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSourceVideoCount() { return sourceVideoCount; }
    public void setSourceVideoCount(int sourceVideoCount) { this.sourceVideoCount = sourceVideoCount; }
    public int getSourceWebCount() { return sourceWebCount; }
    public void setSourceWebCount(int sourceWebCount) { this.sourceWebCount = sourceWebCount; }
    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
    public BigDecimal getRecommendRatio() { return recommendRatio; }
    public void setRecommendRatio(BigDecimal recommendRatio) { this.recommendRatio = recommendRatio; }
    public BigDecimal getAvgDifficulty() { return avgDifficulty; }
    public void setAvgDifficulty(BigDecimal avgDifficulty) { this.avgDifficulty = avgDifficulty; }
    public String getCostCurrency() { return costCurrency; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }
    public String getAggregateStats() { return aggregateStats; }
    public void setAggregateStats(String aggregateStats) { this.aggregateStats = aggregateStats; }
    public String[] getSupportedLangs() { return supportedLangs; }
    public void setSupportedLangs(String[] supportedLangs) { this.supportedLangs = supportedLangs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
