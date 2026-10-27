package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.member.model.Member;

/**
 * 닉네임 검색 결과 — 다른 회원에게 보여도 되는 필드만 담는다.
 * 이메일·생년월일·연락처 등은 {@link MemberResult} 에만 있다.
 */
public record MemberSummaryResult(
        Long id,
        String nickname,
        String name,
        String profileImageMediaId
) {

    public static MemberSummaryResult from(Member member) {
        return new MemberSummaryResult(
                member.getId() != null ? member.getId().value() : null,
                member.getNickname(),
                member.getName(),
                member.getProfileImageMediaId()
        );
    }
}
