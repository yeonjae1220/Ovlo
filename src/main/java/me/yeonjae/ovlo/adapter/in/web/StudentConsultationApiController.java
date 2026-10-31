package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.OpenConsultationRequest;
import me.yeonjae.ovlo.application.dto.command.ConsultationActionCommand;
import me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand;
import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.dto.result.TranscriptMessageResult;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationTranscriptQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageConsultationUseCase;
import me.yeonjae.ovlo.application.port.in.office.OpenConsultationUseCase;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 학생의 상담 이관. 소속 검사는 열기 때 유스케이스가, 그 뒤는 본인 소유 여부로 한다. */
@Tag(name = "Office", description = "국제처 — 학생 상담 이관")
@RestController
@RequestMapping("/api/v1/office/{officeId}/consultations")
public class StudentConsultationApiController {

    private final OpenConsultationUseCase openUseCase;
    private final ManageConsultationUseCase manageUseCase;
    private final GetConsultationQuery getQuery;
    private final GetConsultationTranscriptQuery transcriptQuery;

    public StudentConsultationApiController(OpenConsultationUseCase openUseCase, ManageConsultationUseCase manageUseCase,
                                            GetConsultationQuery getQuery, GetConsultationTranscriptQuery transcriptQuery) {
        this.openUseCase = openUseCase;
        this.manageUseCase = manageUseCase;
        this.getQuery = getQuery;
        this.transcriptQuery = transcriptQuery;
    }

    @Operation(summary = "담당자에게 전달 — 동의 버전 필수. 학생 + 활동 담당자 채팅방이 생긴다")
    @PostMapping
    public ResponseEntity<ConsultationResult> open(@PathVariable Long officeId, @Valid @RequestBody OpenConsultationRequest request,
                                                   @AuthenticationPrincipal Long memberId) {
        ConsultationResult result = openUseCase.open(new OpenConsultationCommand(officeId, memberId, request.inquiryId(),
                request.consentVersion(), request.message()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "내 상담 목록")
    @GetMapping
    public ResponseEntity<List<ConsultationResult>> mine(@PathVariable Long officeId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(getQuery.findMine(officeId, memberId));
    }

    @Operation(summary = "내 상담 상세")
    @GetMapping("/{consultationId}")
    public ResponseEntity<ConsultationResult> get(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                  @AuthenticationPrincipal Long memberId) {
        return getQuery.findMine(officeId, consultationId, memberId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ConsultationException("상담을 찾을 수 없습니다", ConsultationException.ErrorType.NOT_FOUND));
    }

    @Operation(summary = "상담 종료 (본인)")
    @PatchMapping("/{consultationId}/close")
    public ResponseEntity<ConsultationResult> close(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                    @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(manageUseCase.close(new ConsultationActionCommand(officeId, consultationId, memberId)));
    }

    @Operation(summary = "상담 대화 (내 언어로 번역 병기)")
    @GetMapping("/{consultationId}/messages")
    public ResponseEntity<List<TranscriptMessageResult>> messages(@PathVariable Long officeId, @PathVariable Long consultationId,
                                                                  @RequestParam(defaultValue = "ko") String lang,
                                                                  @RequestParam(defaultValue = "100") int limit,
                                                                  @AuthenticationPrincipal Long memberId) {
        NoticeLang noticeLang = NoticeLang.fromCode(lang).orElse(NoticeLang.KO);
        return ResponseEntity.ok(transcriptQuery.transcript(officeId, consultationId, memberId, false, noticeLang, limit));
    }
}
