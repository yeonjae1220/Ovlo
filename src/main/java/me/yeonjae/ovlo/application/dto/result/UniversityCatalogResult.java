package me.yeonjae.ovlo.application.dto.result;

/**
 * 통합 대학 카탈로그 검색 결과 한 건.
 *
 * <p>홈 "팁"(university_report)과 교환대학 후기(exchange_universities)를 단일 카탈로그로 합쳐
 * "콘텐츠가 있는 대학"을 표현한다. 프론트는 {@code hasReport}/{@code hasReviews} 플래그로
 * 클릭 시 라우팅 대상을 결정한다.</p>
 *
 * <ul>
 *   <li>{@code hasReport}  → /university-reports/{reportId} (집계 가이드, 우선)</li>
 *   <li>{@code hasReviews} → /exchange-universities/{exchangeUnivId} (영상 후기)</li>
 * </ul>
 */
public record UniversityCatalogResult(
        Long globalUnivId,
        Long exchangeUnivId,
        Long reportId,
        String nameEn,
        String nameKo,
        String country,
        String countryCode,
        String city,
        boolean hasReport,
        boolean hasReviews,
        long reviewCount,
        Double avgRating
) {}
