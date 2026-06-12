package me.yeonjae.ovlo.domain.verification.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationCodeTest {

    @Test
    @DisplayName("6자리 숫자(선행 0 포함)는 유효")
    void valid() {
        assertThat(new VerificationCode("000123").value()).isEqualTo("000123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12a456", "abcdef", "", "  "})
    @DisplayName("6자리 숫자가 아니면 예외")
    void invalid(String raw) {
        assertThatThrownBy(() -> new VerificationCode(raw))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("matches는 동일 코드에 true, 다르면 false")
    void matches() {
        VerificationCode code = new VerificationCode("123456");
        assertThat(code.matches(new VerificationCode("123456"))).isTrue();
        assertThat(code.matches(new VerificationCode("123457"))).isFalse();
        assertThat(code.matches(null)).isFalse();
    }
}
