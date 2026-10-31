package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeCategory;

import java.time.LocalDate;
import java.util.List;

public record RegisterNoticeCommand(Long officeId, NoticeCategory category, String title, String body, String sourceUrl,
                                    List<Long> attachmentMediaIds, String term, LocalDate effectiveFrom,
                                    LocalDate expiresAt, Long createdBy) {
}
