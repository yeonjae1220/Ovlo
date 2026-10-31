package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.AskQuestionRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.InquiryFeedbackRequest;
import me.yeonjae.ovlo.application.dto.command.AskQuestionCommand;
import me.yeonjae.ovlo.application.dto.command.InquiryFeedbackCommand;
import me.yeonjae.ovlo.application.dto.result.AskResult;
import me.yeonjae.ovlo.application.port.in.office.AskQuestionUseCase;
import me.yeonjae.ovlo.application.port.in.office.GiveInquiryFeedbackUseCase;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 학생 질문·피드백. 회원 단위 요청 제한(분·일)은 여기서, Office 일일 상한과 소속 검사는 유스케이스에서. */
@Tag(name = "Office", description = "국제처 — 학생 질문")
@RestController
@RequestMapping("/api/v1/office/{officeId}/inquiries")
public class StudentInquiryApiController {

    private final AskQuestionUseCase askQuestionUseCase;
    private final GiveInquiryFeedbackUseCase feedbackUseCase;
    private final RateLimiterService rateLimiter;

    public StudentInquiryApiController(AskQuestionUseCase askQuestionUseCase, GiveInquiryFeedbackUseCase feedbackUseCase,
                                       RateLimiterService rateLimiter) {
        this.askQuestionUseCase = askQuestionUseCase;
        this.feedbackUseCase = feedbackUseCase;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "질문 — 승인 자료만 근거로 답하고 근거가 없으면 NO_GROUNDS")
    @PostMapping
    public ResponseEntity<AskResult> ask(@PathVariable Long officeId, @Valid @RequestBody AskQuestionRequest request,
                                         @AuthenticationPrincipal Long memberId) {
        rateLimiter.checkOfficeInquiryRate(memberId);
        NoticeLang lang = NoticeLang.fromCode(request.lang()).orElse(NoticeLang.KO);
        AskResult result = askQuestionUseCase.ask(new AskQuestionCommand(officeId, memberId, request.question(), lang));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "답변 피드백 👍/👎 (본인 문의만)")
    @PostMapping("/{inquiryId}/feedback")
    public ResponseEntity<Void> feedback(@PathVariable Long officeId, @PathVariable Long inquiryId,
                                         @Valid @RequestBody InquiryFeedbackRequest request,
                                         @AuthenticationPrincipal Long memberId) {
        feedbackUseCase.feedback(new InquiryFeedbackCommand(officeId, inquiryId, memberId, request.helpful(), request.reason()));
        return ResponseEntity.noContent().build();
    }
}
