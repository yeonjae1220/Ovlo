package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import me.yeonjae.ovlo.application.dto.command.SearchExchangeUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.VideoReviewResult;
import me.yeonjae.ovlo.application.port.in.university.GetExchangeUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchExchangeUniversityQuery;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Exchange University", description = "교환 대학 API")
@Validated
@RestController
@RequestMapping("/api/v1/exchange-universities")
public class ExchangeUniversityApiController {

    private final SearchExchangeUniversityQuery searchQuery;
    private final GetExchangeUniversityQuery getQuery;

    public ExchangeUniversityApiController(SearchExchangeUniversityQuery searchQuery,
                                            GetExchangeUniversityQuery getQuery) {
        this.searchQuery = searchQuery;
        this.getQuery = getQuery;
    }

    @Operation(summary = "교환 대학 검색", description = "이름(한/영) 또는 국가로 검색")
    @GetMapping
    public ResponseEntity<PageResult<ExchangeUniversityResult>> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 100) String country,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(searchQuery.search(
                new SearchExchangeUniversityCommand(keyword, country, page, size)));
    }

    @Operation(summary = "교환 대학 단건 조회", description = "리뷰 수, 평균 평점 포함")
    @GetMapping("/{id}")
    public ResponseEntity<ExchangeUniversityResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getQuery.getById(new ExchangeUniversityId(id)));
    }

    @Operation(summary = "교환 대학 영상 리뷰 목록", description = "평점 높은 순 정렬")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<PageResult<VideoReviewResult>> getReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(getQuery.getReviews(new ExchangeUniversityId(id), page, size));
    }
}
