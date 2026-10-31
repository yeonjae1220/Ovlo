package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.InviteOfficeStaffRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdateOfficeStaffRoleRequest;
import me.yeonjae.ovlo.application.dto.command.ChangeOfficeStaffRoleCommand;
import me.yeonjae.ovlo.application.dto.command.InviteOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.command.RemoveOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.service.office.OfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.shared.security.OfficePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 국제처 담당자 화면 API. {@code /api/v1/office-admin/**} 은 SecurityConfig 에서 OFFICE_STAFF 권한이 1차 관문이고,
 * 여기서는 매 요청 첫 줄에서 {@link OfficeAccessPolicy} 로 "이 Office 의" 담당자인지 확인한다(ADR-0005).
 */
@Tag(name = "Office Admin", description = "국제처 담당자 전용 — 담당자 관리")
@RestController
@RequestMapping("/api/v1/office-admin/{officeId}")
public class OfficeStaffApiController {

    private final GetOfficeQuery getOfficeQuery;
    private final ManageOfficeStaffUseCase manageStaffUseCase;
    private final OfficeAccessPolicy accessPolicy;

    public OfficeStaffApiController(GetOfficeQuery getOfficeQuery, ManageOfficeStaffUseCase manageStaffUseCase,
                                    OfficeAccessPolicy accessPolicy) {
        this.getOfficeQuery = getOfficeQuery;
        this.manageStaffUseCase = manageStaffUseCase;
        this.accessPolicy = accessPolicy;
    }

    @Operation(summary = "내 국제처 상세(담당자 목록 포함)")
    @GetMapping
    public ResponseEntity<OfficeResult> get(@PathVariable Long officeId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return getOfficeQuery.findById(officeId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다", OfficeException.ErrorType.NOT_FOUND));
    }

    @Operation(summary = "담당자 초대 (OFFICE_ADMIN). 가입된 회원의 이메일만 가능")
    @PostMapping("/staff")
    public ResponseEntity<OfficeStaffResult> invite(@PathVariable Long officeId,
                                                    @Valid @RequestBody InviteOfficeStaffRequest request,
                                                    @AuthenticationPrincipal Long memberId,
                                                    Authentication authentication) {
        accessPolicy.assertAdmin(OfficePrincipal.membershipOrNull(authentication), officeId);
        OfficeStaffResult result = manageStaffUseCase.invite(
                new InviteOfficeStaffCommand(officeId, request.email(), request.role(), String.valueOf(memberId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "담당자 역할 변경 (OFFICE_ADMIN)")
    @PatchMapping("/staff/{targetMemberId}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long officeId,
                                           @PathVariable Long targetMemberId,
                                           @Valid @RequestBody UpdateOfficeStaffRoleRequest request,
                                           Authentication authentication) {
        accessPolicy.assertAdmin(OfficePrincipal.membershipOrNull(authentication), officeId);
        manageStaffUseCase.changeRole(new ChangeOfficeStaffRoleCommand(officeId, targetMemberId, request.role()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "담당자 제거 (OFFICE_ADMIN). 마지막 관리자는 제거할 수 없다")
    @DeleteMapping("/staff/{targetMemberId}")
    public ResponseEntity<Void> remove(@PathVariable Long officeId,
                                       @PathVariable Long targetMemberId,
                                       Authentication authentication) {
        accessPolicy.assertAdmin(OfficePrincipal.membershipOrNull(authentication), officeId);
        manageStaffUseCase.remove(new RemoveOfficeStaffCommand(officeId, targetMemberId));
        return ResponseEntity.noContent().build();
    }
}
