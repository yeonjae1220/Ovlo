package me.yeonjae.ovlo.application.dto.result;

/**
 * 통합 카탈로그 국가 목록 한 건 — 콘텐츠(리포트/후기) 보유 대학을 국가별 집계.
 * 후기 보유 국가만 세던 {@link ExchangeUniversityCountryResult} 와 달리
 * 리포트만 있는 국가도 포함한다.
 */
public record UniversityCatalogCountryResult(
        String country,
        String countryCode,
        long universityCount
) {}
