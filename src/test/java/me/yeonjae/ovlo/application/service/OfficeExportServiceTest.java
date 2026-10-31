package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.dto.result.InquiryLogResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.application.port.in.office.GetInquiryQuery;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.service.office.CsvWriter;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OfficeExportServiceTest {

    private static final Instant T = Instant.parse("2027-03-02T00:00:00Z");

    @Mock GetInquiryQuery inquiryQuery;
    @Mock GetConsultationQuery consultationQuery;
    @Mock GetNoticeQuery noticeQuery;

    @Test
    @DisplayName("문의 CSV: 헤더 + 행, 질문의 쉼표는 감싸고 인용 id 는 ; 로 잇는다. 회원 id 열은 없다")
    void inquiriesCsv() {
        given(inquiryQuery.findForStaff(eq(5L), isNull(), isNull(), isNull(), any())).willReturn(new PageImpl<>(List.of(
                new InquiryLogResult(1L, NoticeLang.EN, "Dorm, when?", InquiryOutcome.ANSWERED, "Feb 10 [1]",
                        List.of(7L, 8L), 3, null, 800L, "claude-test", Boolean.TRUE, null))));

        String csv = new OfficeExportService(inquiryQuery, consultationQuery, noticeQuery).inquiriesCsv(5L, null, null);

        assertThat(csv).startsWith(CsvWriter.BOM + "id,askedAt,lang,outcome,question,answerText,citedNoticeIds,candidateCount,latencyMs,modelId,helpful,feedbackReason\r\n");
        assertThat(csv).contains("1,,EN,ANSWERED,\"Dorm, when?\",Feb 10 [1],7;8,3,800,claude-test,true,\r\n");
        assertThat(csv).doesNotContainIgnoringCase("memberId").doesNotContain("@");
    }

    @Test
    @DisplayName("상담·공지 CSV 도 헤더와 행을 만든다")
    void consultationsAndNoticesCsv() {
        given(consultationQuery.findForStaff(eq(5L), isNull(), any())).willReturn(new PageImpl<>(List.of(
                new ConsultationResult(3L, 5L, ConsultationStatus.RESOLVED, 1L, "Q", "A", NoticeLang.VI, true, 9L,
                        11L, "staff", "v1", T, T.plusSeconds(60), T.plusSeconds(3600), null))));
        given(noticeQuery.findForStaff(eq(5L), isNull(), isNull(), any())).willReturn(new PageImpl<>(List.of(
                new NoticeSummaryResult(2L, NoticeCategory.HOUSING, "기숙사", "2027-1", LocalDate.of(2027, 2, 1),
                        LocalDate.of(2027, 3, 15), NoticeStatus.APPROVED, 1, 2, 4, T))));
        var service = new OfficeExportService(inquiryQuery, consultationQuery, noticeQuery);

        assertThat(service.consultationsCsv(5L)).contains("3,RESOLVED,2027-03-02T00:00:00Z,2027-03-02T00:01:00Z,2027-03-02T01:00:00Z,,VI,true,staff,Q,v1\r\n");
        assertThat(service.noticesCsv(5L)).contains("2,HOUSING,기숙사,2027-1,2027-02-01,2027-03-15,APPROVED,1,2,4,2027-03-02T00:00:00Z\r\n");
    }
}
