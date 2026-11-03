package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import me.yeonjae.ovlo.application.dto.command.SearchGlobalUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.GlobalUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.port.in.university.SearchGlobalUniversityQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Global University", description = "전세계 대학 검색 API (10,150개)")
@Validated
@RestController
@RequestMapping("/api/v1/global-universities")
public class GlobalUniversityApiController {

    private final SearchGlobalUniversityQuery searchQuery;

    public GlobalUniversityApiController(SearchGlobalUniversityQuery searchQuery) {
        this.searchQuery = searchQuery;
    }

    @Operation(
        summary = "전세계 대학 검색",
        description = "이름(영문·현지어·다국어·별칭) 또는 국가코드(ISO 3166-1 alpha-2)로 검색. 10,150개 대학 지원. "
                    + "lang(UI 언어 코드)을 주면 localName 에 그 언어의 대학명을 채운다(없으면 null)."
    )
    @GetMapping
    public ResponseEntity<PageResult<GlobalUniversityResult>> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 2)   String countryCode,
            @RequestParam(required = false) @Size(max = 5)   String lang,
            @RequestParam(defaultValue = "0")  @Min(0)       int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(searchQuery.search(
                new SearchGlobalUniversityCommand(keyword, countryCode, lang, page, size)));
    }
}
