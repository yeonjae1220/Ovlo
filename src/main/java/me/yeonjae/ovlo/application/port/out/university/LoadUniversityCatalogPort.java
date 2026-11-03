package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.application.dto.result.UniversityCatalogCountryResult;

import java.util.List;

/**
 * 통합 대학 카탈로그 조회 포트.
 *
 * <p>콘텐츠 보유 대학 집합 = {@code university_report(PUBLISHED, global_univ_id)}
 * ∪ {@code exchange_universities}. 리뷰 수·평점 집계는 별도 포트
 * ({@code LoadExchangeUniversityPort})에서 보강한다.</p>
 */
public interface LoadUniversityCatalogPort {

    /**
     * @param lang 표시 언어 코드(ko/en/ja/…). 행의 {@code nameLocal} 을 이 언어의 이름으로 채운다.
     *             없는 언어면 null 로 둔다(호출자가 영문으로 폴백).
     */
    List<CatalogRow> search(String keyword, String countryCode, String lang, int offset, int limit);

    long count(String keyword, String countryCode);

    List<UniversityCatalogCountryResult> findCountries();

    /**
     * 카탈로그 한 행. 리뷰 집계 전 상태(globalUnivId/exchangeUnivId/reportId + 표시 정보).
     * {@code nameLocal} 은 요청한 표시 언어의 이름(없으면 null).
     */
    record CatalogRow(
            Long globalUnivId,
            Long exchangeUnivId,
            Long reportId,
            String nameEn,
            String nameKo,
            String nameLocal,
            String country,
            String countryCode,
            String city
    ) {}
}
