package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeCategory;

import java.time.LocalDate;
import java.util.List;

/** 제목·본문이 바뀌면 도메인이 version+1·DRAFT 로 되돌린다. 날짜·학기만 바꾸면 승인 상태는 유지된다. */
public record UpdateNoticeCommand(Long officeId, Long noticeId, NoticeCategory category, String title, String body,
                                  String sourceUrl, List<Long> attachmentMediaIds, String term,
                                  LocalDate effectiveFrom, LocalDate expiresAt) {
}
