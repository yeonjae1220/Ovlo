package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.verification.ExpireStaleCredentialsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 인증 자격 만료 스케줄러 (inbound adapter).
 * 기본 매일 04:00(KST 아님—서버 TZ)에 TTL 경과 자격을 EXPIRED로 전환한다.
 * 크론은 {@code ovlo.verification.expiry-cron}으로 조정 가능.
 */
@Component
public class VerificationCredentialExpiryScheduler {

    private final ExpireStaleCredentialsUseCase expireStaleCredentials;

    public VerificationCredentialExpiryScheduler(ExpireStaleCredentialsUseCase expireStaleCredentials) {
        this.expireStaleCredentials = expireStaleCredentials;
    }

    @Scheduled(cron = "${ovlo.verification.expiry-cron:0 0 4 * * *}")
    public void expireStaleCredentials() {
        expireStaleCredentials.expireStale();
    }
}
