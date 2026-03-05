package me.yeonjae.ovlo.domain.member.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    @DisplayName("유효한 이메일로 Email을 생성할 수 있다")
    void shouldCreate_whenValidEmail() {
        Email email = new Email("user@example.com");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("같은 값의 Email은 동일하다")
    void shouldBeEqual_whenSameValue() {
        assertThat(new Email("user@example.com")).isEqualTo(new Email("user@example.com"));
    }

    @Test
    @DisplayName("null로 Email 생성 시 예외가 발생한다")
    void shouldThrow_whenNull() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing@", "@nodomain.com", "no-at-sign", ""})
    @DisplayName("유효하지 않은 형식으로 Email 생성 시 예외가 발생한다")
    void shouldThrow_whenInvalidFormat(String invalid) {
        assertThatThrownBy(() -> new Email(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 이메일 형식");
    }
}
