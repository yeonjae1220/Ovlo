package me.yeonjae.ovlo.domain.office.model;

import java.time.Instant;
import java.util.Objects;

/** 공지 번역 1개 언어(Entity, Notice 하위). sourceVersion 이 공지 version 과 다르면 stale 이라 근거로 쓰지 않는다. */
public class NoticeTranslation {

    private final NoticeLang lang;
    private String title;
    private String body;
    private TranslationStatus status;
    private int sourceVersion;
    private Instant generatedAt;

    private NoticeTranslation(NoticeLang lang, String title, String body, TranslationStatus status,
                              int sourceVersion, Instant generatedAt) {
        this.lang = lang;
        this.title = title;
        this.body = body;
        this.status = status;
        this.sourceVersion = sourceVersion;
        this.generatedAt = generatedAt;
    }

    static NoticeTranslation machine(NoticeLang lang, String title, String body, int sourceVersion, Instant now) {
        Objects.requireNonNull(lang, "lang은 필수입니다");
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            throw new IllegalArgumentException("번역 제목·본문은 비어 있을 수 없습니다");
        }
        return new NoticeTranslation(lang, title.trim(), body.trim(), TranslationStatus.MACHINE, sourceVersion, now);
    }

    public static NoticeTranslation restore(NoticeLang lang, String title, String body, TranslationStatus status,
                                            int sourceVersion, Instant generatedAt) {
        return new NoticeTranslation(lang, title, body, status, sourceVersion, generatedAt);
    }

    void replace(String title, String body, int sourceVersion, Instant now) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            throw new IllegalArgumentException("번역 제목·본문은 비어 있을 수 없습니다");
        }
        this.title = title.trim();
        this.body = body.trim();
        this.status = TranslationStatus.MACHINE;
        this.sourceVersion = sourceVersion;
        this.generatedAt = now;
    }

    void markReviewed() {
        this.status = TranslationStatus.REVIEWED;
    }

    public boolean isStaleFor(int noticeVersion) {
        return sourceVersion != noticeVersion;
    }

    public NoticeLang getLang()            { return lang; }
    public String getTitle()               { return title; }
    public String getBody()                { return body; }
    public TranslationStatus getStatus()   { return status; }
    public int getSourceVersion()          { return sourceVersion; }
    public Instant getGeneratedAt()        { return generatedAt; }
}
