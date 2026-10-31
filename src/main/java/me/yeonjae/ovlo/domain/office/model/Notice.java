package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.NoticeException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 국제처 승인 자료 (Aggregate Root). 설계 §4.3.
 *
 * 불변식
 * - 제목·본문 필수, expiresAt > effectiveFrom
 * - APPROVED 전이는 DRAFT/EXPIRED 에서만, 승인자 필수, 만료일이 오늘 이후여야 한다
 * - 제목·본문이 바뀌면 version+1 · DRAFT 로 복귀 · 기존 번역은 stale
 * - ARCHIVED 는 수정·승인·번역을 받지 않는다
 * - 근거 자격 = APPROVED ∧ effectiveFrom ≤ today ≤ expiresAt ∧ (원문 언어이거나 최신 번역 존재)
 */
public class Notice {

    private Long id;
    private final Long officeId;
    private NoticeCategory category;
    private String title;
    private String body;
    private final NoticeLang sourceLang;
    private String sourceUrl;
    private List<Long> attachmentMediaIds;
    private String term;
    private LocalDate effectiveFrom;
    private LocalDate expiresAt;
    private NoticeStatus status;
    private String approvedBy;
    private Instant approvedAt;
    private Instant expiryWarnedAt;
    private int version;
    private final Long createdBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Map<NoticeLang, NoticeTranslation> translations = new EnumMap<>(NoticeLang.class);

    private Notice(Long id, Long officeId, NoticeCategory category, String title, String body, NoticeLang sourceLang,
                   String sourceUrl, List<Long> attachmentMediaIds, String term, LocalDate effectiveFrom,
                   LocalDate expiresAt, NoticeStatus status, String approvedBy, Instant approvedAt,
                   Instant expiryWarnedAt, int version,
                   Long createdBy, Instant createdAt, Instant updatedAt, List<NoticeTranslation> translations) {
        this.id = id;
        this.officeId = officeId;
        this.category = category;
        this.title = title;
        this.body = body;
        this.sourceLang = sourceLang;
        this.sourceUrl = sourceUrl;
        this.attachmentMediaIds = new ArrayList<>(attachmentMediaIds);
        this.term = term;
        this.effectiveFrom = effectiveFrom;
        this.expiresAt = expiresAt;
        this.status = status;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.expiryWarnedAt = expiryWarnedAt;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        translations.forEach(t -> this.translations.put(t.getLang(), t));
    }

    public static Notice register(Long officeId, NoticeCategory category, String title, String body, String sourceUrl,
                                  List<Long> attachmentMediaIds, String term, LocalDate effectiveFrom, LocalDate expiresAt,
                                  Long createdBy, Instant now) {
        Objects.requireNonNull(officeId, "officeId는 필수입니다");
        Objects.requireNonNull(category, "category는 필수입니다");
        Objects.requireNonNull(createdBy, "createdBy는 필수입니다");
        Objects.requireNonNull(now, "now는 필수입니다");
        validateDates(effectiveFrom, expiresAt);
        return new Notice(null, officeId, category, requireText(title, "제목"), requireText(body, "본문"), NoticeLang.KO,
                blankToNull(sourceUrl), attachmentMediaIds == null ? List.of() : attachmentMediaIds, requireText(term, "적용 학기"),
                effectiveFrom, expiresAt, NoticeStatus.DRAFT, null, null, null, 1, createdBy, now, now, List.of());
    }

    public static Notice restore(Long id, Long officeId, NoticeCategory category, String title, String body,
                                 NoticeLang sourceLang, String sourceUrl, List<Long> attachmentMediaIds, String term,
                                 LocalDate effectiveFrom, LocalDate expiresAt, NoticeStatus status, String approvedBy,
                                 Instant approvedAt, Instant expiryWarnedAt, int version, Long createdBy,
                                 Instant createdAt, Instant updatedAt, List<NoticeTranslation> translations) {
        return new Notice(id, officeId, category, title, body, sourceLang, sourceUrl, attachmentMediaIds, term,
                effectiveFrom, expiresAt, status, approvedBy, approvedAt, expiryWarnedAt, version, createdBy,
                createdAt, updatedAt, translations);
    }

    // ── 편집 ──────────────────────────────────────────────────────────────────

    /** 제목·본문·분류·출처·첨부. 제목이나 본문이 바뀌면 version+1, DRAFT 로 복귀. */
    public void editContent(NoticeCategory category, String title, String body, String sourceUrl,
                            List<Long> attachmentMediaIds, Instant now) {
        assertNotArchived();
        Objects.requireNonNull(category, "category는 필수입니다");
        String newTitle = requireText(title, "제목");
        String newBody = requireText(body, "본문");
        boolean contentChanged = !newTitle.equals(this.title) || !newBody.equals(this.body);
        this.category = category;
        this.title = newTitle;
        this.body = newBody;
        this.sourceUrl = blankToNull(sourceUrl);
        this.attachmentMediaIds = new ArrayList<>(attachmentMediaIds == null ? List.of() : attachmentMediaIds);
        this.updatedAt = now;
        if (contentChanged) {
            this.version += 1;
            this.status = NoticeStatus.DRAFT;
            this.approvedBy = null;
            this.approvedAt = null;
        }
    }

    /** 적용 학기·시작일·만료일. EXPIRED 공지에 새 만료일을 주고 다시 승인하는 경로. */
    public void reschedule(LocalDate effectiveFrom, LocalDate expiresAt, Instant now) {
        assertNotArchived();
        validateDates(effectiveFrom, expiresAt);
        this.effectiveFrom = effectiveFrom;
        this.expiresAt = expiresAt;
        this.expiryWarnedAt = null;
        this.updatedAt = now;
    }

    public void changeTerm(String term, Instant now) {
        assertNotArchived();
        this.term = requireText(term, "적용 학기");
        this.updatedAt = now;
    }

    // ── 상태 ──────────────────────────────────────────────────────────────────

    public void approve(String approvedBy, Instant now, LocalDate today) {
        assertNotArchived();
        if (status != NoticeStatus.DRAFT && status != NoticeStatus.EXPIRED) {
            throw new NoticeException("DRAFT 또는 EXPIRED 상태만 승인할 수 있습니다: " + status,
                    NoticeException.ErrorType.INVALID_STATE);
        }
        if (approvedBy == null || approvedBy.isBlank()) {
            throw new IllegalArgumentException("승인자(approvedBy)는 필수입니다");
        }
        if (expiresAt.isBefore(today)) {
            throw new NoticeException("만료일이 이미 지났습니다. 새 만료일을 지정한 뒤 승인하세요: " + expiresAt,
                    NoticeException.ErrorType.BAD_REQUEST);
        }
        this.status = NoticeStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = now;
        this.updatedAt = now;
    }

    /** 만료일이 지난 APPROVED 를 EXPIRED 로. 바뀌었으면 true. */
    public boolean expire(LocalDate today) {
        if (status != NoticeStatus.APPROVED || !expiresAt.isBefore(today)) {
            return false;
        }
        this.status = NoticeStatus.EXPIRED;
        return true;
    }

    /** 만료 {@code daysAhead}일 전부터, 아직 알리지 않은 APPROVED 공지. 알림은 만료일마다 한 번만 보낸다. */
    public boolean needsExpiryWarning(LocalDate today, int daysAhead) {
        return status == NoticeStatus.APPROVED
                && expiryWarnedAt == null
                && !expiresAt.isBefore(today)
                && !expiresAt.isAfter(today.plusDays(daysAhead));
    }

    public void markExpiryWarned(Instant now) {
        this.expiryWarnedAt = now;
    }

    public void archive() {
        this.status = NoticeStatus.ARCHIVED;
    }

    // ── 번역 ──────────────────────────────────────────────────────────────────

    public NoticeTranslation putTranslation(NoticeLang lang, String title, String body, Instant now) {
        assertNotArchived();
        if (lang == sourceLang) {
            throw new NoticeException("원문 언어(" + sourceLang.code() + ")에는 번역을 넣지 않습니다",
                    NoticeException.ErrorType.BAD_REQUEST);
        }
        NoticeTranslation existing = translations.get(lang);
        if (existing != null) {
            existing.replace(title, body, version, now);
            return existing;
        }
        NoticeTranslation created = NoticeTranslation.machine(lang, title, body, version, now);
        translations.put(lang, created);
        return created;
    }

    public void reviewTranslation(NoticeLang lang) {
        NoticeTranslation t = translations.get(lang);
        if (t == null || t.isStaleFor(version)) {
            throw new NoticeException("최신 번역이 없어 검토 표시를 할 수 없습니다: " + lang.code(),
                    NoticeException.ErrorType.INVALID_STATE);
        }
        t.markReviewed();
    }

    /** 생성해야 할 번역 언어: 대상 중 원문 언어가 아니고, 번역이 없거나 stale 인 것. */
    public List<NoticeLang> translationsToGenerate(Set<NoticeLang> targets) {
        return targets.stream()
                .filter(l -> l != sourceLang)
                .filter(l -> translations.get(l) == null || translations.get(l).isStaleFor(version))
                .sorted()
                .toList();
    }

    public Optional<NoticeTranslation> getTranslation(NoticeLang lang) {
        return Optional.ofNullable(translations.get(lang));
    }

    /** 답변 근거 자격. 설계 §4.3 — 이 판정은 여기 한 곳에만 둔다. */
    public boolean isGroundingEligible(LocalDate today, NoticeLang lang) {
        if (status != NoticeStatus.APPROVED) return false;
        if (today.isBefore(effectiveFrom) || today.isAfter(expiresAt)) return false;
        if (lang == sourceLang) return true;
        NoticeTranslation t = translations.get(lang);
        return t != null && !t.isStaleFor(version);
    }

    public boolean isActiveOn(LocalDate today) {
        return status == NoticeStatus.APPROVED && !today.isBefore(effectiveFrom) && !today.isAfter(expiresAt);
    }

    // ── 내부 ──────────────────────────────────────────────────────────────────

    private void assertNotArchived() {
        if (status == NoticeStatus.ARCHIVED) {
            throw new NoticeException("보관된 공지는 변경할 수 없습니다", NoticeException.ErrorType.INVALID_STATE);
        }
    }

    private static void validateDates(LocalDate effectiveFrom, LocalDate expiresAt) {
        Objects.requireNonNull(effectiveFrom, "시작일은 필수입니다");
        Objects.requireNonNull(expiresAt, "만료일은 필수입니다");
        if (!expiresAt.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("만료일은 시작일보다 뒤여야 합니다: " + effectiveFrom + " ~ " + expiresAt);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "은(는) 비어 있을 수 없습니다");
        }
        return value.trim();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getId()                        { return id; }
    public Long getOfficeId()                  { return officeId; }
    public NoticeCategory getCategory()        { return category; }
    public String getTitle()                   { return title; }
    public String getBody()                    { return body; }
    public NoticeLang getSourceLang()          { return sourceLang; }
    public String getSourceUrl()               { return sourceUrl; }
    public List<Long> getAttachmentMediaIds()  { return Collections.unmodifiableList(attachmentMediaIds); }
    public String getTerm()                    { return term; }
    public LocalDate getEffectiveFrom()        { return effectiveFrom; }
    public LocalDate getExpiresAt()            { return expiresAt; }
    public NoticeStatus getStatus()            { return status; }
    public String getApprovedBy()              { return approvedBy; }
    public Instant getApprovedAt()             { return approvedAt; }
    public Instant getExpiryWarnedAt()         { return expiryWarnedAt; }
    public int getVersion()                    { return version; }
    public Long getCreatedBy()                 { return createdBy; }
    public Instant getCreatedAt()              { return createdAt; }
    public Instant getUpdatedAt()              { return updatedAt; }
    public List<NoticeTranslation> getTranslations() { return List.copyOf(translations.values()); }
}
