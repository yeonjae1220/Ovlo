package me.yeonjae.ovlo.application.service.admin;

import me.yeonjae.ovlo.application.dto.command.IssueManualVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.AdminVerificationView;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.verification.ExpireVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.LoadMemberHomeUniversityPort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationId;
import me.yeonjae.ovlo.domain.verification.model.VerificationStatus;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVerificationServiceTest {

    @Mock private LoadVerificationCredentialPort loadPort;
    @Mock private SaveVerificationCredentialPort savePort;
    @Mock private ExpireVerificationCredentialPort expirePort;
    @Mock private LoadMemberPort loadMemberPort;
    @Mock private LoadUniversityPort loadUniversityPort;
    @Mock private LoadMemberHomeUniversityPort loadHomePort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    private AdminVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AdminVerificationService(loadPort, savePort, expirePort,
                loadMemberPort, loadUniversityPort, loadHomePort, clock);
    }

    private IssueManualVerificationCommand cmd() {
        return new IssueManualVerificationCommand(1L, 100L, "교환 증빙서류 확인", "admin@ovlo.me");
    }

    @Test
    @DisplayName("수동 인증 발급 — ADMIN_VERIFIED 자격을 이메일 없이 VERIFIED로 저장")
    void issueManual_savesAdminVerifiedCredentialWithoutEmail() {
        when(loadMemberPort.findById(any())).thenReturn(Optional.of(mock(Member.class)));
        when(loadUniversityPort.existsById(any())).thenReturn(true);

        service.issueManual(cmd());

        ArgumentCaptor<VerificationCredential> captor = ArgumentCaptor.forClass(VerificationCredential.class);
        verify(savePort).save(captor.capture());
        VerificationCredential saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(VerificationType.ADMIN_VERIFIED);
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getUniversityId()).isEqualTo(100L);
        assertThat(saved.getVerifiedBy()).isEqualTo("admin@ovlo.me");
        assertThat(saved.getNote()).isEqualTo("교환 증빙서류 확인");
        // 수동 인증은 학교 이메일을 받지 않는다 → 유일성 충돌 클래스 제거
        assertThat(saved.getVerifiedEmail()).isNull();
        verify(loadPort, never()).existsActiveByVerifiedEmailAndMemberIdNot(anyString(), any());
    }

    @Test
    @DisplayName("수동 인증 발급 — 회원이 없으면 MemberException")
    void issueManual_memberNotFound_throws() {
        when(loadMemberPort.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueManual(cmd()))
                .isInstanceOf(MemberException.class);
        verify(savePort, never()).save(any());
    }

    @Test
    @DisplayName("수동 인증 발급 — 대학이 없으면 예외")
    void issueManual_universityNotFound_throws() {
        when(loadMemberPort.findById(any())).thenReturn(Optional.of(mock(Member.class)));
        when(loadUniversityPort.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> service.issueManual(cmd()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savePort, never()).save(any());
    }

    @Test
    @DisplayName("수동 인증 발급 — note가 500자 초과면 예외")
    void issueManual_noteTooLong_throws() {
        when(loadMemberPort.findById(any())).thenReturn(Optional.of(mock(Member.class)));
        when(loadUniversityPort.existsById(any())).thenReturn(true);
        String longNote = "x".repeat(501);

        assertThatThrownBy(() -> service.issueManual(
                new IssueManualVerificationCommand(1L, 100L, longNote, "admin@ovlo.me")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savePort, never()).save(any());
    }

    @Test
    @DisplayName("자격 취소 — 소유자 일치 + 활성이면 취소자/시각과 함께 revoke")
    void revoke_activeCredential_revokesWithAudit() {
        when(expirePort.revokeByIdAndMemberId(eq(7L), eq(1L), eq("admin@ovlo.me"), any()))
                .thenReturn(1);

        service.revoke(7L, 1L, "admin@ovlo.me");

        verify(expirePort).revokeByIdAndMemberId(7L, 1L, "admin@ovlo.me", clock.instant());
    }

    @Test
    @DisplayName("자격 취소 — 소유자 불일치/미존재면 예외")
    void revoke_notFoundOrWrongOwner_throws() {
        when(expirePort.revokeByIdAndMemberId(eq(7L), eq(1L), anyString(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.revoke(7L, 1L, "admin@ovlo.me"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("회원 인증 현황 — 대학명/TrustLevel/취소정보 포함")
    void findByMember_returnsViewWithUniversityNameAndTrustLevel() {
        VerificationCredential active = VerificationCredential.restore(
                new VerificationId(5L), 1L, VerificationType.ADMIN_VERIFIED, 100L,
                null, VerificationStatus.VERIFIED, clock.instant(), "admin@ovlo.me", "서류", null, null);
        when(loadPort.findByMemberId(1L)).thenReturn(List.of(active));
        when(loadHomePort.findHomeUniversityId(1L)).thenReturn(Optional.of(200L));
        University univ = mock(University.class);
        when(univ.getName()).thenReturn("Tokyo University");
        when(loadUniversityPort.findById(any())).thenReturn(Optional.of(univ));

        AdminVerificationView view = service.findByMember(1L);

        assertThat(view.memberId()).isEqualTo(1L);
        // 본교(200)와 다른 대학(100) 활성 자격 → EXCHANGE_VERIFIED
        assertThat(view.trustLevel()).isEqualTo("EXCHANGE_VERIFIED");
        assertThat(view.credentials()).hasSize(1);
        AdminVerificationView.Credential row = view.credentials().get(0);
        assertThat(row.universityName()).isEqualTo("Tokyo University");
        assertThat(row.type()).isEqualTo("ADMIN_VERIFIED");
        assertThat(row.revokedBy()).isNull();
    }

    // Mockito.mock helper alias for readability
    private static <T> T mock(Class<T> c) {
        return org.mockito.Mockito.mock(c);
    }
}
