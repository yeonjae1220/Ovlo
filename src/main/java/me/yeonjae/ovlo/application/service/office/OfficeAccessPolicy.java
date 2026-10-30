package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import org.springframework.stereotype.Component;

/**
 * 담당자 범위 검사(ADR-0005). JWT 클레임에서 나온 {@link OfficeMembership} 과 URL 의 officeId 를 비교한다.
 * 모든 office-admin 유스케이스 진입점(어댑터)이 첫 줄에서 호출한다. DB 를 보지 않는다.
 */
@Component
public class OfficeAccessPolicy {

    public void assertStaff(OfficeMembership membership, Long officeId) {
        if (membership == null || officeId == null || !membership.belongsTo(officeId)) {
            throw new OfficeException("이 국제처의 담당자가 아닙니다", OfficeException.ErrorType.FORBIDDEN);
        }
    }

    public void assertAdmin(OfficeMembership membership, Long officeId) {
        assertStaff(membership, officeId);
        if (!membership.isAdmin()) {
            throw new OfficeException("국제처 관리자(OFFICE_ADMIN)만 할 수 있습니다", OfficeException.ErrorType.FORBIDDEN);
        }
    }
}
