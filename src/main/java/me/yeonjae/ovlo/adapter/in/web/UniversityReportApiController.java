package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportSummaryResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityReportQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "University Report", description = "대학별 AI 종합 보고서 API")
@Validated
@RestController
@RequestMapping("/api/v1/university-reports")
public class UniversityReportApiController {

    private final GetUniversityReportQuery query;

    public UniversityReportApiController(GetUniversityReportQuery query) {
        this.query = query;
    }

    @Operation(summary = "보고서 목록 조회", description = "PUBLISHED 상태 보고서만 반환. keyword로 대학명(영문) 검색 가능")
    @GetMapping
    public ResponseEntity<PageResult<UniversityReportSummaryResult>> list(
            @RequestParam(defaultValue = "ko") String lang,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(query.getReports(lang, keyword, page, size));
    }

    @Operation(summary = "보고서 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<UniversityReportResult> getById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ko") String lang
    ) {
        return query.getById(id, lang)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "대학(globalUnivId)으로 보고서 조회")
    @GetMapping("/by-university/{globalUnivId}")
    public ResponseEntity<UniversityReportResult> getByUniversity(
            @PathVariable Long globalUnivId,
            @RequestParam(defaultValue = "ko") String lang
    ) {
        return query.getByGlobalUnivId(globalUnivId, lang)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "교환대학(exchangeUnivId)으로 보고서 조회")
    @GetMapping("/by-exchange-university/{exchangeUnivId}")
    public ResponseEntity<UniversityReportResult> getByExchangeUniversity(
            @PathVariable Long exchangeUnivId,
            @RequestParam(defaultValue = "ko") String lang
    ) {
        return query.getByExchangeUnivId(exchangeUnivId, lang)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "보고서 지원 언어 목록")
    @GetMapping("/{id}/languages")
    public ResponseEntity<List<String>> getLanguages(@PathVariable Long id) {
        return ResponseEntity.ok(query.getAvailableLangs(id));
    }
}
