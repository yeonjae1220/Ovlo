package me.yeonjae.ovlo.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenHashUtilTest {

    @Test
    @DisplayName("동일 입력은 동일 해시를 반환한다 (역인덱스 조회 가능)")
    void shouldBeDeterministic() {
        assertThat(TokenHashUtil.sha256("my-token"))
                .isEqualTo(TokenHashUtil.sha256("my-token"));
    }

    @Test
    @DisplayName("SHA-256 hex는 64자 소문자다")
    void shouldReturn64CharLowercaseHex() {
        String hash = TokenHashUtil.sha256("my-token");
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("알려진 입력에 대한 SHA-256 벡터가 일치한다")
    void shouldMatchKnownVector() {
        // echo -n "abc" | sha256sum
        assertThat(TokenHashUtil.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("서로 다른 입력은 다른 해시를 반환한다")
    void shouldDifferForDifferentInput() {
        assertThat(TokenHashUtil.sha256("token-a"))
                .isNotEqualTo(TokenHashUtil.sha256("token-b"));
    }

    @Test
    @DisplayName("null 입력은 거부한다")
    void shouldRejectNull() {
        assertThatThrownBy(() -> TokenHashUtil.sha256(null))
                .isInstanceOf(NullPointerException.class);
    }
}
