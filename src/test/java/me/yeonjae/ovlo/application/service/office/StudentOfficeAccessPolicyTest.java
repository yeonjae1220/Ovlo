package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentOfficeAccessPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Long UNIV = 266L;
    private static final Long OTHER_UNIV = 70L;
    private static final Long OFFICE_ID = 5L;
    private static final Long MEMBER = 42L;

    @Mock LoadOfficePort loadOfficePort;
    @Mock LoadVerificationCredentialPort loadCredentialPort;

    private StudentOfficeAccessPolicy policy;
    private Office office;

    @BeforeEach
    void setUp() {
        policy = new StudentOfficeAccessPolicy(loadOfficePort, loadCredentialPort);
        office = Office.create(UNIV, "국제처", NOW);
        office.assignId(new OfficeId(OFFICE_ID));
    }

    private static VerificationCredential active(Long univ) {
        return VerificationCredential.issue(MEMBER, VerificationType.SCHOOL_EMAIL, univ, "s@univ.ac.kr", NOW);
    }

    @Test
    @DisplayName("그 대학의 활성 인증 자격이 있으면 Office 를 돌려준다")
    void verifiedStudentPasses() {
        given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));
        given(loadCredentialPort.findByMemberId(MEMBER)).willReturn(List.of(active(UNIV)));

        assertThat(policy.assertMember(MEMBER, OFFICE_ID)).isSameAs(office);
    }

    @Test
    @DisplayName("다른 대학 자격만 있거나 자격이 만료됐으면 FORBIDDEN")
    void otherUniversityOrExpired() {
        given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));
        VerificationCredential expired = active(UNIV);
        expired.expire();
        given(loadCredentialPort.findByMemberId(MEMBER)).willReturn(List.of(active(OTHER_UNIV), expired));

        assertThatThrownBy(() -> policy.assertMember(MEMBER, OFFICE_ID))
                .isInstanceOf(OfficeException.class)
                .extracting(e -> ((OfficeException) e).getErrorType())
                .isEqualTo(OfficeException.ErrorType.FORBIDDEN);
    }

    @Test
    @DisplayName("정지된 Office 는 인증 학생이어도 SUSPENDED")
    void suspendedOffice() {
        office.suspend();
        given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));

        assertThatThrownBy(() -> policy.assertMember(MEMBER, OFFICE_ID))
                .isInstanceOf(OfficeException.class)
                .extracting(e -> ((OfficeException) e).getErrorType())
                .isEqualTo(OfficeException.ErrorType.SUSPENDED);
    }

    @Test
    @DisplayName("없는 Office 는 NOT_FOUND")
    void unknownOffice() {
        given(loadOfficePort.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> policy.assertMember(MEMBER, 99L))
                .isInstanceOf(OfficeException.class)
                .extracting(e -> ((OfficeException) e).getErrorType())
                .isEqualTo(OfficeException.ErrorType.NOT_FOUND);
    }
}
