package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchGlobalUniversityCommand;
import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.GlobalUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchGlobalUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.university.exception.UniversityException;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 단일 대학 카탈로그 조회/검색 서비스 (global_universities 백킹).
 * 구 GlobalUniversityQueryService를 흡수: 프론트 /api/v1/global-universities 검색도 함께 제공.
 */
@Service
@Transactional(readOnly = true)
public class UniversityQueryService
        implements SearchUniversityQuery, GetUniversityQuery, SearchGlobalUniversityQuery {

    private final LoadUniversityPort loadUniversityPort;
    private final SearchUniversityPort searchUniversityPort;

    public UniversityQueryService(LoadUniversityPort loadUniversityPort,
                                  SearchUniversityPort searchUniversityPort) {
        this.loadUniversityPort = loadUniversityPort;
        this.searchUniversityPort = searchUniversityPort;
    }

    @Override
    public PageResult<UniversityResult> search(SearchUniversityCommand command) {
        List<University> universities = searchUniversityPort.search(
                command.keyword(), command.countryCode(), command.offset(), command.size());
        long total = searchUniversityPort.count(command.keyword(), command.countryCode());

        List<UniversityResult> content = universities.stream().map(UniversityResult::from).toList();
        return PageResult.of(content, total, command.page(), command.size());
    }

    @Override
    public PageResult<GlobalUniversityResult> search(SearchGlobalUniversityCommand command) {
        int offset = command.page() * command.size();
        List<University> universities = searchUniversityPort.search(
                command.keyword(), command.countryCode(), offset, command.size());
        long total = searchUniversityPort.count(command.keyword(), command.countryCode());

        Map<Long, String> localNames = localizedNames(universities, command.lang());
        List<GlobalUniversityResult> content = universities.stream()
                .map(u -> GlobalUniversityResult.from(u, localNames.get(u.getId().value())))
                .toList();
        return PageResult.of(content, total, command.page(), command.size());
    }

    /** 영문 UI 는 name_en 이 곧 표시명이라 조회를 건너뛴다. */
    private Map<Long, String> localizedNames(List<University> universities, String lang) {
        if (universities.isEmpty() || lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang.trim())) {
            return Map.of();
        }
        List<Long> ids = universities.stream().map(u -> u.getId().value()).toList();
        return searchUniversityPort.findLocalizedNames(ids, lang);
    }

    @Override
    public UniversityResult getById(UniversityId id) {
        University university = loadUniversityPort.findById(id)
                .orElseThrow(() -> new UniversityException.NotFound(id.value()));
        return UniversityResult.from(university);
    }
}
