package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.CreateOfficeRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.InviteOfficeStaffRequest;
import me.yeonjae.ovlo.application.dto.command.CreateOfficeCommand;
import me.yeonjae.ovlo.application.dto.command.InviteOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 플랫폼 ADMIN 전용(/api/v1/admin/** → hasAuthority("ADMIN")). Office 생성·정지·첫 담당자 초대만 한다. */
@Tag(name = "Admin", description = "어드민 API (ADMIN role 전용)")
@RestController
@RequestMapping("/api/v1/admin/offices")
public class OfficeAdminApiController {

    private final AdministerOfficeUseCase administerUseCase;
    private final ManageOfficeStaffUseCase manageStaffUseCase;
    private final GetOfficeQuery getOfficeQuery;

    public OfficeAdminApiController(AdministerOfficeUseCase administerUseCase,
                                    ManageOfficeStaffUseCase manageStaffUseCase,
                                    GetOfficeQuery getOfficeQuery) {
        this.administerUseCase = administerUseCase;
        this.manageStaffUseCase = manageStaffUseCase;
        this.getOfficeQuery = getOfficeQuery;
    }

    @Operation(summary = "국제처 테넌트 목록")
    @GetMapping
    public ResponseEntity<Page<OfficeResult>> list(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(getOfficeQuery.findAll(pageable));
    }

    @Operation(summary = "국제처 테넌트 상세")
    @GetMapping("/{officeId}")
    public ResponseEntity<OfficeResult> get(@PathVariable Long officeId) {
        return getOfficeQuery.findById(officeId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다", OfficeException.ErrorType.NOT_FOUND));
    }

    @Operation(summary = "국제처 테넌트 생성 (대학당 1개)")
    @PostMapping
    public ResponseEntity<OfficeResult> create(@Valid @RequestBody CreateOfficeRequest request) {
        OfficeResult result = administerUseCase.create(new CreateOfficeCommand(request.globalUnivId(), request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "첫 담당자(보통 OFFICE_ADMIN) 초대")
    @PostMapping("/{officeId}/staff")
    public ResponseEntity<OfficeStaffResult> invite(@PathVariable Long officeId,
                                                    @Valid @RequestBody InviteOfficeStaffRequest request,
                                                    Authentication authentication) {
        String invitedBy = authentication != null ? String.valueOf(authentication.getName()) : "admin";
        OfficeStaffResult result = manageStaffUseCase.invite(
                new InviteOfficeStaffCommand(officeId, request.email(), request.role(), "admin:" + invitedBy));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "정지 — 담당자 세션을 모두 지우고 학생·담당자 쓰기를 거부한다")
    @PostMapping("/{officeId}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable Long officeId) {
        administerUseCase.suspend(officeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "재개(ACTIVE)")
    @PostMapping("/{officeId}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long officeId) {
        administerUseCase.activate(officeId);
        return ResponseEntity.noContent().build();
    }
}
