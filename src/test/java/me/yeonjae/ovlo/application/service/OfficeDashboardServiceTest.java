package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort.ConsultationCounts;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort.FeedbackCounts;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort.InquiryCounts;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfficeDashboardServiceTest {

    private static final long OFFICE = 5L;
    /** 2027-03-02 09:00 KST */
    private static final Instant NOW = Instant.parse("2027-03-02T00:00:00Z");

    @Mock LoadOfficeStatsPort statsPort;

    private OfficeDashboardService service;

    @BeforeEach
    void setUp() {
        service = new OfficeDashboardService(statsPort, Clock.fixed(NOW, ZoneOffset.UTC), 7);
    }

    @Test
    @DisplayName("기본 기간은 KST 오늘까지 30일이고, 비율·반복 문의·만료 임박을 함께 돌려준다")
    void defaultRangeAndRates() {
        LocalDate today = LocalDate.of(2027, 3, 2);
        Instant from = LocalDate.of(2027, 2, 1).atStartOfDay(OfficeDashboardService.OFFICE_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(OfficeDashboardService.OFFICE_ZONE).toInstant();
        given(statsPort.countInquiries(OFFICE, from, to)).willReturn(new InquiryCounts(10, 7, 2, 1));
        given(statsPort.countFeedback(OFFICE, from, to)).willReturn(new FeedbackCounts(3, 1));
        given(statsPort.countConsultations(OFFICE, from, to)).willReturn(new ConsultationCounts(1, 2, 3, 4));
        given(statsPort.averageFirstReplyMinutes(OFFICE, from, to)).willReturn(42.0);
        given(statsPort.averageResolveHours(OFFICE, from, to)).willReturn(null);
        given(statsPort.countNotices(OFFICE, NoticeStatus.APPROVED)).willReturn(12L);
        given(statsPort.countNotices(OFFICE, NoticeStatus.DRAFT)).willReturn(2L);
        given(statsPort.countPartnerClicks(OFFICE, from, to)).willReturn(Map.of(PartnerCategory.SIM, 5L));
        Notice expiring = Notice.register(OFFICE, NoticeCategory.HOUSING, "기숙사", "본문", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 5), 1L, NOW);
        given(statsPort.findNoticesExpiringWithin(OFFICE, today, 7)).willReturn(List.of(expiring));
        given(statsPort.findQuestions(eq(OFFICE), isNull(), eq(from), eq(to), anyInt()))
                .willReturn(List.of("비자 연장?", "비자 연장", "기숙사"));
        given(statsPort.findQuestions(eq(OFFICE), eq(InquiryOutcome.NO_GROUNDS), eq(from), eq(to), anyInt()))
                .willReturn(List.of("알바 가능?", "알바 가능"));

        OfficeDashboardResult r = service.dashboard(OFFICE, null, null);

        assertThat(r.from()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(r.to()).isEqualTo(today);
        assertThat(r.inquiries().answerRate()).isEqualTo(0.7);
        assertThat(r.feedback().helpfulRate()).isEqualTo(0.75);
        assertThat(r.consultations().avgFirstReplyMinutes()).isEqualTo(42.0);
        assertThat(r.consultations().avgResolveHours()).isNull();
        assertThat(r.notices().approved()).isEqualTo(12);
        assertThat(r.notices().expiringSoon()).extracting(OfficeDashboardResult.ExpiringNotice::title).containsExactly("기숙사");
        assertThat(r.partnerClicks()).containsEntry(PartnerCategory.SIM, 5L);
        assertThat(r.repeatedQuestions()).containsExactly(new OfficeDashboardResult.RepeatedQuestion("비자 연장?", 2));
        assertThat(r.noGroundsQuestions()).containsExactly(new OfficeDashboardResult.RepeatedQuestion("알바 가능?", 2));
    }

    @Test
    @DisplayName("문의가 없으면 비율은 0 이 아니라 null 이다")
    void ratesNullWhenEmpty() {
        given(statsPort.countInquiries(eq(OFFICE), any(), any())).willReturn(new InquiryCounts(0, 0, 0, 0));
        given(statsPort.countFeedback(eq(OFFICE), any(), any())).willReturn(new FeedbackCounts(0, 0));
        given(statsPort.countConsultations(eq(OFFICE), any(), any())).willReturn(new ConsultationCounts(0, 0, 0, 0));
        given(statsPort.findNoticesExpiringWithin(eq(OFFICE), any(), anyInt())).willReturn(List.of());
        given(statsPort.findQuestions(eq(OFFICE), any(), any(), any(), anyInt())).willReturn(List.of());

        OfficeDashboardResult r = service.dashboard(OFFICE, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31));

        assertThat(r.inquiries().answerRate()).isNull();
        assertThat(r.feedback().helpfulRate()).isNull();
        verify(statsPort).countInquiries(OFFICE,
                LocalDate.of(2027, 1, 1).atStartOfDay(OfficeDashboardService.OFFICE_ZONE).toInstant(),
                LocalDate.of(2027, 2, 1).atStartOfDay(OfficeDashboardService.OFFICE_ZONE).toInstant());
    }

    @Test
    @DisplayName("시작이 끝보다 늦거나 1년을 넘는 기간은 거부한다")
    void rejectsBadRange() {
        assertThatThrownBy(() -> service.dashboard(OFFICE, LocalDate.of(2027, 3, 2), LocalDate.of(2027, 3, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.dashboard(OFFICE, LocalDate.of(2025, 1, 1), LocalDate.of(2027, 3, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
