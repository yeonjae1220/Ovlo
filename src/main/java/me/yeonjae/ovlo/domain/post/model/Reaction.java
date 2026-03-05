package me.yeonjae.ovlo.domain.post.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Objects;

public record Reaction(MemberId memberId, ReactionType type) {

    public Reaction {
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다");
        Objects.requireNonNull(type, "반응 타입은 필수입니다");
    }
}
