package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.OfficeException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 대학 국제처 테넌트 (Aggregate Root). 대학(global_universities) 1곳에 Office 1개.
 *
 * 불변식
 * - 활동 중(ACTIVE) OFFICE_ADMIN 이 생긴 뒤에는 최소 1명이 남아야 한다(마지막 관리자 강등·제거 금지).
 * - 같은 회원이 INVITED/ACTIVE 로 두 번 들어올 수 없다. REMOVED 는 재초대로 같은 기록이 INVITED 로 돌아간다.
 * - SUSPENDED 면 담당자 변경을 거부한다.
 */
public class Office {

    private OfficeId id;
    private final Long globalUnivId;
    private String name;
    private OfficeStatus status;
    private final Instant createdAt;
    private final List<OfficeStaff> staff;
    /** 이 대학 학생에게 보여 줄 제휴 분류. 기본 전부 꺼짐(설계 §4.1) — 국제처가 켜기 전에는 노출하지 않는다. */
    private final EnumSet<PartnerCategory> enabledPartnerCategories;

    private Office(OfficeId id, Long globalUnivId, String name, OfficeStatus status,
                   Instant createdAt, List<OfficeStaff> staff, java.util.Set<PartnerCategory> enabledPartnerCategories) {
        this.id = id;
        this.globalUnivId = globalUnivId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.staff = new ArrayList<>(staff);
        this.enabledPartnerCategories = enabledPartnerCategories.isEmpty()
                ? EnumSet.noneOf(PartnerCategory.class) : EnumSet.copyOf(enabledPartnerCategories);
    }

    public static Office create(Long globalUnivId, String name, Instant now) {
        Objects.requireNonNull(globalUnivId, "globalUnivId는 필수입니다");
        Objects.requireNonNull(now, "now는 필수입니다");
        return new Office(null, globalUnivId, requireName(name), OfficeStatus.PILOT, now, List.of(), java.util.Set.of());
    }

    public static Office restore(OfficeId id, Long globalUnivId, String name, OfficeStatus status,
                                 Instant createdAt, List<OfficeStaff> staff,
                                 java.util.Set<PartnerCategory> enabledPartnerCategories) {
        return new Office(id, globalUnivId, name, status, createdAt, staff, enabledPartnerCategories);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Office 이름은 비어 있을 수 없습니다");
        }
        return name.trim();
    }

    // ── 담당자 ────────────────────────────────────────────────────────────────

    public OfficeStaff invite(Long memberId, OfficeRole role, String invitedBy, Instant now) {
        assertNotSuspended();
        Optional<OfficeStaff> existing = findStaff(memberId);
        if (existing.isPresent()) {
            OfficeStaff s = existing.get();
            if (s.getStatus() != OfficeStaffStatus.REMOVED) {
                throw new OfficeException("이미 초대되었거나 활동 중인 담당자입니다: member=" + memberId,
                        OfficeException.ErrorType.CONFLICT);
            }
            s.reinvite(role, invitedBy, now);
            return s;
        }
        OfficeStaff invited = OfficeStaff.invite(memberId, role, invitedBy, now);
        staff.add(invited);
        return invited;
    }

    public OfficeStaff accept(Long memberId, Instant now) {
        assertNotSuspended();
        OfficeStaff s = findStaff(memberId)
                .filter(OfficeStaff::isInvited)
                .orElseThrow(() -> new OfficeException("초대받은 기록이 없습니다: member=" + memberId,
                        OfficeException.ErrorType.NOT_FOUND));
        s.accept(now);
        return s;
    }

    public void changeRole(Long memberId, OfficeRole role) {
        assertNotSuspended();
        OfficeStaff s = requireStaff(memberId);
        if (s.isActiveAdmin() && !role.isAdmin()) {
            assertAnotherActiveAdminExists(memberId);
        }
        s.changeRole(role);
    }

    public void remove(Long memberId, Instant now) {
        assertNotSuspended();
        OfficeStaff s = requireStaff(memberId);
        if (s.isActiveAdmin()) {
            assertAnotherActiveAdminExists(memberId);
        }
        s.remove(now);
    }

    /** 활동 중 담당자면 그 소속 정보를 돌려준다. 정지된 Office 는 비어 있는 값을 준다. */
    public Optional<OfficeMembership> activeMembershipOf(Long memberId) {
        if (status == OfficeStatus.SUSPENDED) return Optional.empty();
        return findStaff(memberId)
                .filter(OfficeStaff::isActive)
                .map(s -> new OfficeMembership(id != null ? id.value() : null, s.getRole()));
    }

    public Optional<OfficeStaff> findStaff(Long memberId) {
        return staff.stream().filter(s -> s.getMemberId().equals(memberId)).findFirst();
    }

    public List<OfficeStaff> activeStaff() {
        return staff.stream().filter(OfficeStaff::isActive).toList();
    }

    private OfficeStaff requireStaff(Long memberId) {
        return findStaff(memberId)
                .filter(s -> s.getStatus() != OfficeStaffStatus.REMOVED)
                .orElseThrow(() -> new OfficeException("담당자를 찾을 수 없습니다: member=" + memberId,
                        OfficeException.ErrorType.NOT_FOUND));
    }

    private void assertAnotherActiveAdminExists(Long exceptMemberId) {
        boolean another = staff.stream()
                .anyMatch(s -> s.isActiveAdmin() && !s.getMemberId().equals(exceptMemberId));
        if (!another) {
            throw new OfficeException("마지막 OFFICE_ADMIN 은 내리거나 제거할 수 없습니다",
                    OfficeException.ErrorType.BAD_REQUEST);
        }
    }

    private void assertNotSuspended() {
        if (status == OfficeStatus.SUSPENDED) {
            throw new OfficeException("정지된 Office 입니다", OfficeException.ErrorType.SUSPENDED);
        }
    }

    // ── 상태 ──────────────────────────────────────────────────────────────────

    public void suspend() {
        this.status = OfficeStatus.SUSPENDED;
    }

    public void activate() {
        this.status = OfficeStatus.ACTIVE;
    }

    /** 제휴 분류 노출 on/off. 정지된 Office 는 설정 변경 불가. */
    public void setPartnerEnabled(PartnerCategory category, boolean enabled) {
        assertNotSuspended();
        java.util.Objects.requireNonNull(category, "category는 필수입니다");
        if (enabled) enabledPartnerCategories.add(category); else enabledPartnerCategories.remove(category);
    }

    public boolean isPartnerEnabled(PartnerCategory category) {
        return enabledPartnerCategories.contains(category);
    }

    public java.util.Set<PartnerCategory> getEnabledPartnerCategories() {
        return Collections.unmodifiableSet(enabledPartnerCategories);
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void assignId(OfficeId id) {
        this.id = id;
    }

    public boolean isSuspended() {
        return status == OfficeStatus.SUSPENDED;
    }

    public OfficeId getId()           { return id; }
    public Long getGlobalUnivId()     { return globalUnivId; }
    public String getName()           { return name; }
    public OfficeStatus getStatus()   { return status; }
    public Instant getCreatedAt()     { return createdAt; }
    public List<OfficeStaff> getStaff() { return Collections.unmodifiableList(staff); }
}
