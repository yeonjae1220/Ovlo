package me.yeonjae.ovlo.domain.member.model;

public enum MemberStatus {
    ACTIVE("활성"),
    SUSPENDED("정지"),
    WITHDRAWN("탈퇴");

    private final String displayName;

    MemberStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
