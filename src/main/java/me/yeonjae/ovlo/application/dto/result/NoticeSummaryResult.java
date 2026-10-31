package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;

import java.time.Instant;
import java.time.LocalDate;

/** 담당자 목록 행. */
public record NoticeSummaryResult(Long id, NoticeCategory category, String title, String term, LocalDate effectiveFrom,
                                  LocalDate expiresAt, NoticeStatus status, int version, int translatedLangCount,
                                  int missingLangCount, Instant updatedAt) {
}
