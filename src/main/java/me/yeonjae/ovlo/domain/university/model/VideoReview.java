package me.yeonjae.ovlo.domain.university.model;

import java.time.OffsetDateTime;

public class VideoReview {

    private final VideoReviewId id;
    private final ExchangeUniversityId exchangeUniversityId; // nullable
    private final String youtubeUrl;
    private final String title;
    private final String channel;
    private final OffsetDateTime publishedAt;
    private final String country;
    private final String city;
    private final Integer overallRating;
    private final Integer difficulty;
    private final Integer workload;
    private final Boolean recommend;
    private final String overallTone;
    private final Integer excitementLevel;
    private final String costTotal;
    private final String costRent;
    private final String costFood;
    private final String costTransport;
    private final String costCurrency;
    private final String visaType;
    private final String visaCost;
    private final String visaDuration;
    private final String visaProcessingDays;
    private final Boolean dormAvailable;
    private final String dormType;
    private final String dormPrice;
    private final String gpaRequirement;
    private final String languageReq;
    private final String deadlineInfo;
    private final String sourceLang;
    private final Double qualityScore;
    private final String summary;
    private final String exchangeInfo; // raw JSON
    private final String[] tags;

    private VideoReview(Builder builder) {
        this.id = builder.id;
        this.exchangeUniversityId = builder.exchangeUniversityId;
        this.youtubeUrl = builder.youtubeUrl;
        this.title = builder.title;
        this.channel = builder.channel;
        this.publishedAt = builder.publishedAt;
        this.country = builder.country;
        this.city = builder.city;
        this.overallRating = builder.overallRating;
        this.difficulty = builder.difficulty;
        this.workload = builder.workload;
        this.recommend = builder.recommend;
        this.overallTone = builder.overallTone;
        this.excitementLevel = builder.excitementLevel;
        this.costTotal = builder.costTotal;
        this.costRent = builder.costRent;
        this.costFood = builder.costFood;
        this.costTransport = builder.costTransport;
        this.costCurrency = builder.costCurrency;
        this.visaType = builder.visaType;
        this.visaCost = builder.visaCost;
        this.visaDuration = builder.visaDuration;
        this.visaProcessingDays = builder.visaProcessingDays;
        this.dormAvailable = builder.dormAvailable;
        this.dormType = builder.dormType;
        this.dormPrice = builder.dormPrice;
        this.gpaRequirement = builder.gpaRequirement;
        this.languageReq = builder.languageReq;
        this.deadlineInfo = builder.deadlineInfo;
        this.sourceLang = builder.sourceLang;
        this.qualityScore = builder.qualityScore;
        this.summary = builder.summary;
        this.exchangeInfo = builder.exchangeInfo;
        this.tags = builder.tags;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private VideoReviewId id;
        private ExchangeUniversityId exchangeUniversityId;
        private String youtubeUrl;
        private String title;
        private String channel;
        private OffsetDateTime publishedAt;
        private String country;
        private String city;
        private Integer overallRating;
        private Integer difficulty;
        private Integer workload;
        private Boolean recommend;
        private String overallTone;
        private Integer excitementLevel;
        private String costTotal;
        private String costRent;
        private String costFood;
        private String costTransport;
        private String costCurrency;
        private String visaType;
        private String visaCost;
        private String visaDuration;
        private String visaProcessingDays;
        private Boolean dormAvailable;
        private String dormType;
        private String dormPrice;
        private String gpaRequirement;
        private String languageReq;
        private String deadlineInfo;
        private String sourceLang;
        private Double qualityScore;
        private String summary;
        private String exchangeInfo;
        private String[] tags;

        private Builder() {}

        public Builder id(VideoReviewId id)                          { this.id = id; return this; }
        public Builder exchangeUniversityId(ExchangeUniversityId v)  { this.exchangeUniversityId = v; return this; }
        public Builder youtubeUrl(String v)                          { this.youtubeUrl = v; return this; }
        public Builder title(String v)                               { this.title = v; return this; }
        public Builder channel(String v)                             { this.channel = v; return this; }
        public Builder publishedAt(OffsetDateTime v)                 { this.publishedAt = v; return this; }
        public Builder country(String v)                             { this.country = v; return this; }
        public Builder city(String v)                                { this.city = v; return this; }
        public Builder overallRating(Integer v)                      { this.overallRating = v; return this; }
        public Builder difficulty(Integer v)                         { this.difficulty = v; return this; }
        public Builder workload(Integer v)                           { this.workload = v; return this; }
        public Builder recommend(Boolean v)                          { this.recommend = v; return this; }
        public Builder overallTone(String v)                         { this.overallTone = v; return this; }
        public Builder excitementLevel(Integer v)                    { this.excitementLevel = v; return this; }
        public Builder costTotal(String v)                           { this.costTotal = v; return this; }
        public Builder costRent(String v)                            { this.costRent = v; return this; }
        public Builder costFood(String v)                            { this.costFood = v; return this; }
        public Builder costTransport(String v)                       { this.costTransport = v; return this; }
        public Builder costCurrency(String v)                        { this.costCurrency = v; return this; }
        public Builder visaType(String v)                            { this.visaType = v; return this; }
        public Builder visaCost(String v)                            { this.visaCost = v; return this; }
        public Builder visaDuration(String v)                        { this.visaDuration = v; return this; }
        public Builder visaProcessingDays(String v)                  { this.visaProcessingDays = v; return this; }
        public Builder dormAvailable(Boolean v)                      { this.dormAvailable = v; return this; }
        public Builder dormType(String v)                            { this.dormType = v; return this; }
        public Builder dormPrice(String v)                           { this.dormPrice = v; return this; }
        public Builder gpaRequirement(String v)                      { this.gpaRequirement = v; return this; }
        public Builder languageReq(String v)                         { this.languageReq = v; return this; }
        public Builder deadlineInfo(String v)                        { this.deadlineInfo = v; return this; }
        public Builder sourceLang(String v)                          { this.sourceLang = v; return this; }
        public Builder qualityScore(Double v)                        { this.qualityScore = v; return this; }
        public Builder summary(String v)                             { this.summary = v; return this; }
        public Builder exchangeInfo(String v)                        { this.exchangeInfo = v; return this; }
        public Builder tags(String[] v)                              { this.tags = v; return this; }

        public VideoReview build() { return new VideoReview(this); }
    }

    public VideoReviewId getId()                          { return id; }
    public ExchangeUniversityId getExchangeUniversityId() { return exchangeUniversityId; }
    public String getYoutubeUrl()                         { return youtubeUrl; }
    public String getTitle()                              { return title; }
    public String getChannel()                            { return channel; }
    public OffsetDateTime getPublishedAt()                { return publishedAt; }
    public String getCountry()                            { return country; }
    public String getCity()                               { return city; }
    public Integer getOverallRating()                     { return overallRating; }
    public Integer getDifficulty()                        { return difficulty; }
    public Integer getWorkload()                          { return workload; }
    public Boolean getRecommend()                         { return recommend; }
    public String getOverallTone()                        { return overallTone; }
    public Integer getExcitementLevel()                   { return excitementLevel; }
    public String getCostTotal()                          { return costTotal; }
    public String getCostRent()                           { return costRent; }
    public String getCostFood()                           { return costFood; }
    public String getCostTransport()                      { return costTransport; }
    public String getCostCurrency()                       { return costCurrency; }
    public String getVisaType()                           { return visaType; }
    public String getVisaCost()                           { return visaCost; }
    public String getVisaDuration()                       { return visaDuration; }
    public String getVisaProcessingDays()                 { return visaProcessingDays; }
    public Boolean getDormAvailable()                     { return dormAvailable; }
    public String getDormType()                           { return dormType; }
    public String getDormPrice()                          { return dormPrice; }
    public String getGpaRequirement()                     { return gpaRequirement; }
    public String getLanguageReq()                        { return languageReq; }
    public String getDeadlineInfo()                       { return deadlineInfo; }
    public String getSourceLang()                         { return sourceLang; }
    public Double getQualityScore()                       { return qualityScore; }
    public String getSummary()                            { return summary; }
    public String getExchangeInfo()                       { return exchangeInfo; }
    public String[] getTags()                             { return tags; }
}
