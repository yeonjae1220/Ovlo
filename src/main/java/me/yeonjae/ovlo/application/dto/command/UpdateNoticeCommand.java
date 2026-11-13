package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeCategory;

import java.time.LocalDate;
import java.util.List;

/** 본문·출처·첨부·학기·기간 변경은 새 내용 버전과 재승인을 요구한다. revision은 DB 경합 검사 값이다. */
public record UpdateNoticeCommand(Long officeId, Long noticeId, NoticeCategory category, String title, String body,
                                  String sourceUrl, List<Long> attachmentMediaIds, String term,
                                  LocalDate effectiveFrom, LocalDate expiresAt, Long revision, Long actor) {
    public UpdateNoticeCommand(Long officeId,Long noticeId,NoticeCategory category,String title,String body,String sourceUrl,
                               List<Long> attachments,String term,LocalDate from,LocalDate until) {
        this(officeId,noticeId,category,title,body,sourceUrl,attachments,term,from,until,null,null);
    }
}
