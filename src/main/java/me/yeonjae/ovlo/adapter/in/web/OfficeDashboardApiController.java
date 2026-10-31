package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdatePartnerSettingsRequest;
import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult;
import me.yeonjae.ovlo.application.port.in.office.ExportOfficeDataQuery;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeDashboardQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficePartnerSettingsUseCase;
import me.yeonjae.ovlo.application.service.office.OfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import me.yeonjae.ovlo.shared.security.OfficePrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** 담당자 대시보드·CSV·제휴 노출 설정. 매 요청 첫 줄에서 "이 Office 의" 담당자인지 확인한다(ADR-0005). */
@Tag(name = "Office Admin", description = "국제처 담당자 전용 — 대시보드·내보내기·설정")
@RestController
@RequestMapping("/api/v1/office-admin/{officeId}")
public class OfficeDashboardApiController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final GetOfficeDashboardQuery dashboardQuery;
    private final ExportOfficeDataQuery exportQuery;
    private final ManageOfficePartnerSettingsUseCase partnerSettings;
    private final OfficeAccessPolicy accessPolicy;

    public OfficeDashboardApiController(GetOfficeDashboardQuery dashboardQuery, ExportOfficeDataQuery exportQuery,
                                        ManageOfficePartnerSettingsUseCase partnerSettings, OfficeAccessPolicy accessPolicy) {
        this.dashboardQuery = dashboardQuery;
        this.exportQuery = exportQuery;
        this.partnerSettings = partnerSettings;
        this.accessPolicy = accessPolicy;
    }

    @Operation(summary = "대시보드 지표 (기본 최근 30일, KST 날짜 포함 구간)")
    @GetMapping("/dashboard")
    public ResponseEntity<OfficeDashboardResult> dashboard(@PathVariable Long officeId,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                           Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(dashboardQuery.dashboard(officeId, from, to));
    }

    @Operation(summary = "문의 로그 CSV (학생 식별 정보 없음)")
    @GetMapping("/export/inquiries.csv")
    public ResponseEntity<byte[]> inquiriesCsv(@PathVariable Long officeId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return csv("inquiries", exportQuery.inquiriesCsv(officeId, from, to));
    }

    @Operation(summary = "상담 CSV (학생 식별 정보 없음)")
    @GetMapping("/export/consultations.csv")
    public ResponseEntity<byte[]> consultationsCsv(@PathVariable Long officeId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return csv("consultations", exportQuery.consultationsCsv(officeId));
    }

    @Operation(summary = "승인 자료 CSV")
    @GetMapping("/export/notices.csv")
    public ResponseEntity<byte[]> noticesCsv(@PathVariable Long officeId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return csv("notices", exportQuery.noticesCsv(officeId));
    }

    @Operation(summary = "이 대학에 켜진 제휴 분류")
    @GetMapping("/settings/partners")
    public ResponseEntity<Map<String, Set<PartnerCategory>>> partnerSettings(@PathVariable Long officeId,
                                                                             Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(Map.of("enabledCategories", partnerSettings.enabledCategories(officeId)));
    }

    @Operation(summary = "제휴 분류 노출 설정 (OFFICE_ADMIN)")
    @PutMapping("/settings/partners")
    public ResponseEntity<Map<String, Set<PartnerCategory>>> updatePartnerSettings(@PathVariable Long officeId,
                                                                                   @Valid @RequestBody UpdatePartnerSettingsRequest request,
                                                                                   Authentication authentication) {
        accessPolicy.assertAdmin(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(Map.of("enabledCategories", partnerSettings.update(officeId, request.enabledCategories())));
    }

    private static ResponseEntity<byte[]> csv(String name, String body) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ovlo-" + name + ".csv\"")
                .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
