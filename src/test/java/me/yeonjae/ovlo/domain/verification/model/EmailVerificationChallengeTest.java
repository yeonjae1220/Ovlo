package me.yeonjae.ovlo.domain.verification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationChallengeTest {

    private static final Instant NOW = Instant.parse("2026-06-12T00:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(600);

    private EmailVerificationChallenge challenge(int maxAttempts) {
        return EmailVerificationChallenge.create(
                1L, 100L, "alice@snu.ac.kr",
                new VerificationCode("123456"), LATER, maxAttempts);
    }

    @Nested
    @DisplayName("코드 확인")
    class Verify {

        @Test
        @DisplayName("일치하면 SUCCESS")
        void success() {
            EmailVerificationChallenge c = challenge(5);
            assertThat(c.verify(new VerificationCode("123456"), NOW)).isEqualTo(ChallengeOutcome.SUCCESS);
            assertThat(c.getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("불일치하면 MISMATCH + 시도 1 소비")
        void mismatch() {
            EmailVerificationChallenge c = challenge(5);
            assertThat(c.verify(new VerificationCode("000000"), NOW)).isEqualTo(ChallengeOutcome.MISMATCH);
            assertThat(c.getAttemptCount()).isEqualTo(1);
            assertThat(c.remainingAttempts()).isEqualTo(4);
        }

        @Test
        @DisplayName("만료되면 코드와 무관하게 EXPIRED, 시도 미소비")
        void expired() {
            EmailVerificationChallenge c = challenge(5);
            Instant afterExpiry = LATER.plusSeconds(1);
            assertThat(c.verify(new VerificationCode("123456"), afterExpiry)).isEqualTo(ChallengeOutcome.EXPIRED);
            assertThat(c.getAttemptCount()).isZero();
        }

        @Test
        @DisplayName("시도횟수를 모두 소진하면 EXHAUSTED")
        void exhausted() {
            EmailVerificationChallenge c = challenge(2);
            c.verify(new VerificationCode("000000"), NOW); // 1
            c.verify(new VerificationCode("000000"), NOW); // 2
            assertThat(c.verify(new VerificationCode("123456"), NOW)).isEqualTo(ChallengeOutcome.EXHAUSTED);
            assertThat(c.remainingAttempts()).isZero();
        }
    }

    @Nested
    @DisplayName("생성 불변식")
    class Create {

        @Test
        @DisplayName("빈 이메일은 예외")
        void blankEmail() {
            assertThatThrownBy(() -> EmailVerificationChallenge.create(
                    1L, 100L, "  ", new VerificationCode("123456"), LATER, 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("최대 시도횟수 0 이하는 예외")
        void nonPositiveMaxAttempts() {
            assertThatThrownBy(() -> EmailVerificationChallenge.create(
                    1L, 100L, "a@b.edu", new VerificationCode("123456"), LATER, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
