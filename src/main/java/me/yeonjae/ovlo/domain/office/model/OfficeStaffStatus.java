package me.yeonjae.ovlo.domain.office.model;

/** INVITED → ACTIVE(수락) → REMOVED. REMOVED 는 다시 INVITED 로 돌아갈 수 있다(재초대). */
public enum OfficeStaffStatus {
    INVITED, ACTIVE, REMOVED
}
