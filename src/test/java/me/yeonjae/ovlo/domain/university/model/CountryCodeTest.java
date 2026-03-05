package me.yeonjae.ovlo.domain.university.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryCodeTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("대문자 2글자 코드로 생성할 수 있다")
        void shouldCreate_whenValidUppercase() {
            CountryCode code = new CountryCode("KR");
            assertThat(code.value()).isEqualTo("KR");
        }

        @Test
        @DisplayName("소문자 입력은 대문자로 정규화된다")
        void shouldNormalize_whenLowercase() {
            CountryCode code = new CountryCode("kr");
            assertThat(code.value()).isEqualTo("KR");
        }

        @Test
        @DisplayName("혼합 대소문자도 대문자로 정규화된다")
        void shouldNormalize_whenMixedCase() {
            CountryCode code = new CountryCode("Us");
            assertThat(code.value()).isEqualTo("US");
        }

        @Test
        @DisplayName("null이면 예외가 발생한다")
        void shouldThrow_whenNull() {
            assertThatThrownBy(() -> new CountryCode(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("빈 값이면 예외가 발생한다")
        void shouldThrow_whenBlank() {
            assertThatThrownBy(() -> new CountryCode(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("국가 코드는 ISO 3166-1 alpha-2 형식(대문자 2글자)이어야 합니다");
        }

        @Test
        @DisplayName("1글자면 예외가 발생한다")
        void shouldThrow_whenOneChar() {
            assertThatThrownBy(() -> new CountryCode("K"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("국가 코드는 ISO 3166-1 alpha-2 형식(대문자 2글자)이어야 합니다");
        }

        @Test
        @DisplayName("3글자 이상이면 예외가 발생한다")
        void shouldThrow_whenThreeChars() {
            assertThatThrownBy(() -> new CountryCode("KOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("국가 코드는 ISO 3166-1 alpha-2 형식(대문자 2글자)이어야 합니다");
        }

        @Test
        @DisplayName("숫자가 포함되면 예외가 발생한다")
        void shouldThrow_whenContainsDigit() {
            assertThatThrownBy(() -> new CountryCode("K1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("국가 코드는 ISO 3166-1 alpha-2 형식(대문자 2글자)이어야 합니다");
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 코드의 CountryCode는 동일하다")
        void shouldBeEqual_whenSameCode() {
            assertThat(new CountryCode("KR")).isEqualTo(new CountryCode("KR"));
        }

        @Test
        @DisplayName("대소문자 관계없이 같은 코드면 동일하다")
        void shouldBeEqual_whenSameCodeDifferentCase() {
            assertThat(new CountryCode("kr")).isEqualTo(new CountryCode("KR"));
        }
    }
}
