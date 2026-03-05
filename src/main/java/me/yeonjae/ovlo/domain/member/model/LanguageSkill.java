package me.yeonjae.ovlo.domain.member.model;

import java.util.Objects;

public record LanguageSkill(String languageCode, CefrLevel level) {

    public LanguageSkill {
        Objects.requireNonNull(languageCode, "언어 코드는 null일 수 없습니다");
        if (languageCode.isBlank()) {
            throw new IllegalArgumentException("언어 코드는 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(level, "CEFR 레벨은 null일 수 없습니다");
    }
}
