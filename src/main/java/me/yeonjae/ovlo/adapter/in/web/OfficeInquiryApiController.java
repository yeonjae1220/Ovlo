package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.result.InquiryLogResult;
import me.yeonjae.ovlo.application.port.in.office.GetInquiryQuery;
import me.yeonjae.ovlo.application.service.office.OfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.shared.security.OfficePrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/** 담당자 문의 로그. 학생 식별 정보는 응답에 없다. */
@Tag(name = "Office Admin", description = "국제처 담당자 전용 — 문의 로그")
@RestController
@RequestMapping("/api/v1/office-admin/{officeId}/inquiries")
public class OfficeInquiryApiController {

    private final GetInquiryQuery getInquiryQuery;
    private final OfficeAccessPolicy accessPolicy;

    public OfficeInquiryApiController(GetInquiryQuery getInquiryQuery, OfficeAccessPolicy accessPolicy) {
        this.getInquiryQuery = getInquiryQuery;
        this.accessPolicy = accessPolicy;
    }

    @Operation(summary = "문의 로그 (결과·기간 필터)")
    @GetMapping
    public ResponseEntity<Page<InquiryLogResult>> list(@PathVariable Long officeId,
                                                       @RequestParam(required = false) InquiryOutcome outcome,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                       @PageableDefault(size = 20, sort = "askedAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                       Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(getInquiryQuery.findForStaff(officeId, outcome, from, to, pageable));
    }
}
