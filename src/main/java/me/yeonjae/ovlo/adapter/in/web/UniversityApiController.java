package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogCountryResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogResult;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.security.ClientIpResolver;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "University", description = "대학 API")
@Validated
@RestController
@RequestMapping("/api/v1/universities")
public class UniversityApiController {

    private final SearchUniversityQuery searchUniversityQuery;
    private final GetUniversityQuery getUniversityQuery;
    private final SearchUniversityCatalogQuery searchCatalogQuery;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    public UniversityApiController(
            SearchUniversityQuery searchUniversityQuery,
            GetUniversityQuery getUniversityQuery,
            SearchUniversityCatalogQuery searchCatalogQuery,
            RateLimiterService rateLimiterService,
            ClientIpResolver clientIpResolver
    ) {
        this.searchUniversityQuery = searchUniversityQuery;
        this.getUniversityQuery = getUniversityQuery;
        this.searchCatalogQuery = searchCatalogQuery;
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
    }

    @Operation(
            summary = "통합 대학 카탈로그 검색",
            description = "콘텐츠(리포트/후기)를 보유한 대학을 이름(한/영/현지어)·국가코드로 검색. "
                    + "결과의 hasReport/hasReviews 플래그로 상세 라우팅을 결정한다."
    )
    @GetMapping("/catalog")
    public ResponseEntity<PageResult<UniversityCatalogResult>> searchCatalog(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 2) String countryCode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest httpRequest
    ) {
        rateLimiterService.checkSearchRate(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(searchCatalogQuery.search(
                new SearchUniversityCatalogCommand(keyword, countryCode, page, size)));
    }

    @Operation(
            summary = "통합 카탈로그 국가 목록",
            description = "콘텐츠(리포트/후기)를 보유한 대학을 국가별로 집계한 목록 (필터 드롭다운용)"
    )
    @GetMapping("/catalog/countries")
    public ResponseEntity<List<UniversityCatalogCountryResult>> getCatalogCountries() {
        return ResponseEntity.ok(searchCatalogQuery.getCountries());
    }

    @Operation(summary = "대학 검색")
    @GetMapping
    public ResponseEntity<PageResult<UniversityResult>> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 10) String countryCode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageResult<UniversityResult> result = searchUniversityQuery.search(
                new SearchUniversityCommand(keyword, countryCode, page, size)
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "대학 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<UniversityResult> getById(@PathVariable Long id) {
        UniversityResult result = getUniversityQuery.getById(new UniversityId(id));
        return ResponseEntity.ok(result);
    }
}
