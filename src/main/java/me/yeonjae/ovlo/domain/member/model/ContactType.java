package me.yeonjae.ovlo.domain.member.model;

public enum ContactType {
    PHONE("전화번호"),
    SNS("SNS 계정"),
    EMAIL("이메일"),
    LINK("기타 링크");

    private final String displayName;

    ContactType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
