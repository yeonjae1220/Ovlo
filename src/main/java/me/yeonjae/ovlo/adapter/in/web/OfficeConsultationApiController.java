package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.command.ConsultationActionCommand;
import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.dto.result.TranscriptMessageResult;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationTranscriptQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageConsultationUseCase;
import me.yeonjae.ovlo.application.service.office.OfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.security.OfficePrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 담당자 받은함. 모든 메서드 첫 줄에서 OfficeAccessPolicy 로 범위를 확인한다. */
@Tag(name = "Office Admin", description = "국제처 담당자 전용 — 상담")
@RestController
@RequestMapping("/api/v1/office-admin/{officeId}/consultations")
public class OfficeConsultationApiController {

    private final GetConsultationQuery getQuery;
    private final ManageConsultationUseCase manageUseCase;
    private final GetConsultationTranscriptQuery transcriptQuery;
    private final OfficeAccessPolicy accessPolicy;
    private final me.yeonjae.ovlo.shared.metrics.OfficeMetrics metrics;

    public OfficeConsultationApiController(GetConsultationQuery getQuery, ManageConsultationUseCase manageUseCase,
                                           GetConsultationTranscriptQuery transcriptQuery, OfficeAccessPolicy accessPolicy,
                                          me.yeonjae.ovlo.shared.metrics.OfficeMetrics metrics) {
        this.getQuery = getQuery;
        this.metrics = metrics;
        this.manageUseCase = manageUseCase;
        this.transcriptQuery = transcriptQuery;
        this.accessPolicy = accessPolicy;
    }

    @Operation(summary = "받은함 (상태 필터)")
    @GetMapping
    public ResponseEntity<Page<ConsultationResult>> list(@PathVariable Long officeId,
                                                         @RequestParam(required = false) ConsultationStatus status,
                                                         @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                         Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(getQuery.findForStaff(officeId, status, pageable));
    }

    @Operation(summary = "상담 상세")
    @GetMapping("/{consultationId}")
    public ResponseEntity<ConsultationResult> get(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                  Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return getQuery.findForStaff(officeId, consultationId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ConsultationException("상담을 찾을 수 없습니다", ConsultationException.ErrorType.NOT_FOUND));
    }

    @Operation(summary = "맡기 — 호출한 담당자에게 배정하고 채팅방에 넣는다")
    @PostMapping("/{consultationId}/assign")
    public ResponseEntity<ConsultationResult> assign(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                     @AuthenticationPrincipal Long memberId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        metrics.consultation("assigned");
        return ResponseEntity.ok(manageUseCase.assign(new ConsultationActionCommand(officeId, consultationId, memberId)));
    }

    @Operation(summary = "해결 표시")
    @PostMapping("/{consultationId}/resolve")
    public ResponseEntity<ConsultationResult> resolve(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                      @AuthenticationPrincipal Long memberId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        metrics.consultation("resolved");
        return ResponseEntity.ok(manageUseCase.resolve(new ConsultationActionCommand(officeId, consultationId, memberId)));
    }

    @Operation(summary = "상담 대화 (학생 메시지를 한국어 등 요청 언어로 번역 병기)")
    @GetMapping("/{consultationId}/messages")
    public ResponseEntity<List<TranscriptMessageResult>> messages(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                                  @RequestParam(defaultValue = "ko") String lang,
                                                                  @RequestParam(defaultValue = "100") int limit,
                                                                  @AuthenticationPrincipal Long memberId,
                                                                  Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        NoticeLang noticeLang = NoticeLang.fromCode(lang).orElse(NoticeLang.KO);
        return ResponseEntity.ok(transcriptQuery.transcript(officeId, consultationId, memberId, true, noticeLang, limit));
    }
}
