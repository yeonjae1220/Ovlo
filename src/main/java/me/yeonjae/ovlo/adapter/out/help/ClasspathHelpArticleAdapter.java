package me.yeonjae.ovlo.adapter.out.help;

import me.yeonjae.ovlo.application.port.out.help.LoadHelpArticlesPort;
import me.yeonjae.ovlo.domain.help.model.HelpArticle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@code classpath:help/ko/*.md} 를 기동 시 한 번 읽는다. 파일명 순서가 화면·인용 순서다.
 * 형식: 파일명 {@code NN-id.md}, 첫 줄 {@code # 제목}, 나머지 본문(평문, "- " 목록 허용).
 * 문서가 잘못되면 조용히 빠뜨리지 않고 기동을 막는다 — 빠진 문서는 AI 가 "근거 없음"으로 답하게 만든다.
 */
@Component
public class ClasspathHelpArticleAdapter implements LoadHelpArticlesPort {

    private static final String LOCATION = "classpath:help/ko/*.md";
    private static final String NUMBER_PREFIX = "^\\d+-";

    private final List<HelpArticle> articles;

    public ClasspathHelpArticleAdapter() {
        this.articles = validate(read());
    }

    @Override
    public List<HelpArticle> loadAll() {
        return articles;
    }

    static HelpArticle parse(String filename, String content) {
        String text = content.replace("\r\n", "\n").strip();
        if (!text.startsWith("# ")) {
            throw new IllegalStateException("도움말 문서 첫 줄은 '# 제목' 이어야 합니다: " + filename);
        }
        int lineEnd = text.indexOf('\n');
        String title = (lineEnd < 0 ? text : text.substring(0, lineEnd)).substring(2).strip();
        String body = lineEnd < 0 ? "" : text.substring(lineEnd + 1).strip();
        String id = filename.replaceFirst("\\.md$", "").replaceFirst(NUMBER_PREFIX, "");
        try {
            return new HelpArticle(id, title, body);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalStateException(filename + ": " + invalid.getMessage(), invalid);
        }
    }

    static List<HelpArticle> validate(List<HelpArticle> loaded) {
        if (loaded.isEmpty()) {
            throw new IllegalStateException("도움말 문서가 없습니다: " + LOCATION);
        }
        Set<String> ids = new HashSet<>();
        int bytes = 0;
        for (HelpArticle article : loaded) {
            if (!ids.add(article.id())) {
                throw new IllegalStateException("도움말 문서 id 가 겹칩니다: " + article.id());
            }
            bytes += (article.title() + article.body()).getBytes(StandardCharsets.UTF_8).length;
        }
        if (bytes > MAX_CORPUS_BYTES) {
            throw new IllegalStateException("도움말 문서 전체가 " + MAX_CORPUS_BYTES + "바이트를 넘습니다: " + bytes);
        }
        return List.copyOf(loaded);
    }

    private static List<HelpArticle> read() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
            return Arrays.stream(resources)
                    .sorted(Comparator.comparing(r -> Objects.requireNonNull(r.getFilename())))
                    .map(ClasspathHelpArticleAdapter::parse)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("도움말 문서를 읽지 못했습니다", e);
        }
    }

    private static HelpArticle parse(Resource resource) {
        try {
            return parse(resource.getFilename(), resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("도움말 문서를 읽지 못했습니다: " + resource.getFilename(), e);
        }
    }
}
