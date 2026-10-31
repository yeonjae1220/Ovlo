package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 대시보드 집계(설계 §5.4). 기간은 [from, to). 어댑터는 집계 쿼리만 하고 해석은 서비스가 한다. */
public interface LoadOfficeStatsPort {

    record InquiryCounts(long total, long answered, long noGrounds, long escalated) {}

    record FeedbackCounts(long helpful, long unhelpful) {}

    record ConsultationCounts(long open, long inProgress, long resolved, long closed) {}

    InquiryCounts countInquiries(Long officeId, Instant from, Instant to);

    FeedbackCounts countFeedback(Long officeId, Instant from, Instant to);

    /** 반복 문의 묶기용 질문 본문. outcome 이 null 이면 전체. 최신순, limit 까지. */
    List<String> findQuestions(Long officeId, InquiryOutcome outcome, Instant from, Instant to, int limit);

    ConsultationCounts countConsultations(Long officeId, Instant from, Instant to);

    /** 담당자 첫 답변까지 평균 분. 답변이 없는 상담은 제외. 대상이 없으면 null. */
    Double averageFirstReplyMinutes(Long officeId, Instant from, Instant to);

    /** 열림→해결 평균 시간. 대상이 없으면 null. */
    Double averageResolveHours(Long officeId, Instant from, Instant to);

    Map<PartnerCategory, Long> countPartnerClicks(Long officeId, Instant from, Instant to);

    /** APPROVED 이고 만료일이 [today, today+days] 인 공지. */
    List<Notice> findNoticesExpiringWithin(Long officeId, LocalDate today, int days);

    long countNotices(Long officeId, me.yeonjae.ovlo.domain.office.model.NoticeStatus status);
}
