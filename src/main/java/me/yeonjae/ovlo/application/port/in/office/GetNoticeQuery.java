package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.dto.result.StudentNoticeResult;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GetNoticeQuery {
    Page<NoticeSummaryResult> findForStaff(Long officeId, NoticeStatus status, NoticeCategory category, Pageable pageable);

    /** 다른 Office 의 공지는 없는 것으로 취급한다. */
    Optional<NoticeResult> findDetail(Long officeId, Long noticeId);

    /** 오늘 유효한 승인 공지를 학생 언어로. 번역이 없으면 원문으로 폴백. */
    List<StudentNoticeResult> findForStudent(Long officeId, NoticeCategory category, NoticeLang lang);

    Optional<StudentNoticeResult> findStudentDetail(Long officeId, Long noticeId, NoticeLang lang);
}
