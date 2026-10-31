package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.PartnerException;

import java.time.Instant;
import java.util.Objects;

/**
 * 정착 제휴 링크 (Aggregate Root, 플랫폼 공통). 설계 §4.6·§6.3.
 * 화면은 항상 "제휴" 표시를 붙이고, 공지 본문·자동 답변에는 넣지 않는다. 대학별 노출은 Office 가 분류 단위로 켠다.
 */
public class Partner {

    private Long id;
    private PartnerCategory category;
    private String name;
    private String description;
    private String targetUrl;
    private boolean enabled;
    private int sortOrder;
    private final Instant createdAt;

    private Partner(Long id, PartnerCategory category, String name, String description, String targetUrl,
                    boolean enabled, int sortOrder, Instant createdAt) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.targetUrl = targetUrl;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public static Partner create(PartnerCategory category, String name, String description, String targetUrl,
                                 int sortOrder, Instant now) {
        Objects.requireNonNull(now, "now는 필수입니다");
        Partner p = new Partner(null, null, null, null, null, false, sortOrder, now);
        p.update(category, name, description, targetUrl, sortOrder);
        return p;
    }

    public static Partner restore(Long id, PartnerCategory category, String name, String description, String targetUrl,
                                  boolean enabled, int sortOrder, Instant createdAt) {
        return new Partner(id, category, name, description, targetUrl, enabled, sortOrder, createdAt);
    }

    public void update(PartnerCategory category, String name, String description, String targetUrl, int sortOrder) {
        Objects.requireNonNull(category, "category는 필수입니다");
        if (name == null || name.isBlank()) {
            throw new PartnerException("제휴사 이름은 비어 있을 수 없습니다", PartnerException.ErrorType.BAD_REQUEST);
        }
        String url = targetUrl == null ? null : targetUrl.trim();
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            throw new PartnerException("제휴 링크는 http(s) URL 이어야 합니다", PartnerException.ErrorType.BAD_REQUEST);
        }
        this.category = category;
        this.name = name.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.targetUrl = url;
        this.sortOrder = sortOrder;
    }

    public void enable()  { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public void assignId(Long id) { this.id = id; }

    public Long getId()                  { return id; }
    public PartnerCategory getCategory() { return category; }
    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public String getTargetUrl()         { return targetUrl; }
    public boolean isEnabled()           { return enabled; }
    public int getSortOrder()            { return sortOrder; }
    public Instant getCreatedAt()        { return createdAt; }
}
