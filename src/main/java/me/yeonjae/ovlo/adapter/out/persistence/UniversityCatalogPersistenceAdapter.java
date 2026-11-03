package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityCatalogJpaRepository;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogCountryResult;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityCatalogPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link LoadUniversityCatalogPort} 구현. 네이티브 통합 쿼리 결과(Object[])를
 * 컬럼 순서 기준으로 {@link CatalogRow} 로 매핑한다.
 */
@Component
public class UniversityCatalogPersistenceAdapter implements LoadUniversityCatalogPort {

    private final UniversityCatalogJpaRepository repository;

    public UniversityCatalogPersistenceAdapter(UniversityCatalogJpaRepository repository) {
        this.repository = repository;
    }

    /** 표시 언어를 안 주면 영문 — 네이티브 쿼리의 CASE 비교에 null 이 들어가지 않게 한다. */
    private static final String DEFAULT_LANG = "en";

    @Override
    public List<CatalogRow> search(String keyword, String countryCode, String lang, int offset, int limit) {
        String displayLang = blankToNull(lang) == null ? DEFAULT_LANG : lang.trim().toLowerCase();
        return repository.search(blankToNull(keyword), blankToNull(countryCode), displayLang, limit, offset)
                .stream()
                .map(UniversityCatalogPersistenceAdapter::toRow)
                .toList();
    }

    @Override
    public long count(String keyword, String countryCode) {
        return repository.countSearch(blankToNull(keyword), blankToNull(countryCode));
    }

    @Override
    public List<UniversityCatalogCountryResult> findCountries() {
        return repository.findCountries().stream()
                .map(r -> new UniversityCatalogCountryResult(
                        (String) r[0],
                        (String) r[1],
                        ((Number) r[2]).longValue()))
                .toList();
    }

    /** 컬럼 순서: global_univ_id, exchange_univ_id, report_id, name_en, name_ko, name_local, country, country_code, city */
    private static CatalogRow toRow(Object[] r) {
        return new CatalogRow(
                toLong(r[0]),
                toLong(r[1]),
                toLong(r[2]),
                (String) r[3],
                (String) r[4],
                (String) r[5],
                (String) r[6],
                (String) r[7],
                (String) r[8]
        );
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
