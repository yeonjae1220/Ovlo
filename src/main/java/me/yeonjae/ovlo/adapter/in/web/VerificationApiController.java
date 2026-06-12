package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.ConfirmEmailVerificationRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.RequestEmailVerificationRequest;
import me.yeonjae.ovlo.application.dto.command.ConfirmSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.command.RequestSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.VerificationRequestResult;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.port.in.verification.ConfirmSchoolEmailVerificationUseCase;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.in.verification.RequestSchoolEmailVerificationUseCase;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Verification", description = "학생 인증 — 학교/파견 대학 이메일 인증")
@RestController
@RequestMapping("/api/v1/verification")
public class VerificationApiController {

    private final RequestSchoolEmailVerificationUseCase requestUseCase;
    private final ConfirmSchoolEmailVerificationUseCase confirmUseCase;
    private final GetMyVerificationStatusQuery statusQuery;
    private final RateLimiterService rateLimiter;

    public VerificationApiController(RequestSchoolEmailVerificationUseCase requestUseCase,
                                     ConfirmSchoolEmailVerificationUseCase confirmUseCase,
                                     GetMyVerificationStatusQuery statusQuery,
                                     RateLimiterService rateLimiter) {
        this.requestUseCase = requestUseCase;
        this.confirmUseCase = confirmUseCase;
        this.statusQuery = statusQuery;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "학교 이메일 인증 코드 발송",
            description = "지정한 대학의 학교 이메일로 6자리 코드를 발송. 도메인이 지정 대학과 일치해야 함.")
    @PostMapping("/email/request")
    public ResponseEntity<VerificationRequestResult> requestCode(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RequestEmailVerificationRequest body) {
        rateLimiter.checkEmailVerificationRate(memberId);
        return ResponseEntity.ok(requestUseCase.request(
                new RequestSchoolEmailVerificationCommand(memberId, body.universityId(), body.schoolEmail())));
    }

    @Operation(summary = "인증 코드 확인", description = "발송된 코드를 확인해 인증 자격을 발급한다.")
    @PostMapping("/email/confirm")
    public ResponseEntity<VerificationStatusResult> confirmCode(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ConfirmEmailVerificationRequest body) {
        return ResponseEntity.ok(confirmUseCase.confirm(
                new ConfirmSchoolEmailVerificationCommand(memberId, body.code())));
    }

    @Operation(summary = "내 인증 현황", description = "본인의 신뢰 등급과 인증된 대학 목록.")
    @GetMapping("/me")
    public ResponseEntity<VerificationStatusResult> myStatus(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(statusQuery.getByMemberId(memberId));
    }
}
