package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 담당자용 공지 상세. missingLangs: 번역이 없거나 stale 이라 그 언어 학생에게는 근거로 쓰이지 않는 언어. */
public record NoticeResult(Long id, Long officeId, NoticeCategory category, String title, String body,
                           NoticeLang sourceLang, String sourceUrl, List<Long> attachmentMediaIds, String term,
                           LocalDate effectiveFrom, LocalDate expiresAt, NoticeStatus status, String approvedBy,
                           Instant approvedAt, int version, boolean activeToday, Instant createdAt, Instant updatedAt,
                           List<NoticeTranslationResult> translations, List<NoticeLang> missingLangs) {
}
