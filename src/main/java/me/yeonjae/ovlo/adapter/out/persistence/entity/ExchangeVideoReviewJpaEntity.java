package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "exchange_video_reviews")
public class ExchangeVideoReviewJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id")
    private Long universityId;

    @Column(name = "youtube_url", nullable = false, unique = true)
    private String youtubeUrl;

    private String title;
    private String channel;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    private String country;
    private String city;

    @Column(name = "overall_rating")
    private Integer overallRating;

    private Integer difficulty;
    private Integer workload;
    private Boolean recommend;

    @Column(name = "overall_tone")
    private String overallTone;

    @Column(name = "excitement_level")
    private Integer excitementLevel;

    @Column(name = "cost_total")
    private String costTotal;

    @Column(name = "cost_rent")
    private String costRent;

    @Column(name = "cost_food")
    private String costFood;

    @Column(name = "cost_transport")
    private String costTransport;

    @Column(name = "cost_currency")
    private String costCurrency;

    @Column(name = "visa_type")
    private String visaType;

    @Column(name = "visa_cost")
    private String visaCost;

    @Column(name = "visa_duration")
    private String visaDuration;

    @Column(name = "visa_processing_days")
    private String visaProcessingDays;

    @Column(name = "dorm_available")
    private Boolean dormAvailable;

    @Column(name = "dorm_type")
    private String dormType;

    @Column(name = "dorm_price")
    private String dormPrice;

    @Column(name = "gpa_requirement")
    private String gpaRequirement;

    @Column(name = "language_req")
    private String languageReq;

    @Column(name = "deadline_info")
    private String deadlineInfo;

    @Column(name = "source_lang")
    private String sourceLang;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "exchange_info", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String exchangeInfo;

    @Column(name = "tags", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 100)
    private String[] tags;

    public ExchangeVideoReviewJpaEntity() {}

    public Long getId()                  { return id; }
    public Long getUniversityId()        { return universityId; }
    public String getYoutubeUrl()        { return youtubeUrl; }
    public String getTitle()             { return title; }
    public String getChannel()           { return channel; }
    public OffsetDateTime getPublishedAt(){ return publishedAt; }
    public String getCountry()           { return country; }
    public String getCity()              { return city; }
    public Integer getOverallRating()    { return overallRating; }
    public Integer getDifficulty()       { return difficulty; }
    public Integer getWorkload()         { return workload; }
    public Boolean getRecommend()        { return recommend; }
    public String getOverallTone()       { return overallTone; }
    public Integer getExcitementLevel()  { return excitementLevel; }
    public String getCostTotal()         { return costTotal; }
    public String getCostRent()          { return costRent; }
    public String getCostFood()          { return costFood; }
    public String getCostTransport()     { return costTransport; }
    public String getCostCurrency()      { return costCurrency; }
    public String getVisaType()          { return visaType; }
    public String getVisaCost()          { return visaCost; }
    public String getVisaDuration()      { return visaDuration; }
    public String getVisaProcessingDays(){ return visaProcessingDays; }
    public Boolean getDormAvailable()    { return dormAvailable; }
    public String getDormType()          { return dormType; }
    public String getDormPrice()         { return dormPrice; }
    public String getGpaRequirement()    { return gpaRequirement; }
    public String getLanguageReq()       { return languageReq; }
    public String getDeadlineInfo()      { return deadlineInfo; }
    public String getSourceLang()        { return sourceLang; }
    public Double getQualityScore()      { return qualityScore; }
    public String getSummary()           { return summary; }
    public String getExchangeInfo()      { return exchangeInfo; }
    public String[] getTags()            { return tags; }
}
