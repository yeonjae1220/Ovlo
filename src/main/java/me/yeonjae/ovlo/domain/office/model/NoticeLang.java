package me.yeonjae.ovlo.domain.office.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/** 안내 언어. 리포트 번역 체계와 같은 7개(ko·en·ja·zh·de·fr·vi). 코드는 소문자 ISO 639-1. */
public enum NoticeLang {
    KO("ko"), EN("en"), JA("ja"), ZH("zh"), DE("de"), FR("fr"), VI("vi");

    public static final Set<NoticeLang> ALL = Set.of(values());

    private final String code;

    NoticeLang(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<NoticeLang> fromCode(String code) {
        if (code == null) return Optional.empty();
        String c = code.trim().toLowerCase();
        return Arrays.stream(values()).filter(l -> l.code.equals(c)).findFirst();
    }
}
