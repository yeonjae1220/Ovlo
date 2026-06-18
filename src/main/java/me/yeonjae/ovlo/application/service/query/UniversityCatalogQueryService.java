package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogResult;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadExchangeUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityCatalogPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityCatalogPort.CatalogRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 콘텐츠(리포트/후기) 보유 대학 통합 검색 서비스.
 *
 * <p>카탈로그 행을 조회한 뒤, 후기를 가진 대학에 한해 리뷰 수·평점을
 * {@link LoadExchangeUniversityPort} 로 보강한다.</p>
 */
@Service
@Transactional(readOnly = true)
public class UniversityCatalogQueryService implements SearchUniversityCatalogQuery {

    private final LoadUniversityCatalogPort loadCatalogPort;
    private final LoadExchangeUniversityPort loadExchangePort;

    public UniversityCatalogQueryService(LoadUniversityCatalogPort loadCatalogPort,
                                         LoadExchangeUniversityPort loadExchangePort) {
        this.loadCatalogPort = loadCatalogPort;
        this.loadExchangePort = loadExchangePort;
    }

    @Override
    public PageResult<UniversityCatalogResult> search(SearchUniversityCatalogCommand command) {
        int offset = command.page() * command.size();
        List<CatalogRow> rows = loadCatalogPort.search(
                command.keyword(), command.countryCode(), offset, command.size());

        List<Long> exchangeIds = rows.stream()
                .map(CatalogRow::exchangeUnivId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, Long> counts = loadExchangePort.countReviewsByUniversityIds(exchangeIds);
        Map<Long, Double> avgRatings = loadExchangePort.avgRatingsByUniversityIds(exchangeIds);

        List<UniversityCatalogResult> content = rows.stream()
                .map(row -> toResult(row, counts, avgRatings))
                .toList();

        long total = loadCatalogPort.count(command.keyword(), command.countryCode());
        return PageResult.of(content, total, command.page(), command.size());
    }

    private UniversityCatalogResult toResult(CatalogRow row,
                                             Map<Long, Long> counts,
                                             Map<Long, Double> avgRatings) {
        boolean hasReviews = row.exchangeUnivId() != null;
        long reviewCount = hasReviews ? counts.getOrDefault(row.exchangeUnivId(), 0L) : 0L;
        Double avgRating = hasReviews ? avgRatings.get(row.exchangeUnivId()) : null;
        return new UniversityCatalogResult(
                row.globalUnivId(),
                row.exchangeUnivId(),
                row.reportId(),
                row.nameEn(),
                row.nameKo(),
                row.country(),
                row.countryCode(),
                row.city(),
                row.reportId() != null,
                hasReviews,
                reviewCount,
                avgRating
        );
    }
}
