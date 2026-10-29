package me.yeonjae.ovlo.domain.university.model;

import java.util.List;

public class UniversityReport {

    private final Long id;
    private final Long globalUnivId;
    private final Long exchangeUnivId;
    private final String status;
    private final int sourceVideoCount;
    private final int sourceWebCount;
    private final List<String> supportedLangs;
    private final String avgCostMonthly;
    private final String costCurrency;

    private UniversityReport(Long id, Long globalUnivId, Long exchangeUnivId, String status,
                             int sourceVideoCount, int sourceWebCount, List<String> supportedLangs,
                             String avgCostMonthly, String costCurrency) {
        this.id = id;
        this.globalUnivId = globalUnivId;
        this.exchangeUnivId = exchangeUnivId;
        this.status = status;
        this.sourceVideoCount = sourceVideoCount;
        this.sourceWebCount = sourceWebCount;
        this.supportedLangs = supportedLangs;
        this.avgCostMonthly = avgCostMonthly;
        this.costCurrency = costCurrency;
    }

    public static UniversityReport restore(Long id, Long globalUnivId, Long exchangeUnivId,
                                           String status, int sourceVideoCount, int sourceWebCount,
                                           List<String> supportedLangs,
                                           String avgCostMonthly, String costCurrency) {
        return new UniversityReport(id, globalUnivId, exchangeUnivId, status,
                sourceVideoCount, sourceWebCount, supportedLangs, avgCostMonthly, costCurrency);
    }

    public Long getId()               { return id; }
    public Long getGlobalUnivId()     { return globalUnivId; }
    public Long getExchangeUnivId()   { return exchangeUnivId; }
    public String getStatus()         { return status; }
    public int getSourceVideoCount()  { return sourceVideoCount; }
    public int getSourceWebCount()    { return sourceWebCount; }
    public List<String> getSupportedLangs() { return supportedLangs; }
    public String getAvgCostMonthly() { return avgCostMonthly; }
    public String getCostCurrency()   { return costCurrency; }
}
