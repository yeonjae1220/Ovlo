package me.yeonjae.ovlo.domain.office.model;

/**
 * 국제처 담당자 역할. 플랫폼 {@code MemberRole.ADMIN} 과는 독립이다(ADR-0005).
 * OFFICE_ADMIN 은 담당자 초대·역할 변경·설정을 할 수 있고, OFFICE_STAFF 는 자료·상담 처리만 한다.
 */
public enum OfficeRole {
    OFFICE_ADMIN, OFFICE_STAFF;

    public boolean isAdmin() {
        return this == OFFICE_ADMIN;
    }
}
