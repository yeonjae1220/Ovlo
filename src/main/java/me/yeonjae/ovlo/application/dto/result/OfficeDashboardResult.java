package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 담당자 대시보드(설계 §5.4). 기간 [from, to] 은 날짜 단위(KST). 학생 식별 정보는 없다.
 * 비율·평균은 대상이 없으면 null — 0 으로 보이면 "안 좋음"으로 오독된다.
 */
public record OfficeDashboardResult(LocalDate from, LocalDate to,
                                    Inquiries inquiries, Feedback feedback, Consultations consultations, Notices notices,
                                    Map<PartnerCategory, Long> partnerClicks,
                                    List<RepeatedQuestion> repeatedQuestions,
                                    List<RepeatedQuestion> noGroundsQuestions) {

    public record Inquiries(long total, long answered, long noGrounds, long escalated, Double answerRate) {}

    public record Feedback(long helpful, long unhelpful, Double helpfulRate) {}

    public record Consultations(long open, long inProgress, long resolved, long closed,
                                Double avgFirstReplyMinutes, Double avgResolveHours) {}

    public record Notices(long approved, long draft, List<ExpiringNotice> expiringSoon) {}

    public record ExpiringNotice(Long id, String title, LocalDate expiresAt) {}

    /** 같은 질문으로 묶인 그룹. sample 은 가장 최근 원문, count 는 건수. */
    public record RepeatedQuestion(String sample, long count) {}
}
