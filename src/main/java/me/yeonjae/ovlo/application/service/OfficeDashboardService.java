package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult;
import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.Consultations;
import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.ExpiringNotice;
import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.Feedback;
import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.Inquiries;
import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.Notices;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeDashboardQuery;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort;
import me.yeonjae.ovlo.application.service.office.InquiryClustering;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** 대시보드 집계(설계 §5.4). 쿼리는 포트가, 해석(비율·묶기)은 여기서. */
@Service
@Transactional(readOnly = true)
public class OfficeDashboardService implements GetOfficeDashboardQuery {

    /** 국제처 업무 기준 시간대. 기간 경계와 "오늘" 판정에 쓴다. */
    static final ZoneId OFFICE_ZONE = ZoneId.of("Asia/Seoul");
    static final int DEFAULT_RANGE_DAYS = 30;
    static final int MAX_RANGE_DAYS = 366;
    static final int CLUSTER_SAMPLE_LIMIT = 2000;
    static final int CLUSTER_TOP = 10;

    private final LoadOfficeStatsPort statsPort;
    private final Clock clock;
    private final int expiryWarnDays;

    public OfficeDashboardService(LoadOfficeStatsPort statsPort, Clock clock,
                                  @Value("${ovlo.office.notice-expiry-warn-days:7}") int expiryWarnDays) {
        this.statsPort = statsPort;
        this.clock = clock;
        this.expiryWarnDays = expiryWarnDays;
    }

    @Override
    public OfficeDashboardResult dashboard(Long officeId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock.withZone(OFFICE_ZONE));
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("기간의 시작이 끝보다 늦습니다: " + start + " > " + end);
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new IllegalArgumentException("기간은 최대 " + MAX_RANGE_DAYS + "일까지입니다");
        }
        Instant f = start.atStartOfDay(OFFICE_ZONE).toInstant();
        Instant t = end.plusDays(1).atStartOfDay(OFFICE_ZONE).toInstant();

        var inq = statsPort.countInquiries(officeId, f, t);
        var fb = statsPort.countFeedback(officeId, f, t);
        var con = statsPort.countConsultations(officeId, f, t);
        List<ExpiringNotice> expiring = statsPort.findNoticesExpiringWithin(officeId, today, expiryWarnDays).stream()
                .map(n -> new ExpiringNotice(n.getId(), n.getTitle(), n.getExpiresAt()))
                .toList();

        return new OfficeDashboardResult(start, end,
                new Inquiries(inq.total(), inq.answered(), inq.noGrounds(), inq.escalated(),
                        rate(inq.answered(), inq.total())),
                new Feedback(fb.helpful(), fb.unhelpful(), rate(fb.helpful(), fb.helpful() + fb.unhelpful())),
                new Consultations(con.open(), con.inProgress(), con.resolved(), con.closed(),
                        statsPort.averageFirstReplyMinutes(officeId, f, t), statsPort.averageResolveHours(officeId, f, t)),
                new Notices(statsPort.countNotices(officeId, NoticeStatus.APPROVED),
                        statsPort.countNotices(officeId, NoticeStatus.DRAFT), expiring),
                statsPort.countPartnerClicks(officeId, f, t),
                InquiryClustering.cluster(statsPort.findQuestions(officeId, null, f, t, CLUSTER_SAMPLE_LIMIT), CLUSTER_TOP),
                InquiryClustering.cluster(statsPort.findQuestions(officeId, InquiryOutcome.NO_GROUNDS, f, t, CLUSTER_SAMPLE_LIMIT),
                        CLUSTER_TOP));
    }

    private static Double rate(long part, long whole) {
        return whole == 0 ? null : (double) part / whole;
    }
}
