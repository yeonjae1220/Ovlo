package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminStatsResponse;
import me.yeonjae.ovlo.application.service.admin.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Read-only summary consumed by the lab.mungji console aggregator. Returns
 * aggregate counts only (no PII), gated by {@code ServiceTokenAuthFilter} on the
 * {@code /api/internal/**} security chain.
 */
@RestController
@RequestMapping("/api/internal/console")
public class InternalConsoleController {

    private final AdminService adminService;

    public InternalConsoleController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        AdminStatsResponse stats = adminService.getStats();
        return Map.of(
                "totalUsers", stats.totalMembers(),
                "usage", List.of(
                        Map.of("label", "게시판", "value", stats.totalBoards()),
                        Map.of("label", "게시글", "value", stats.totalPosts()),
                        Map.of("label", "대학", "value", stats.totalUniversities())
                ));
    }
}
