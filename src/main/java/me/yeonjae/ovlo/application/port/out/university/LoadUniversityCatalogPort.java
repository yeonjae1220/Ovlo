package me.yeonjae.ovlo.application.port.out.university;

import java.util.List;

/**
 * 통합 대학 카탈로그 조회 포트.
 *
 * <p>콘텐츠 보유 대학 집합 = {@code university_report(PUBLISHED, global_univ_id)}
 * ∪ {@code exchange_universities}. 리뷰 수·평점 집계는 별도 포트
 * ({@code LoadExchangeUniversityPort})에서 보강한다.</p>
 */
public interface LoadUniversityCatalogPort {

    List<CatalogRow> search(String keyword, String countryCode, int offset, int limit);

    long count(String keyword, String countryCode);

    /**
     * 카탈로그 한 행. 리뷰 집계 전 상태(globalUnivId/exchangeUnivId/reportId + 표시 정보).
     */
    record CatalogRow(
            Long globalUnivId,
            Long exchangeUnivId,
            Long reportId,
            String nameEn,
            String nameKo,
            String country,
            String countryCode,
            String city
    ) {}
}
