package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;

import java.util.Optional;

/**
 * 토큰 발급·재발급 때 JWT 에 넣을 담당자 소속을 조회한다(ADR-0005). 정지된 Office 의 소속은 돌려주지 않는다.
 * 한 회원이 여러 Office 의 활동 담당자면 1단계에서는 가장 먼저 수락한 것 하나만 준다.
 */
public interface LoadOfficeMembershipPort {
    Optional<OfficeMembership> findActiveMembership(MemberId memberId);
}
