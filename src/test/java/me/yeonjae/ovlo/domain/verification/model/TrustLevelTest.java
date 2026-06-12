package me.yeonjae.ovlo.domain.verification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrustLevelTest {

    private VerificationCredential schoolEmail(VerificationStatus status) {
        VerificationCredential c = VerificationCredential.issue(
                1L, VerificationType.SCHOOL_EMAIL, 100L, "alice@snu.ac.kr", Instant.now());
        if (status == VerificationStatus.EXPIRED) {
            c.expire();
        }
        return c;
    }

    @Test
    @DisplayName("자격 없으면 UNVERIFIED")
    void unverified_whenNoCredentials() {
        assertThat(TrustLevel.from(List.of())).isEqualTo(TrustLevel.UNVERIFIED);
        assertThat(TrustLevel.from(null)).isEqualTo(TrustLevel.UNVERIFIED);
    }

    @Test
    @DisplayName("활성 학교 이메일 자격이 있으면 STUDENT")
    void student_whenActiveSchoolEmail() {
        assertThat(TrustLevel.from(List.of(schoolEmail(VerificationStatus.VERIFIED))))
                .isEqualTo(TrustLevel.STUDENT);
    }

    @Test
    @DisplayName("만료된 자격만 있으면 UNVERIFIED")
    void unverified_whenOnlyExpired() {
        assertThat(TrustLevel.from(List.of(schoolEmail(VerificationStatus.EXPIRED))))
                .isEqualTo(TrustLevel.UNVERIFIED);
    }
}
