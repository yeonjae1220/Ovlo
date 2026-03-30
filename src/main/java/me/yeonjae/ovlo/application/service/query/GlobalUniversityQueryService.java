package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchGlobalUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.GlobalUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.port.in.university.SearchGlobalUniversityQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadGlobalUniversityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GlobalUniversityQueryService implements SearchGlobalUniversityQuery {

    private final LoadGlobalUniversityPort loadGlobalUniversityPort;

    public GlobalUniversityQueryService(LoadGlobalUniversityPort loadGlobalUniversityPort) {
        this.loadGlobalUniversityPort = loadGlobalUniversityPort;
    }

    @Override
    public PageResult<GlobalUniversityResult> search(SearchGlobalUniversityCommand command) {
        int offset = command.page() * command.size();
        List<GlobalUniversityResult> content = loadGlobalUniversityPort
                .search(command.keyword(), command.countryCode(), offset, command.size())
                .stream()
                .map(GlobalUniversityResult::from)
                .toList();
        long total = loadGlobalUniversityPort.count(command.keyword(), command.countryCode());
        return PageResult.of(content, total, command.page(), command.size());
    }
}
