package me.yeonjae.ovlo.domain.help.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelpModelTest {

    @Test
    @DisplayName("질문은 앞뒤 공백을 지운 값으로 보관한다")
    void question_isTrimmed() {
        assertThat(new HelpQuestion("  탈퇴?  ").value()).isEqualTo("탈퇴?");
    }

    @Test
    @DisplayName("빈 질문과 1000자 초과 질문은 거절한다")
    void question_rejectsBlankAndTooLong() {
        assertThatThrownBy(() -> new HelpQuestion(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HelpQuestion(" \n ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HelpQuestion("가".repeat(1001))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new HelpQuestion("가".repeat(1000)).value()).hasSize(1000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Upper", "has space", "-leading", "a/b", "../etc"})
    @DisplayName("문서 id 는 소문자·숫자·하이픈만 쓴다")
    void article_rejectsBadIds(String id) {
        assertThatThrownBy(() -> new HelpArticle(id, "제목", "본문")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문서 제목과 본문은 비어 있을 수 없다")
    void article_requiresTitleAndBody() {
        assertThatThrownBy(() -> new HelpArticle("ok", " ", "본문")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HelpArticle("ok", "제목", "")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new HelpArticle("account-delete", "회원 탈퇴", "본문").id()).isEqualTo("account-delete");
    }
}
