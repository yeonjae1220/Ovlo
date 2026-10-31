package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.time.LocalDate;
import java.util.List;

/**
 * 학생용 공지. lang 은 실제로 보여 주는 언어(요청 언어에 최신 번역이 없으면 원문 언어로 폴백).
 * machineTranslated 가 true 면 "기계 번역" 표시를 붙인다.
 */
public record StudentNoticeResult(Long id, NoticeCategory category, String title, String body, NoticeLang lang,
                                  boolean machineTranslated, String sourceUrl, List<Long> attachmentMediaIds,
                                  String term, LocalDate effectiveFrom, LocalDate expiresAt) {
}
