package me.yeonjae.ovlo.domain.office.model;

import java.util.Objects;

/**
 * "이 회원은 Office N 의 담당자다"를 나타내는 값. JWT 클레임(officeId·officeRole)의 원형이며
 * 범위 검사(OfficeAccessPolicy)의 입력이다. officeId 는 아직 저장되지 않은 Office 에서는 null 일 수 있다.
 */
public record OfficeMembership(Long officeId, OfficeRole role) {
    public OfficeMembership {
        Objects.requireNonNull(role, "role은 null일 수 없습니다");
    }

    public boolean isAdmin() {
        return role.isAdmin();
    }

    public boolean belongsTo(Long otherOfficeId) {
        return officeId != null && officeId.equals(otherOfficeId);
    }
}
