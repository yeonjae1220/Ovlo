package me.yeonjae.ovlo.domain.help.model;

/** 도움말 질문. 웹 검증과 별개로 유료 호출 전에 다시 확인하는 마지막 방어선이다. */
public record HelpQuestion(String value) {

    public static final int MAX_LENGTH = 1000;

    public HelpQuestion {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("질문을 입력해 주세요");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("질문은 " + MAX_LENGTH + "자 이하로 입력해 주세요");
        }
    }
}
