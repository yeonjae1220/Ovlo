package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoadNoticePort {
    Optional<Notice> findById(Long noticeId);

    /** 담당자 목록. status·category 는 null 이면 전체. */
    Page<Notice> findByOffice(Long officeId, NoticeStatus status, NoticeCategory category, Pageable pageable);

    /** APPROVED 이고 today 가 기간 안인 공지(번역 포함). 학생 조회와 S3 근거 후보의 1차 필터. */
    List<Notice> findActiveForStudent(Long officeId, NoticeCategory category, LocalDate today);

    /** APPROVED 이면서 expiresAt < today 인 공지 id. */
    List<Long> findIdsToExpire(LocalDate today);

    /** APPROVED, 미알림, expiresAt ∈ [today, today+daysAhead] 인 공지 id. */
    List<Long> findIdsExpiringWithin(LocalDate today, int daysAhead);
}
