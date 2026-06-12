package me.yeonjae.ovlo.domain.verification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrustLevelTest {

    private VerificationCredential schoolEmail(VerificationStatus status) {
        return schoolEmailAt(100L, status);
    }

    private VerificationCredential schoolEmailAt(long universityId, VerificationStatus status) {
        VerificationCredential c = VerificationCredential.issue(
                1L, VerificationType.SCHOOL_EMAIL, universityId, "alice@univ.edu", Instant.now());
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

    @Test
    @DisplayName("본교(200)와 다른 대학(100) 학교이메일 인증 → EXCHANGE_VERIFIED")
    void exchangeVerified_whenSchoolEmailAtNonHomeUniversity() {
        assertThat(TrustLevel.from(List.of(schoolEmailAt(100L, VerificationStatus.VERIFIED)), 200L))
                .isEqualTo(TrustLevel.EXCHANGE_VERIFIED);
    }

    @Test
    @DisplayName("본교(100)와 같은 대학 학교이메일 인증 → STUDENT (파견 아님)")
    void student_whenSchoolEmailAtHomeUniversity() {
        assertThat(TrustLevel.from(List.of(schoolEmailAt(100L, VerificationStatus.VERIFIED)), 100L))
                .isEqualTo(TrustLevel.STUDENT);
    }

    @Test
    @DisplayName("본교가 null이면 교환 판정 불가 → STUDENT")
    void student_whenHomeUniversityNull() {
        assertThat(TrustLevel.from(List.of(schoolEmailAt(100L, VerificationStatus.VERIFIED)), null))
                .isEqualTo(TrustLevel.STUDENT);
    }

    @Test
    @DisplayName("본교 자격 + 비본교 자격 동시 보유 → EXCHANGE_VERIFIED 우선")
    void exchangeVerified_whenBothHomeAndNonHome() {
        assertThat(TrustLevel.from(
                List.of(schoolEmailAt(100L, VerificationStatus.VERIFIED),
                        schoolEmailAt(200L, VerificationStatus.VERIFIED)), 100L))
                .isEqualTo(TrustLevel.EXCHANGE_VERIFIED);
    }

    @Test
    @DisplayName("비본교지만 만료된 자격만 있으면 EXCHANGE 아님 (UNVERIFIED)")
    void notExchange_whenNonHomeButExpired() {
        assertThat(TrustLevel.from(List.of(schoolEmailAt(200L, VerificationStatus.EXPIRED)), 100L))
                .isEqualTo(TrustLevel.UNVERIFIED);
    }
}
