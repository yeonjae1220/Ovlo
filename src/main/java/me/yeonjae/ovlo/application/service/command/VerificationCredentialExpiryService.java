package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.in.verification.ExpireStaleCredentialsUseCase;
import me.yeonjae.ovlo.application.port.out.verification.ExpireVerificationCredentialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 인증 자격 만료 처리.
 * 학교 이메일 인증은 영구가 아니므로 TTL(기본 365일)이 지난 활성 자격을 EXPIRED로 전환한다.
 * 만료된 자격은 TrustLevel 파생에서 제외되어 등급이 자연스럽게 내려간다.
 */
@Service
public class VerificationCredentialExpiryService implements ExpireStaleCredentialsUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerificationCredentialExpiryService.class);

    private final ExpireVerificationCredentialPort expirePort;
    private final Clock clock;
    private final long ttlDays;

    public VerificationCredentialExpiryService(ExpireVerificationCredentialPort expirePort,
                                               Clock clock,
                                               @Value("${ovlo.verification.credential-ttl-days:365}") long ttlDays) {
        this.expirePort = expirePort;
        this.clock = clock;
        this.ttlDays = ttlDays;
    }

    @Override
    @Transactional
    public int expireStale() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(ttlDays));
        int expired = expirePort.expireVerifiedOlderThan(cutoff);
        if (expired > 0) {
            log.info("[Verification] 만료 처리된 인증 자격 {}건 (cutoff={}, ttlDays={})", expired, cutoff, ttlDays);
        }
        return expired;
    }
}
