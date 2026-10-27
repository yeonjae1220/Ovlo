package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.List;

public interface GetMemberQuery {
    MemberResult getById(MemberId memberId);

    /**
     * 조회자에 따라 공개 범위를 달리한 프로필. 본인이면 전체, 다른 회원(또는 알 수 없는 조회자)이면
     * 계정 이메일·생년월일을 뺀다.
     */
    MemberResult getProfile(MemberId target, MemberId viewer);
    List<MemberSummaryResult> searchByNickname(String keyword);
    boolean isNicknameAvailable(String nickname);
}
