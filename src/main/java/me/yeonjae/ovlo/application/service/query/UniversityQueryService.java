package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityPageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.university.exception.UniversityException;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UniversityQueryService implements SearchUniversityQuery, GetUniversityQuery {

    private final LoadUniversityPort loadUniversityPort;
    private final SearchUniversityPort searchUniversityPort;

    public UniversityQueryService(LoadUniversityPort loadUniversityPort,
                                  SearchUniversityPort searchUniversityPort) {
        this.loadUniversityPort = loadUniversityPort;
        this.searchUniversityPort = searchUniversityPort;
    }

    @Override
    public UniversityPageResult search(SearchUniversityCommand command) {
        List<University> universities = searchUniversityPort.search(
                command.keyword(),
                command.countryCode(),
                command.offset(),
                command.size());
        long total = searchUniversityPort.count(command.keyword(), command.countryCode());

        List<UniversityResult> content = universities.stream()
                .map(UniversityResult::from)
                .toList();

        return new UniversityPageResult(content, total, command.page(), command.size());
    }

    @Override
    public UniversityResult getById(UniversityId id) {
        University university = loadUniversityPort.findById(id)
                .orElseThrow(() -> new UniversityException("대학을 찾을 수 없습니다: " + id.value()));
        return UniversityResult.from(university);
    }
}
