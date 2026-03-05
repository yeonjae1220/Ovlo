package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.domain.member.model.MemberId;

public interface GetMemberQuery {
    MemberResult getById(MemberId memberId);
}
