package me.yeonjae.ovlo.domain.member.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    @Test
    @DisplayName("해시된 비밀번호로 Password를 생성할 수 있다")
    void shouldCreate_whenHashedValue() {
        Password password = new Password("$2a$10$hashedvalue");
        assertThat(password.hashedValue()).isEqualTo("$2a$10$hashedvalue");
    }

    @Test
    @DisplayName("같은 해시 값의 Password는 동일하다")
    void shouldBeEqual_whenSameHash() {
        assertThat(new Password("$2a$10$hash")).isEqualTo(new Password("$2a$10$hash"));
    }

    @Test
    @DisplayName("null로 Password 생성 시 예외가 발생한다")
    void shouldThrow_whenNull() {
        assertThatThrownBy(() -> new Password(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("빈 값으로 Password 생성 시 예외가 발생한다")
    void shouldThrow_whenBlank() {
        assertThatThrownBy(() -> new Password("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호는 빈 값일 수 없습니다");
    }
}
