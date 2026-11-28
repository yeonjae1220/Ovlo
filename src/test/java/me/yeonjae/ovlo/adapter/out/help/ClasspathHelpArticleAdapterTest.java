package me.yeonjae.ovlo.adapter.out.help;

import me.yeonjae.ovlo.application.port.out.help.LoadHelpArticlesPort;
import me.yeonjae.ovlo.domain.help.model.HelpArticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathHelpArticleAdapterTest {

    @Test
    @DisplayName("파일명 앞 번호를 떼어 id 로, 첫 줄 '# ' 을 제목으로, 나머지를 본문으로 읽는다")
    void parse_readsIdTitleAndBody() {
        HelpArticle article = ClasspathHelpArticleAdapter.parse("03-account-delete.md",
                "# 회원 탈퇴\n\n프로필에서 탈퇴합니다.\n- 단계 1\n");

        assertThat(article).isEqualTo(new HelpArticle("account-delete", "회원 탈퇴", "프로필에서 탈퇴합니다.\n- 단계 1"));
    }

    @Test
    @DisplayName("제목 줄이 없으면 기동을 막는다")
    void parse_requiresHeading() {
        assertThatThrownBy(() -> ClasspathHelpArticleAdapter.parse("01-x.md", "본문만 있음"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("01-x.md");
    }

    @Test
    @DisplayName("id 가 겹치거나 전체 크기가 한도를 넘으면 기동을 막는다")
    void validate_rejectsDuplicatesAndOversizedCorpus() {
        var a = new HelpArticle("same", "가", "본문");
        assertThatThrownBy(() -> ClasspathHelpArticleAdapter.validate(List.of(a, a)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("same");
        var big = new HelpArticle("big", "큰 문서", "x".repeat(LoadHelpArticlesPort.MAX_CORPUS_BYTES));
        assertThatThrownBy(() -> ClasspathHelpArticleAdapter.validate(List.of(big)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ClasspathHelpArticleAdapter.validate(List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("저장소에 들어 있는 실제 도움말 문서가 한도 안에서 모두 읽힌다")
    void bundledArticles_loadWithinLimits() {
        List<HelpArticle> loaded = new ClasspathHelpArticleAdapter().loadAll();

        assertThat(loaded).hasSizeGreaterThanOrEqualTo(5);
        assertThat(new HashSet<>(loaded.stream().map(HelpArticle::id).toList())).hasSameSizeAs(loaded);
        int bytes = loaded.stream()
                .mapToInt(a -> (a.title() + a.body()).getBytes(StandardCharsets.UTF_8).length).sum();
        assertThat(bytes).isLessThanOrEqualTo(LoadHelpArticlesPort.MAX_CORPUS_BYTES);
    }
}
