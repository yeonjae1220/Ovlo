package me.yeonjae.ovlo.domain.office.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Office 에 속한 담당자(Entity, Office 하위). 담당자는 일반 Member 다.
 * 상태 전이는 {@link Office} 가 불변식(마지막 관리자 보호 등)을 확인한 뒤 호출한다.
 */
public class OfficeStaff {

    private Long id;
    private final Long memberId;
    private OfficeRole role;
    private OfficeStaffStatus status;
    private String invitedBy;
    private Instant invitedAt;
    private Instant acceptedAt;
    private Instant removedAt;

    private OfficeStaff(Long id, Long memberId, OfficeRole role, OfficeStaffStatus status,
                        String invitedBy, Instant invitedAt, Instant acceptedAt, Instant removedAt) {
        this.id = id;
        this.memberId = memberId;
        this.role = role;
        this.status = status;
        this.invitedBy = invitedBy;
        this.invitedAt = invitedAt;
        this.acceptedAt = acceptedAt;
        this.removedAt = removedAt;
    }

    static OfficeStaff invite(Long memberId, OfficeRole role, String invitedBy, Instant now) {
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        Objects.requireNonNull(role, "role은 필수입니다");
        if (invitedBy == null || invitedBy.isBlank()) {
            throw new IllegalArgumentException("초대자(invitedBy)는 필수입니다");
        }
        Objects.requireNonNull(now, "now는 필수입니다");
        return new OfficeStaff(null, memberId, role, OfficeStaffStatus.INVITED, invitedBy, now, null, null);
    }

    public static OfficeStaff restore(Long id, Long memberId, OfficeRole role, OfficeStaffStatus status,
                                      String invitedBy, Instant invitedAt, Instant acceptedAt, Instant removedAt) {
        return new OfficeStaff(id, memberId, role, status, invitedBy, invitedAt, acceptedAt, removedAt);
    }

    void reinvite(OfficeRole role, String invitedBy, Instant now) {
        this.role = Objects.requireNonNull(role, "role은 필수입니다");
        this.status = OfficeStaffStatus.INVITED;
        this.invitedBy = invitedBy;
        this.invitedAt = now;
        this.acceptedAt = null;
        this.removedAt = null;
    }

    void accept(Instant now) {
        this.status = OfficeStaffStatus.ACTIVE;
        this.acceptedAt = now;
    }

    void changeRole(OfficeRole role) {
        this.role = Objects.requireNonNull(role, "role은 필수입니다");
    }

    void remove(Instant now) {
        this.status = OfficeStaffStatus.REMOVED;
        this.removedAt = now;
    }

    public boolean isActive() {
        return status == OfficeStaffStatus.ACTIVE;
    }

    public boolean isInvited() {
        return status == OfficeStaffStatus.INVITED;
    }

    public boolean isActiveAdmin() {
        return isActive() && role.isAdmin();
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getId()                  { return id; }
    public Long getMemberId()            { return memberId; }
    public OfficeRole getRole()          { return role; }
    public OfficeStaffStatus getStatus() { return status; }
    public String getInvitedBy()         { return invitedBy; }
    public Instant getInvitedAt()        { return invitedAt; }
    public Instant getAcceptedAt()       { return acceptedAt; }
    public Instant getRemovedAt()        { return removedAt; }
}
