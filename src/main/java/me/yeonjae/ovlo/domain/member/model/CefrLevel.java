package me.yeonjae.ovlo.domain.member.model;

public enum CefrLevel {
    A1("입문"),
    A2("기초"),
    B1("중하"),
    B2("중상"),
    C1("고급"),
    C2("원어민 수준");

    private final String displayName;

    CefrLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
