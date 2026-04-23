package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchExchangeUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.VideoReviewResult;
import me.yeonjae.ovlo.application.port.in.university.GetExchangeUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchExchangeUniversityQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadExchangeUniversityPort;
import me.yeonjae.ovlo.domain.university.exception.UniversityException;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversity;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ExchangeUniversityQueryService implements SearchExchangeUniversityQuery, GetExchangeUniversityQuery {

    private final LoadExchangeUniversityPort loadExchangeUniversityPort;

    public ExchangeUniversityQueryService(LoadExchangeUniversityPort loadExchangeUniversityPort) {
        this.loadExchangeUniversityPort = loadExchangeUniversityPort;
    }

    @Override
    public PageResult<ExchangeUniversityResult> search(SearchExchangeUniversityCommand command) {
        int offset = command.page() * command.size();
        List<ExchangeUniversity> universities = loadExchangeUniversityPort
                .search(command.keyword(), command.countryCode(), offset, command.size());

        List<Long> ids = universities.stream().map(university -> university.getId().value()).toList();
        Map<Long, Long> counts = loadExchangeUniversityPort.countReviewsByUniversityIds(ids);
        Map<Long, Double> avgRatings = loadExchangeUniversityPort.avgRatingsByUniversityIds(ids);

        List<ExchangeUniversityResult> content = universities.stream()
                .map(university -> ExchangeUniversityResult.of(
                        university,
                        counts.getOrDefault(university.getId().value(), 0L),
                        avgRatings.get(university.getId().value())
                ))
                .toList();
        long total = loadExchangeUniversityPort.count(command.keyword(), command.countryCode());
        return PageResult.of(content, total, command.page(), command.size());
    }

    @Override
    public ExchangeUniversityResult getById(ExchangeUniversityId id) {
        ExchangeUniversity university = loadExchangeUniversityPort.findById(id)
                .orElseThrow(() -> new UniversityException.NotFound(id.value()));
        long reviewCount = loadExchangeUniversityPort.countReviewsByUniversityId(id, null);
        Double avgRating = loadExchangeUniversityPort.avgRatingByUniversityId(id);
        return ExchangeUniversityResult.of(university, reviewCount, avgRating);
    }

    @Override
    public PageResult<VideoReviewResult> getReviews(ExchangeUniversityId id, String direction, int page, int size) {
        loadExchangeUniversityPort.findById(id)
                .orElseThrow(() -> new UniversityException.NotFound(id.value()));
        int offset = page * size;
        List<VideoReviewResult> content = loadExchangeUniversityPort
                .findReviewsByUniversityId(id, direction, offset, size)
                .stream()
                .map(VideoReviewResult::from)
                .toList();
        long total = loadExchangeUniversityPort.countReviewsByUniversityId(id, direction);
        return PageResult.of(content, total, page, size);
    }
}
