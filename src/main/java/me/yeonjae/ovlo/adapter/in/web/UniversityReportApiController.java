package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.yeonjae.ovlo.application.dto.result.UniversityReportPageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.port.in.report.GetUniversityReportQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UniversityReport", description = "대학 교환학생 종합 보고서 API")
@Validated
@RestController
@RequestMapping("/api/v1/university-reports")
public class UniversityReportApiController {

    private final GetUniversityReportQuery getUniversityReportQuery;

    public UniversityReportApiController(GetUniversityReportQuery getUniversityReportQuery) {
        this.getUniversityReportQuery = getUniversityReportQuery;
    }

    @Operation(summary = "보고서 목록 조회 (페이지네이션)")
    @GetMapping
    public ResponseEntity<UniversityReportPageResult> list(
            @RequestParam(defaultValue = "ko") String lang,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(getUniversityReportQuery.list(lang, page, size));
    }

    @Operation(summary = "보고서 단건 조회 (report ID)")
    @GetMapping("/{id}")
    public ResponseEntity<UniversityReportResult> getById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ko") String lang
    ) {
        return ResponseEntity.ok(getUniversityReportQuery.getById(id, lang));
    }

    @Operation(summary = "대학 보고서 조회 (global_univ_id)")
    @GetMapping("/by-university/{globalUnivId}")
    public ResponseEntity<UniversityReportResult> getByUniversity(
            @PathVariable Long globalUnivId,
            @RequestParam(defaultValue = "ko") String lang
    ) {
        return ResponseEntity.ok(getUniversityReportQuery.getByGlobalUnivId(globalUnivId, lang));
    }

    @Operation(summary = "보고서 지원 언어 목록")
    @GetMapping("/{id}/languages")
    public ResponseEntity<List<String>> getLanguages(@PathVariable Long id) {
        return ResponseEntity.ok(getUniversityReportQuery.getSupportedLanguages(id));
    }
}
