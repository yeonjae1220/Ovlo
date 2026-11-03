package me.yeonjae.ovlo.application.dto.command;

/**
 * 통합 대학 카탈로그 검색 커맨드.
 * 콘텐츠(리포트/후기)를 보유한 대학을 이름(한/영/현지어)·국가코드로 검색한다.
 *
 * @param lang 표시 언어(UI 언어 코드, 예: ko/en/ja). 결과의 {@code nameLocal} 을 이 언어로 채운다.
 *             null 이면 영문.
 */
public record SearchUniversityCatalogCommand(
        String keyword,
        String countryCode,
        String lang,
        int page,
        int size
) {}
