package me.yeonjae.ovlo.application.dto.command;

/**
 * 본교 선택(회원가입·온보딩) 대학 검색 커맨드.
 *
 * @param lang 표시 언어(UI 언어 코드). 결과의 {@code localName} 을 이 언어의 대학명으로 채운다.
 *             null/en 이면 비워 둔다(영문명이 곧 표시명).
 */
public record SearchGlobalUniversityCommand(
        String keyword,
        String countryCode,
        String lang,
        int page,
        int size
) {}
