package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityPageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "University", description = "대학 API")
@RestController
@RequestMapping("/api/v1/universities")
public class UniversityApiController {

    private final SearchUniversityQuery searchUniversityQuery;
    private final GetUniversityQuery getUniversityQuery;

    public UniversityApiController(
            SearchUniversityQuery searchUniversityQuery,
            GetUniversityQuery getUniversityQuery
    ) {
        this.searchUniversityQuery = searchUniversityQuery;
        this.getUniversityQuery = getUniversityQuery;
    }

    @Operation(summary = "대학 검색")
    @GetMapping
    public ResponseEntity<UniversityPageResult> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String countryCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UniversityPageResult result = searchUniversityQuery.search(
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
