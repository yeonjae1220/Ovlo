package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ConfirmSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.command.RequestSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityEmailResolution;
import me.yeonjae.ovlo.application.dto.result.VerificationRequestResult;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.port.in.verification.ConfirmSchoolEmailVerificationUseCase;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.in.verification.RequestSchoolEmailVerificationUseCase;
import me.yeonjae.ovlo.application.port.in.university.ResolveUniversityByEmailQuery;
import me.yeonjae.ovlo.application.port.out.verification.ChallengeStorePort;
import me.yeonjae.ovlo.application.port.out.verification.EmailSenderPort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.VerificationCodeGenerator;
import me.yeonjae.ovlo.domain.verification.exception.VerificationException;
import me.yeonjae.ovlo.domain.verification.exception.VerificationException.ErrorType;
import me.yeonjae.ovlo.domain.verification.model.ChallengeOutcome;
import me.yeonjae.ovlo.domain.verification.model.EmailVerificationChallenge;
import me.yeonjae.ovlo.domain.verification.model.VerificationCode;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import me.yeonjae.ovlo.domain.verification.model.TrustLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 학교/파견 대학 이메일 인증 유스케이스 (발송·확인·현황).
 * 도메인↔대학 일치는 {@link ResolveUniversityByEmailQuery}로 판정하며,
 * 본교 일치를 요구하지 않으므로 home_university_id가 null이어도 인증 가능하다.
 */
@Service
@Transactional
public class SchoolEmailVerificationService implements
        RequestSchoolEmailVerificationUseCase,
        ConfirmSchoolEmailVerificationUseCase,
        GetMyVerificationStatusQuery {

    static final Duration CODE_TTL = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;

    private final ResolveUniversityByEmailQuery resolveUniversityByEmail;
    private final LoadVerificationCredentialPort loadCredentialPort;
    private final SaveVerificationCredentialPort saveCredentialPort;
    private final ChallengeStorePort challengeStore;
    private final EmailSenderPort emailSender;
    private final VerificationCodeGenerator codeGenerator;
    private final Clock clock;

    public SchoolEmailVerificationService(ResolveUniversityByEmailQuery resolveUniversityByEmail,
                                          LoadVerificationCredentialPort loadCredentialPort,
                                          SaveVerificationCredentialPort saveCredentialPort,
                                          ChallengeStorePort challengeStore,
                                          EmailSenderPort emailSender,
                                          VerificationCodeGenerator codeGenerator,
                                          Clock clock) {
        this.resolveUniversityByEmail = resolveUniversityByEmail;
        this.loadCredentialPort = loadCredentialPort;
        this.saveCredentialPort = saveCredentialPort;
        this.challengeStore = challengeStore;
        this.emailSender = emailSender;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    @Override
    public VerificationRequestResult request(RequestSchoolEmailVerificationCommand command) {
        String email = normalize(command.schoolEmail());

        UniversityEmailResolution resolution = resolveUniversityByEmail.resolveByEmail(email);
        switch (resolution.status()) {
            case PUBLIC_PROVIDER -> throw new VerificationException(
                    "학교 이메일이 아닙니다(공개 메일).", ErrorType.PUBLIC_PROVIDER);
            case NOT_FOUND -> throw new VerificationException(
                    "이메일 도메인이 등록된 대학과 매칭되지 않습니다.", ErrorType.UNIVERSITY_NOT_RESOLVED);
            case MATCHED, AMBIGUOUS -> {
                if (!resolution.matchesUniversity(command.universityId())) {
                    throw new VerificationException(
                            "이메일 도메인이 지정한 대학과 일치하지 않습니다.", ErrorType.DOMAIN_MISMATCH);
                }
            }
        }

        if (loadCredentialPort.existsActiveByVerifiedEmailAndMemberIdNot(email, command.memberId())) {
            throw new VerificationException(
                    "이미 다른 계정에서 인증된 학교 이메일입니다.", ErrorType.EMAIL_ALREADY_USED);
        }

        VerificationCode code = codeGenerator.generate();
        Instant now = clock.instant();
        EmailVerificationChallenge challenge = EmailVerificationChallenge.create(
                command.memberId(), command.universityId(), email, code, now.plus(CODE_TTL), MAX_ATTEMPTS);
        challengeStore.save(challenge, CODE_TTL);

        emailSender.sendVerificationCode(email, code.value(), universityNameOf(resolution, command.universityId()));

        return new VerificationRequestResult(mask(email), CODE_TTL.toSeconds());
    }

    @Override
    public VerificationStatusResult confirm(ConfirmSchoolEmailVerificationCommand command) {
        EmailVerificationChallenge challenge = challengeStore.findByMemberId(command.memberId())
                .orElseThrow(() -> new VerificationException(
                        "진행 중인 인증이 없습니다. 코드를 다시 요청하세요.", ErrorType.CHALLENGE_NOT_FOUND));

        Instant now = clock.instant();
        ChallengeOutcome outcome = challenge.verify(new VerificationCode(command.code()), now);

        switch (outcome) {
            case EXPIRED -> {
                challengeStore.deleteByMemberId(command.memberId());
                throw new VerificationException("인증 코드가 만료되었습니다.", ErrorType.CODE_EXPIRED);
            }
            case EXHAUSTED -> {
                challengeStore.deleteByMemberId(command.memberId());
                throw new VerificationException("시도 횟수를 초과했습니다. 코드를 다시 요청하세요.", ErrorType.TOO_MANY_ATTEMPTS);
            }
            case MISMATCH -> {
                // 시도 1 소비 상태로 재저장(남은 TTL 유지)
                challengeStore.save(challenge, Duration.between(now, challenge.getExpiresAt()));
                throw new VerificationException(
                        "인증 코드가 일치하지 않습니다. (남은 시도 " + challenge.remainingAttempts() + "회)",
                        ErrorType.CODE_MISMATCH);
            }
            case SUCCESS -> {
                challengeStore.deleteByMemberId(command.memberId());
                VerificationCredential credential = VerificationCredential.issue(
                        challenge.getMemberId(), VerificationType.SCHOOL_EMAIL,
                        challenge.getUniversityId(), challenge.getTargetEmail(), now);
                saveCredentialPort.save(credential);
                return getByMemberId(command.memberId());
            }
        }
        throw new IllegalStateException("도달 불가");
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationStatusResult getByMemberId(Long memberId) {
        List<VerificationCredential> credentials = loadCredentialPort.findByMemberId(memberId);
        TrustLevel trustLevel = TrustLevel.from(credentials);
        List<VerificationStatusResult.VerifiedUniversity> verified = credentials.stream()
                .filter(VerificationCredential::isActive)
                .map(c -> new VerificationStatusResult.VerifiedUniversity(
                        c.getUniversityId(), c.getVerifiedEmail(), c.getVerifiedAt().toString()))
                .toList();
        return new VerificationStatusResult(trustLevel.name(), verified);
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("학교 이메일은 필수입니다");
        }
        return email.trim().toLowerCase();
    }

    private String universityNameOf(UniversityEmailResolution resolution, Long universityId) {
        return resolution.candidates().stream()
                .filter(c -> c.id().equals(universityId))
                .map(c -> c.name())
                .findFirst()
                .orElse("");
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String masked = local.charAt(0)
                + "*".repeat(Math.max(1, local.length() - 2))
                + (local.length() > 1 ? String.valueOf(local.charAt(local.length() - 1)) : "");
        return masked + domain;
    }
}
