package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.result.MyOfficeResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 회원(학생·담당자 공통)의 Office 진입점. 모든 경로는 authenticated(). */
@Tag(name = "Office", description = "국제처 — 내 소속 조회·초대 수락")
@RestController
@RequestMapping("/api/v1/office")
public class OfficeApiController {

    private final GetOfficeQuery getOfficeQuery;
    private final AcceptOfficeInvitationUseCase acceptUseCase;

    public OfficeApiController(GetOfficeQuery getOfficeQuery, AcceptOfficeInvitationUseCase acceptUseCase) {
        this.getOfficeQuery = getOfficeQuery;
        this.acceptUseCase = acceptUseCase;
    }

    @Operation(summary = "내 국제처 — 담당자 소속, 인증 대학의 국제처, 수락 대기 초대 순. 없으면 404")
    @GetMapping("/me")
    public ResponseEntity<MyOfficeResult> me(@AuthenticationPrincipal Long memberId) {
        return getOfficeQuery.findMyOffice(memberId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OfficeException("소속된 국제처가 없습니다", OfficeException.ErrorType.NOT_FOUND));
    }

    @Operation(summary = "담당자 초대 수락. 수락 뒤 토큰을 다시 받아야 담당자 권한이 반영된다")
    @PostMapping("/invitations/accept")
    public ResponseEntity<OfficeMembership> accept(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(acceptUseCase.accept(memberId));
    }
}
