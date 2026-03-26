package me.yeonjae.ovlo.domain.member.model;

public record Password(String hashedValue) {
    // null 허용 — LOCAL 회원은 Member.create()에서 null 검증, GOOGLE 회원은 null
}
