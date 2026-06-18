package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityCatalogJpaRepository;
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

    @Override
    public List<CatalogRow> search(String keyword, String countryCode, int offset, int limit) {
        return repository.search(blankToNull(keyword), blankToNull(countryCode), limit, offset)
                .stream()
                .map(UniversityCatalogPersistenceAdapter::toRow)
                .toList();
    }

    @Override
    public long count(String keyword, String countryCode) {
        return repository.countSearch(blankToNull(keyword), blankToNull(countryCode));
    }

    /** 컬럼 순서: global_univ_id, exchange_univ_id, report_id, name_en, name_ko, country, country_code, city */
    private static CatalogRow toRow(Object[] r) {
        return new CatalogRow(
                toLong(r[0]),
                toLong(r[1]),
                toLong(r[2]),
                (String) r[3],
                (String) r[4],
                (String) r[5],
                (String) r[6],
                (String) r[7]
        );
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
