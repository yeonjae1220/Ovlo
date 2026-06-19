package me.yeonjae.ovlo.application.dto.command;

/**
 * 통합 대학 카탈로그 검색 커맨드.
 * 콘텐츠(리포트/후기)를 보유한 대학을 이름(한/영/현지어)·국가코드로 검색한다.
 */
public record SearchUniversityCatalogCommand(
        String keyword,
        String countryCode,
        int page,
        int size
) {}
