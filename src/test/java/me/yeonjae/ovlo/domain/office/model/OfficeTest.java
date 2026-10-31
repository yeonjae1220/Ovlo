package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficeTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Long UNIV = 266L;

    private static Office officeWithAdmin(Long adminMemberId) {
        Office office = Office.create(UNIV, "성균관대학교 국제처", NOW);
        office.invite(adminMemberId, OfficeRole.OFFICE_ADMIN, "admin@ovlo.test", NOW);
        office.accept(adminMemberId, NOW);
        return office;
    }

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("대학 1곳에 PILOT 상태로 만들어지고 담당자는 비어 있다")
        void createsPilotOffice() {
            Office office = Office.create(UNIV, "성균관대학교 국제처", NOW);

            assertThat(office.getGlobalUnivId()).isEqualTo(UNIV);
            assertThat(office.getStatus()).isEqualTo(OfficeStatus.PILOT);
            assertThat(office.getCreatedAt()).isEqualTo(NOW);
            assertThat(office.getStaff()).isEmpty();
        }

        @Test
        @DisplayName("대학 id 나 이름이 없으면 만들 수 없다")
        void rejectsMissingUniversityOrName() {
            assertThatThrownBy(() -> Office.create(null, "x", NOW)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Office.create(UNIV, " ", NOW)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("partner settings")
    class PartnerSettings {
        @Test
        @DisplayName("기본은 전부 꺼져 있고, 켠 분류만 노출된다")
        void defaultsOffAndToggles() {
            Office office = Office.create(UNIV, "국제처", NOW);
            assertThat(office.getEnabledPartnerCategories()).isEmpty();

            office.setPartnerEnabled(PartnerCategory.SIM, true);
            office.setPartnerEnabled(PartnerCategory.INSURANCE, true);
            office.setPartnerEnabled(PartnerCategory.SIM, false);

            assertThat(office.isPartnerEnabled(PartnerCategory.INSURANCE)).isTrue();
            assertThat(office.isPartnerEnabled(PartnerCategory.SIM)).isFalse();
            assertThat(office.getEnabledPartnerCategories()).containsExactly(PartnerCategory.INSURANCE);
        }

        @Test
        @DisplayName("정지된 Office 는 제휴 설정을 바꿀 수 없다")
        void suspendedCannotChange() {
            Office office = Office.create(UNIV, "국제처", NOW);
            office.suspend();
            assertThatThrownBy(() -> office.setPartnerEnabled(PartnerCategory.SIM, true))
                    .isInstanceOf(OfficeException.class);
        }

        @Test
        @DisplayName("restore 로 읽은 설정이 그대로 유지된다")
        void restoreKeepsSettings() {
            Office office = Office.restore(new OfficeId(1L), UNIV, "국제처", OfficeStatus.ACTIVE, NOW,
                    java.util.List.of(), java.util.Set.of(PartnerCategory.HOUSING, PartnerCategory.PICKUP));
            assertThat(office.getEnabledPartnerCategories()).containsExactlyInAnyOrder(PartnerCategory.HOUSING, PartnerCategory.PICKUP);
        }
    }

    @Nested
    @DisplayName("invite / accept")
    class InviteAccept {
        @Test
        @DisplayName("초대하면 INVITED, 수락하면 ACTIVE 가 되고 수락 시각이 남는다")
        void inviteThenAccept() {
            Office office = Office.create(UNIV, "국제처", NOW);

            OfficeStaff invited = office.invite(10L, OfficeRole.OFFICE_ADMIN, "admin@ovlo.test", NOW);
            assertThat(invited.getStatus()).isEqualTo(OfficeStaffStatus.INVITED);
            assertThat(invited.getInvitedBy()).isEqualTo("admin@ovlo.test");

            OfficeStaff accepted = office.accept(10L, NOW.plusSeconds(60));
            assertThat(accepted.getStatus()).isEqualTo(OfficeStaffStatus.ACTIVE);
            assertThat(accepted.getAcceptedAt()).isEqualTo(NOW.plusSeconds(60));
            assertThat(office.activeMembershipOf(10L)).contains(new OfficeMembership(null, OfficeRole.OFFICE_ADMIN));
        }

        @Test
        @DisplayName("이미 초대됐거나 활동 중인 회원을 다시 초대하면 CONFLICT")
        void rejectsDuplicateInvite() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);

            assertThatThrownBy(() -> office.invite(10L, OfficeRole.OFFICE_STAFF, "10", NOW))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.CONFLICT);
            assertThatThrownBy(() -> office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW))
                    .isInstanceOf(OfficeException.class);
        }

        @Test
        @DisplayName("제거된 회원은 다시 초대할 수 있고 같은 기록이 INVITED 로 돌아간다")
        void reinvitesRemovedStaff() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);
            office.accept(11L, NOW);
            office.remove(11L, NOW);

            OfficeStaff again = office.invite(11L, OfficeRole.OFFICE_ADMIN, "10", NOW.plusSeconds(5));

            assertThat(again.getStatus()).isEqualTo(OfficeStaffStatus.INVITED);
            assertThat(again.getRole()).isEqualTo(OfficeRole.OFFICE_ADMIN);
            assertThat(office.getStaff()).hasSize(2);
        }

        @Test
        @DisplayName("초대받지 않은 회원의 수락은 NOT_FOUND")
        void acceptWithoutInvitation() {
            Office office = Office.create(UNIV, "국제처", NOW);

            assertThatThrownBy(() -> office.accept(99L, NOW))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("정지된 Office 는 초대·수락을 거부한다")
        void suspendedOfficeRejectsChanges() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);
            office.suspend();

            assertThatThrownBy(() -> office.invite(12L, OfficeRole.OFFICE_STAFF, "10", NOW))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.SUSPENDED);
            assertThatThrownBy(() -> office.accept(11L, NOW)).isInstanceOf(OfficeException.class);
            assertThat(office.activeMembershipOf(10L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("마지막 OFFICE_ADMIN 보호")
    class LastAdmin {
        @Test
        @DisplayName("유일한 활동 중 관리자를 STAFF 로 내릴 수 없다")
        void cannotDemoteLastAdmin() {
            Office office = officeWithAdmin(10L);

            assertThatThrownBy(() -> office.changeRole(10L, OfficeRole.OFFICE_STAFF))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("유일한 활동 중 관리자를 제거할 수 없다")
        void cannotRemoveLastAdmin() {
            Office office = officeWithAdmin(10L);

            assertThatThrownBy(() -> office.remove(10L, NOW)).isInstanceOf(OfficeException.class);
        }

        @Test
        @DisplayName("관리자가 둘이면 한 명은 내리거나 제거할 수 있다")
        void allowsWhenAnotherAdminRemains() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_ADMIN, "10", NOW);
            office.accept(11L, NOW);

            office.changeRole(10L, OfficeRole.OFFICE_STAFF);
            assertThat(office.findStaff(10L).orElseThrow().getRole()).isEqualTo(OfficeRole.OFFICE_STAFF);

            office.remove(10L, NOW);
            assertThat(office.findStaff(10L).orElseThrow().getStatus()).isEqualTo(OfficeStaffStatus.REMOVED);
            assertThat(office.activeMembershipOf(10L)).isEmpty();
        }

        @Test
        @DisplayName("초대만 받은(미수락) 관리자는 '남아 있는 관리자'로 세지 않는다")
        void invitedAdminDoesNotCount() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_ADMIN, "10", NOW);

            assertThatThrownBy(() -> office.remove(10L, NOW)).isInstanceOf(OfficeException.class);
        }
    }

    @Test
    @DisplayName("없는 회원의 역할 변경·제거는 NOT_FOUND")
    void unknownMember() {
        Office office = officeWithAdmin(10L);

        assertThatThrownBy(() -> office.changeRole(99L, OfficeRole.OFFICE_STAFF)).isInstanceOf(OfficeException.class);
        assertThatThrownBy(() -> office.remove(99L, NOW)).isInstanceOf(OfficeException.class);
    }
}
