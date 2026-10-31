package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficeAccessPolicyTest {

    private final OfficeAccessPolicy policy = new OfficeAccessPolicy();

    @Test
    @DisplayName("클레임이 없으면(일반 회원) FORBIDDEN")
    void noMembership() {
        assertThatThrownBy(() -> policy.assertStaff(null, 5L))
                .isInstanceOf(OfficeException.class)
                .extracting(e -> ((OfficeException) e).getErrorType())
                .isEqualTo(OfficeException.ErrorType.FORBIDDEN);
    }

    @Test
    @DisplayName("다른 Office 의 담당자는 FORBIDDEN")
    void otherOffice() {
        var membership = new OfficeMembership(7L, OfficeRole.OFFICE_ADMIN);

        assertThatThrownBy(() -> policy.assertStaff(membership, 5L)).isInstanceOf(OfficeException.class);
        assertThatThrownBy(() -> policy.assertAdmin(membership, 5L)).isInstanceOf(OfficeException.class);
    }

    @Test
    @DisplayName("같은 Office 의 STAFF 는 staff 검사는 통과하지만 admin 검사는 FORBIDDEN")
    void staffIsNotAdmin() {
        var membership = new OfficeMembership(5L, OfficeRole.OFFICE_STAFF);

        assertThatCode(() -> policy.assertStaff(membership, 5L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.assertAdmin(membership, 5L))
                .isInstanceOf(OfficeException.class)
                .extracting(e -> ((OfficeException) e).getErrorType())
                .isEqualTo(OfficeException.ErrorType.FORBIDDEN);
    }

    @Test
    @DisplayName("같은 Office 의 ADMIN 은 둘 다 통과")
    void adminPasses() {
        var membership = new OfficeMembership(5L, OfficeRole.OFFICE_ADMIN);

        assertThatCode(() -> policy.assertStaff(membership, 5L)).doesNotThrowAnyException();
        assertThatCode(() -> policy.assertAdmin(membership, 5L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("저장 전 Office(officeId null) 클레임은 어떤 경로도 통과하지 못한다")
    void nullOfficeIdNeverMatches() {
        var membership = new OfficeMembership(null, OfficeRole.OFFICE_ADMIN);

        assertThatThrownBy(() -> policy.assertStaff(membership, 5L)).isInstanceOf(OfficeException.class);
        assertThatThrownBy(() -> policy.assertStaff(membership, null)).isInstanceOf(OfficeException.class);
    }
}
