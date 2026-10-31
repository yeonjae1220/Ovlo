package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.domain.office.model.OfficeMembership;

/** 초대받은 회원이 담당자 자격을 수락한다. 수락 뒤 새 토큰을 받아야 클레임이 반영된다. */
public interface AcceptOfficeInvitationUseCase {
    OfficeMembership accept(Long memberId);
}
