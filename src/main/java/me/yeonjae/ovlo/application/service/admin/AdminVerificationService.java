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
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.domain.verification.model.TrustLevel;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * 관리자 수동 대학 인증 유스케이스 (발급·취소·회원별 현황).
 * 셀프서비스 이메일 코드 플로우와 독립적으로 ADMIN_VERIFIED 자격을 다룬다.
 * 수동 인증은 증빙서류 기반이므로 학교 이메일을 받지 않는다(verified_email = null).
 * application/service 규칙: JpaRepository 직접 주입 금지 — out-port에만 의존.
 */
@Service
@Transactional(readOnly = true)
public class AdminVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AdminVerificationService.class);

    /** note 컬럼 길이(VerificationCredentialJpaEntity와 일치). */
    private static final int MAX_NOTE_LENGTH = 500;

    private final LoadVerificationCredentialPort loadPort;
    private final SaveVerificationCredentialPort savePort;
    private final ExpireVerificationCredentialPort expirePort;
    private final LoadMemberPort loadMemberPort;
    private final LoadUniversityPort loadUniversityPort;
    private final LoadMemberHomeUniversityPort loadHomePort;
    private final Clock clock;

    public AdminVerificationService(LoadVerificationCredentialPort loadPort,
                                    SaveVerificationCredentialPort savePort,
                                    ExpireVerificationCredentialPort expirePort,
                                    LoadMemberPort loadMemberPort,
                                    LoadUniversityPort loadUniversityPort,
                                    LoadMemberHomeUniversityPort loadHomePort,
                                    Clock clock) {
        this.loadPort = loadPort;
        this.savePort = savePort;
        this.expirePort = expirePort;
        this.loadMemberPort = loadMemberPort;
        this.loadUniversityPort = loadUniversityPort;
        this.loadHomePort = loadHomePort;
        this.clock = clock;
    }

    @Transactional
    public void issueManual(IssueManualVerificationCommand command) {
        if (loadMemberPort.findById(new MemberId(command.memberId())).isEmpty()) {
            throw new MemberException("회원을 찾을 수 없습니다: id=" + command.memberId(),
                    MemberException.ErrorType.NOT_FOUND);
        }
        if (!loadUniversityPort.existsById(new UniversityId(command.universityId()))) {
            throw new IllegalArgumentException("대학을 찾을 수 없습니다: id=" + command.universityId());
        }
        if (command.note() != null && command.note().length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("사유 메모는 " + MAX_NOTE_LENGTH + "자를 초과할 수 없습니다.");
        }

        VerificationCredential credential = VerificationCredential.issueManual(
                command.memberId(), command.universityId(), null,
                command.adminEmail(), normalizeNote(command.note()), clock.instant());
        savePort.save(credential);

        log.info("[AdminVerification] 수동 인증 발급 admin={} member={} university={}",
                command.adminEmail(), command.memberId(), command.universityId());
    }

    @Transactional
    public void revoke(Long credentialId, Long memberId, String adminEmail) {
        int affected = expirePort.revokeByIdAndMemberId(credentialId, memberId, adminEmail, clock.instant());
        if (affected == 0) {
            throw new IllegalArgumentException(
                    "취소할 활성 자격을 찾을 수 없습니다: id=" + credentialId + ", member=" + memberId);
        }
        log.info("[AdminVerification] 자격 취소 admin={} credentialId={} member={}",
                adminEmail, credentialId, memberId);
    }

    public AdminVerificationView findByMember(Long memberId) {
        List<VerificationCredential> credentials = loadPort.findByMemberId(memberId);
        Long homeUniversityId = loadHomePort.findHomeUniversityId(memberId).orElse(null);
        TrustLevel trustLevel = TrustLevel.from(credentials, homeUniversityId);

        List<AdminVerificationView.Credential> rows = credentials.stream()
                .map(c -> new AdminVerificationView.Credential(
                        c.getId() != null ? c.getId().value() : null,
                        c.getUniversityId(),
                        universityNameOf(c.getUniversityId()),
                        c.getType().name(),
                        c.getStatus().name(),
                        c.getVerifiedEmail(),
                        c.getVerifiedBy(),
                        c.getNote(),
                        c.getVerifiedAt().toString(),
                        c.getRevokedBy(),
                        c.getRevokedAt() != null ? c.getRevokedAt().toString() : null))
                .toList();

        return new AdminVerificationView(memberId, trustLevel.name(), rows);
    }

    private String universityNameOf(Long universityId) {
        return loadUniversityPort.findById(new UniversityId(universityId))
                .map(u -> u.getName())
                .orElse("(알 수 없음, id=" + universityId + ")");
    }

    private String normalizeNote(String note) {
        return (note == null || note.isBlank()) ? null : note.trim();
    }
}
