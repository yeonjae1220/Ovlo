package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.List;

public interface GetMemberQuery {
    MemberResult getById(MemberId memberId);
    List<MemberSummaryResult> searchByNickname(String keyword);
    boolean isNicknameAvailable(String nickname);
}
