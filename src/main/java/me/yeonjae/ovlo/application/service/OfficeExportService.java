package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.dto.result.InquiryLogResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.port.in.office.ExportOfficeDataQuery;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.application.port.in.office.GetInquiryQuery;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.service.office.CsvWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * CSV 내보내기. 기존 조회 유스케이스의 결과 DTO 만 쓴다 — 그 DTO 에 학생 식별 정보가 없다는 것이
 * 이미 테스트로 고정돼 있어 여기서 새로 샐 길이 없다. 파일럿 규모라 한 페이지(상한 MAX_ROWS)로 끝낸다.
 */
@Service
@Transactional(readOnly = true)
public class OfficeExportService implements ExportOfficeDataQuery {

    static final int MAX_ROWS = 5000;

    private final GetInquiryQuery inquiryQuery;
    private final GetConsultationQuery consultationQuery;
    private final GetNoticeQuery noticeQuery;

    public OfficeExportService(GetInquiryQuery inquiryQuery, GetConsultationQuery consultationQuery,
                               GetNoticeQuery noticeQuery) {
        this.inquiryQuery = inquiryQuery;
        this.consultationQuery = consultationQuery;
        this.noticeQuery = noticeQuery;
    }

    @Override
    public String inquiriesCsv(Long officeId, LocalDate from, LocalDate to) {
        Instant f = from == null ? null : from.atStartOfDay(OfficeDashboardService.OFFICE_ZONE).toInstant();
        Instant t = to == null ? null : to.plusDays(1).atStartOfDay(OfficeDashboardService.OFFICE_ZONE).toInstant();
        List<InquiryLogResult> rows = inquiryQuery.findForStaff(officeId, null, f, t,
                PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "askedAt"))).getContent();
        CsvWriter csv = new CsvWriter().header("id", "askedAt", "lang", "outcome", "question", "answerText",
                "citedNoticeIds", "candidateCount", "latencyMs", "modelId", "helpful", "feedbackReason");
        for (InquiryLogResult r : rows) {
            csv.row(r.id(), r.askedAt(), r.lang(), r.outcome(), r.question(), r.answerText(),
                    joinIds(r.citedNoticeIds()), r.candidateCount(), r.latencyMs(), r.modelId(), r.helpful(), r.feedbackReason());
        }
        return csv.build();
    }

    @Override
    public String consultationsCsv(Long officeId) {
        List<ConsultationResult> rows = consultationQuery.findForStaff(officeId, null,
                PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "openedAt"))).getContent();
        CsvWriter csv = new CsvWriter().header("id", "status", "openedAt", "firstStaffReplyAt", "resolvedAt", "closedAt",
                "lang", "verifiedStudent", "assignedStaffNickname", "question", "consentVersion");
        for (ConsultationResult r : rows) {
            csv.row(r.id(), r.status(), r.openedAt(), r.firstStaffReplyAt(), r.resolvedAt(), r.closedAt(),
                    r.lang(), r.verifiedStudent(), r.assignedStaffNickname(), r.question(), r.consentVersion());
        }
        return csv.build();
    }

    @Override
    public String noticesCsv(Long officeId) {
        List<NoticeSummaryResult> rows = noticeQuery.findForStaff(officeId, null, null,
                PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();
        CsvWriter csv = new CsvWriter().header("id", "category", "title", "term", "effectiveFrom", "expiresAt", "status",
                "version", "translatedLangCount", "missingLangCount", "updatedAt");
        for (NoticeSummaryResult r : rows) {
            csv.row(r.id(), r.category(), r.title(), r.term(), r.effectiveFrom(), r.expiresAt(), r.status(),
                    r.version(), r.translatedLangCount(), r.missingLangCount(), r.updatedAt());
        }
        return csv.build();
    }

    private static String joinIds(List<Long> ids) {
        return ids == null ? "" : ids.stream().map(String::valueOf).reduce((a, b) -> a + ";" + b).orElse("");
    }
}
