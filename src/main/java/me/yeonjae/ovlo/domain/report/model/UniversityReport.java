package me.yeonjae.ovlo.domain.report.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class UniversityReport {
    private final Long id;
    private final Long globalUnivId;
    private final Long exchangeUnivId;
    private final String status;
    private final int sourceVideoCount;
    private final int sourceWebCount;
    private final BigDecimal avgRating;
    private final BigDecimal recommendRatio;
    private final BigDecimal avgDifficulty;
    private final String costCurrency;
    private final String aggregateStats;
    private final List<String> supportedLangs;
    private final Instant createdAt;
    private final Instant updatedAt;

    public UniversityReport(Long id, Long globalUnivId, Long exchangeUnivId, String status,
                            int sourceVideoCount, int sourceWebCount,
                            BigDecimal avgRating, BigDecimal recommendRatio, BigDecimal avgDifficulty,
                            String costCurrency, String aggregateStats,
                            List<String> supportedLangs, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.globalUnivId = globalUnivId;
        this.exchangeUnivId = exchangeUnivId;
        this.status = status;
        this.sourceVideoCount = sourceVideoCount;
        this.sourceWebCount = sourceWebCount;
        this.avgRating = avgRating;
        this.recommendRatio = recommendRatio;
        this.avgDifficulty = avgDifficulty;
        this.costCurrency = costCurrency;
        this.aggregateStats = aggregateStats;
        this.supportedLangs = supportedLangs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getGlobalUnivId() { return globalUnivId; }
    public Long getExchangeUnivId() { return exchangeUnivId; }
    public String getStatus() { return status; }
    public int getSourceVideoCount() { return sourceVideoCount; }
    public int getSourceWebCount() { return sourceWebCount; }
    public BigDecimal getAvgRating() { return avgRating; }
    public BigDecimal getRecommendRatio() { return recommendRatio; }
    public BigDecimal getAvgDifficulty() { return avgDifficulty; }
    public String getCostCurrency() { return costCurrency; }
    public String getAggregateStats() { return aggregateStats; }
    public List<String> getSupportedLangs() { return supportedLangs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
