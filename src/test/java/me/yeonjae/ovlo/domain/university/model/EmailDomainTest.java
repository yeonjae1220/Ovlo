package me.yeonjae.ovlo.domain.university.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailDomainTest {

    @ParameterizedTest
    @CsvSource({
            "alice@snu.ac.kr,           snu.ac.kr",
            "Alice@STUDENT.UvA.NL,      student.uva.nl",   // 소문자화
            "bob+exchange@u.nus.edu,    u.nus.edu",        // +alias는 도메인에 영향 없음
            "carol@connect.hku.hk.,     connect.hku.hk",   // 트레일링 닷 제거
            "  dave@kth.se  ,           kth.se"            // 공백 트림
    })
    @DisplayName("이메일에서 도메인을 추출·정규화한다")
    void fromEmail_normalizes(String email, String expected) {
        assertThat(EmailDomain.fromEmail(email).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-at-sign", "@nodomain.com", "trailing@", "local@nodot", "  "})
    @DisplayName("올바르지 않은 이메일은 예외가 발생한다")
    void fromEmail_rejectsInvalid(String email) {
        assertThatThrownBy(() -> EmailDomain.fromEmail(email))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 이메일은 예외가 발생한다")
    void fromEmail_rejectsNull() {
        assertThatThrownBy(() -> EmailDomain.fromEmail(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일은 필수");
    }
}
