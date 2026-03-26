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
    @DisplayName("null로 Password 생성 시 허용된다 (소셜 로그인 회원은 비밀번호 없음)")
    void shouldAllow_whenNull() {
        Password password = new Password(null);
        assertThat(password.hashedValue()).isNull();
    }

    @Test
    @DisplayName("빈 값으로 Password 생성 시 허용된다 (검증은 Member.create()에서 수행)")
    void shouldAllow_whenBlank() {
        Password password = new Password("   ");
        assertThat(password.hashedValue()).isEqualTo("   ");
    }
}
