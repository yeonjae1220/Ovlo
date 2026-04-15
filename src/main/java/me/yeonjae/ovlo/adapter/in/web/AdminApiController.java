package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdateMemberRoleRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminMemberResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminStatsResponse;
import me.yeonjae.ovlo.application.service.admin.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "어드민 API (ADMIN role 전용)")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private final AdminService adminService;

    public AdminApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "전체 회원 목록 조회")
    @GetMapping("/members")
    public ResponseEntity<Page<AdminMemberResponse>> getMembers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getMembers(pageable));
    }

    @Operation(summary = "회원 역할 변경")
    @PatchMapping("/members/{id}/role")
    public ResponseEntity<AdminMemberResponse> updateMemberRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(adminService.updateMemberRole(id, request.role()));
    }

    @Operation(summary = "전체 통계 조회")
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
