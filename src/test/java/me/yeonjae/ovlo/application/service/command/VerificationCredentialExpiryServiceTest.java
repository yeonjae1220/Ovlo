package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.out.verification.ExpireVerificationCredentialPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationCredentialExpiryServiceTest {

    @Mock private ExpireVerificationCredentialPort expirePort;

    private final Instant now = Instant.parse("2026-06-12T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    @DisplayName("expireStale: now - ttlDays 컷오프로 포트에 위임하고 만료 건수를 반환")
    void expiresWithTtlCutoff() {
        long ttlDays = 365;
        var service = new VerificationCredentialExpiryService(expirePort, clock, ttlDays);
        given(expirePort.expireVerifiedOlderThan(now.minus(ttlDays, ChronoUnit.DAYS))).willReturn(3);

        int expired = service.expireStale();

        assertThat(expired).isEqualTo(3);
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(expirePort).expireVerifiedOlderThan(cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(now.minus(365, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("커스텀 TTL(30일)도 컷오프에 반영된다")
    void honorsCustomTtl() {
        var service = new VerificationCredentialExpiryService(expirePort, clock, 30);
        given(expirePort.expireVerifiedOlderThan(now.minus(30, ChronoUnit.DAYS))).willReturn(0);

        assertThat(service.expireStale()).isZero();
        verify(expirePort).expireVerifiedOlderThan(now.minus(30, ChronoUnit.DAYS));
    }
}
