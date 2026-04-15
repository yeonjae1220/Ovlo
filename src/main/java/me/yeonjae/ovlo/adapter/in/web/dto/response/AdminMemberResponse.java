package me.yeonjae.ovlo.adapter.in.web.dto.response;

import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;

public record AdminMemberResponse(
        Long id,
        String nickname,
        String email,
        MemberStatus status,
        MemberRole role,
        String provider
) {
    public static AdminMemberResponse of(Member member) {
        return new AdminMemberResponse(
                member.getId().value(),
                member.getNickname(),
                member.getEmail().value(),
                member.getStatus(),
                member.getRole(),
                member.getProvider() != null ? member.getProvider().name() : null
        );
    }
}
