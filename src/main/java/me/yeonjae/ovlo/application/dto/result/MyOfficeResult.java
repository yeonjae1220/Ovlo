package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;

/**
 * 로그인한 회원 기준 "내 Office". 담당자면 role 이 있고, 소속 대학 학생이면 role 은 null 이다.
 * verifiedStudent: 그 대학에 활성 인증 자격이 있는가. pendingInvitation: 수락 대기 중인 담당자 초대가 있는가.
 */
public record MyOfficeResult(Long officeId, String officeName, Long universityId, OfficeStatus status,
                             OfficeRole role, boolean verifiedStudent, boolean pendingInvitation) {
}
