package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import org.springframework.stereotype.Component;

/**
 * 학생 소속 검사(ADR-0005). 그 Office 의 대학에 **활성 인증 자격**이 있어야 한다.
 * homeUniversityId 는 자기 신고 값이라 쓰지 않는다. 정지된 Office 는 SUSPENDED.
 */
@Component
public class StudentOfficeAccessPolicy {

    private final LoadOfficePort loadOfficePort;
    private final LoadVerificationCredentialPort loadCredentialPort;

    public StudentOfficeAccessPolicy(LoadOfficePort loadOfficePort,
                                     LoadVerificationCredentialPort loadCredentialPort) {
        this.loadOfficePort = loadOfficePort;
        this.loadCredentialPort = loadCredentialPort;
    }

    public Office assertMember(Long memberId, Long officeId) {
        Office office = loadOfficePort.findById(officeId)
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다: id=" + officeId,
                        OfficeException.ErrorType.NOT_FOUND));
        if (office.isSuspended()) {
            throw new OfficeException("정지된 국제처입니다", OfficeException.ErrorType.SUSPENDED);
        }
        if (!isVerifiedAt(memberId, office.getGlobalUnivId())) {
            throw new OfficeException("이 대학의 인증된 학생만 이용할 수 있습니다",
                    OfficeException.ErrorType.FORBIDDEN);
        }
        return office;
    }

    public boolean isVerifiedAt(Long memberId, Long globalUnivId) {
        return loadCredentialPort.findByMemberId(memberId).stream()
                .filter(VerificationCredential::isActive)
                .anyMatch(c -> c.getUniversityId().equals(globalUnivId));
    }
}
