package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ConfirmSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.command.RequestSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityEmailResolution;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.dto.result.VerificationRequestResult;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.port.in.university.ResolveUniversityByEmailQuery;
import me.yeonjae.ovlo.application.port.out.verification.ChallengeStorePort;
import me.yeonjae.ovlo.application.port.out.verification.EmailSenderPort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.VerificationCodeGenerator;
import me.yeonjae.ovlo.domain.verification.exception.VerificationException;
import me.yeonjae.ovlo.domain.verification.exception.VerificationException.ErrorType;
import me.yeonjae.ovlo.domain.verification.model.EmailVerificationChallenge;
import me.yeonjae.ovlo.domain.verification.model.VerificationCode;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchoolEmailVerificationServiceTest {

    @Mock private ResolveUniversityByEmailQuery resolveUniversityByEmail;
    @Mock private LoadVerificationCredentialPort loadCredentialPort;
    @Mock private SaveVerificationCredentialPort saveCredentialPort;
    @Mock private ChallengeStorePort challengeStore;
    @Mock private EmailSenderPort emailSender;
    @Mock private VerificationCodeGenerator codeGenerator;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T00:00:00Z"), ZoneOffset.UTC);
    private SchoolEmailVerificationService service;

    private static final long MEMBER = 1L;
    private static final long UNIV = 100L;

    @BeforeEach
    void setUp() {
        service = new SchoolEmailVerificationService(
                resolveUniversityByEmail, loadCredentialPort, saveCredentialPort,
                challengeStore, emailSender, codeGenerator, clock);
    }

    private UniversityResult univ(long id, String name) {
        return new UniversityResult(id, name, null, "KR", "Seoul", null, null, "https://x", "snu.ac.kr");
    }

    @Nested
    @DisplayName("코드 발송")
    class Request {

        @Test
        @DisplayName("도메인이 지정 대학과 일치하면 코드를 생성·저장·발송한다")
        void sendsCode() {
            given(resolveUniversityByEmail.resolveByEmail("alice@snu.ac.kr"))
                    .willReturn(UniversityEmailResolution.matched("snu.ac.kr", univ(UNIV, "Seoul National University")));
            given(loadCredentialPort.existsActiveByVerifiedEmailAndMemberIdNot("alice@snu.ac.kr", MEMBER)).willReturn(false);
            given(codeGenerator.generate()).willReturn(new VerificationCode("123456"));

            VerificationRequestResult result = service.request(
                    new RequestSchoolEmailVerificationCommand(MEMBER, UNIV, "Alice@SNU.ac.kr"));

            verify(challengeStore).save(any(EmailVerificationChallenge.class), eq(Duration.ofMinutes(10)));
            verify(emailSender).sendVerificationCode("alice@snu.ac.kr", "123456", "Seoul National University");
            assertThat(result.maskedEmail()).contains("@snu.ac.kr");
            assertThat(result.expiresInSeconds()).isEqualTo(600);
        }

        @Test
        @DisplayName("공개 메일이면 PUBLIC_PROVIDER 예외 - 발송 안 함")
        void rejectsPublicProvider() {
            given(resolveUniversityByEmail.resolveByEmail("x@gmail.com"))
                    .willReturn(UniversityEmailResolution.publicProvider("gmail.com"));

            assertThatThrownBy(() -> service.request(new RequestSchoolEmailVerificationCommand(MEMBER, UNIV, "x@gmail.com")))
                    .isInstanceOf(VerificationException.class)
                    .extracting("errorType").isEqualTo(ErrorType.PUBLIC_PROVIDER);
            verify(emailSender, never()).sendVerificationCode(any(), any(), any());
        }

        @Test
        @DisplayName("도메인이 지정 대학과 불일치하면 DOMAIN_MISMATCH")
        void rejectsDomainMismatch() {
            given(resolveUniversityByEmail.resolveByEmail("a@snu.ac.kr"))
                    .willReturn(UniversityEmailResolution.matched("snu.ac.kr", univ(999L, "Other Univ")));

            assertThatThrownBy(() -> service.request(new RequestSchoolEmailVerificationCommand(MEMBER, UNIV, "a@snu.ac.kr")))
                    .isInstanceOf(VerificationException.class)
                    .extracting("errorType").isEqualTo(ErrorType.DOMAIN_MISMATCH);
        }

        @Test
        @DisplayName("다른 멤버가 이미 인증한 이메일이면 EMAIL_ALREADY_USED")
        void rejectsTakenEmail() {
            given(resolveUniversityByEmail.resolveByEmail("a@snu.ac.kr"))
                    .willReturn(UniversityEmailResolution.matched("snu.ac.kr", univ(UNIV, "SNU")));
            given(loadCredentialPort.existsActiveByVerifiedEmailAndMemberIdNot("a@snu.ac.kr", MEMBER)).willReturn(true);

            assertThatThrownBy(() -> service.request(new RequestSchoolEmailVerificationCommand(MEMBER, UNIV, "a@snu.ac.kr")))
                    .isInstanceOf(VerificationException.class)
                    .extracting("errorType").isEqualTo(ErrorType.EMAIL_ALREADY_USED);
        }
    }

    @Nested
    @DisplayName("코드 확인")
    class Confirm {

        private EmailVerificationChallenge pending(String code, int maxAttempts) {
            return EmailVerificationChallenge.create(
                    MEMBER, UNIV, "alice@snu.ac.kr", new VerificationCode(code),
                    clock.instant().plus(Duration.ofMinutes(10)), maxAttempts);
        }

        @Test
        @DisplayName("코드 일치 시 자격 발급 + 챌린지 삭제 + STUDENT 반환")
        void success() {
            given(challengeStore.findByMemberId(MEMBER)).willReturn(Optional.of(pending("123456", 5)));
            VerificationCredential issued = VerificationCredential.issue(
                    MEMBER, VerificationType.SCHOOL_EMAIL, UNIV, "alice@snu.ac.kr", clock.instant());
            given(saveCredentialPort.save(any())).willReturn(issued);
            given(loadCredentialPort.findByMemberId(MEMBER)).willReturn(List.of(issued));

            VerificationStatusResult result = service.confirm(new ConfirmSchoolEmailVerificationCommand(MEMBER, "123456"));

            verify(saveCredentialPort).save(any(VerificationCredential.class));
            verify(challengeStore).deleteByMemberId(MEMBER);
            assertThat(result.trustLevel()).isEqualTo("STUDENT");
            assertThat(result.verifiedUniversities()).hasSize(1);
        }

        @Test
        @DisplayName("진행 중 챌린지 없으면 CHALLENGE_NOT_FOUND")
        void noChallenge() {
            given(challengeStore.findByMemberId(MEMBER)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirm(new ConfirmSchoolEmailVerificationCommand(MEMBER, "123456")))
                    .isInstanceOf(VerificationException.class)
                    .extracting("errorType").isEqualTo(ErrorType.CHALLENGE_NOT_FOUND);
        }

        @Test
        @DisplayName("코드 불일치 시 CODE_MISMATCH + 시도 소비 재저장, 자격 미발급")
        void mismatch() {
            given(challengeStore.findByMemberId(MEMBER)).willReturn(Optional.of(pending("123456", 5)));

            assertThatThrownBy(() -> service.confirm(new ConfirmSchoolEmailVerificationCommand(MEMBER, "000000")))
                    .isInstanceOf(VerificationException.class)
                    .extracting("errorType").isEqualTo(ErrorType.CODE_MISMATCH);
            verify(challengeStore).save(any(EmailVerificationChallenge.class), any(Duration.class));
            verify(saveCredentialPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("현황 조회")
    class Status {

        @Test
        @DisplayName("자격 없으면 UNVERIFIED")
        void unverified() {
            given(loadCredentialPort.findByMemberId(MEMBER)).willReturn(List.of());
            assertThat(service.getByMemberId(MEMBER).trustLevel()).isEqualTo("UNVERIFIED");
        }
    }
}
