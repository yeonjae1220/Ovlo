package me.yeonjae.ovlo.domain.member.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageSkillTest {

    @Test
    @DisplayName("언어 코드와 레벨로 LanguageSkill을 생성할 수 있다")
    void shouldCreate_whenValidInputs() {
        LanguageSkill skill = new LanguageSkill("en", CefrLevel.B2);
        assertThat(skill.languageCode()).isEqualTo("en");
        assertThat(skill.level()).isEqualTo(CefrLevel.B2);
    }

    @Test
    @DisplayName("같은 언어 코드와 레벨의 LanguageSkill은 동일하다")
    void shouldBeEqual_whenSameValues() {
        assertThat(new LanguageSkill("fr", CefrLevel.C1))
                .isEqualTo(new LanguageSkill("fr", CefrLevel.C1));
    }

    @Test
    @DisplayName("null 언어 코드로 생성 시 예외가 발생한다")
    void shouldThrow_whenNullLanguageCode() {
        assertThatThrownBy(() -> new LanguageSkill(null, CefrLevel.B1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("빈 언어 코드로 생성 시 예외가 발생한다")
    void shouldThrow_whenBlankLanguageCode() {
        assertThatThrownBy(() -> new LanguageSkill("  ", CefrLevel.B1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("언어 코드는 빈 값일 수 없습니다");
    }

    @Test
    @DisplayName("null 레벨로 생성 시 예외가 발생한다")
    void shouldThrow_whenNullLevel() {
        assertThatThrownBy(() -> new LanguageSkill("ko", null))
                .isInstanceOf(NullPointerException.class);
    }
}
