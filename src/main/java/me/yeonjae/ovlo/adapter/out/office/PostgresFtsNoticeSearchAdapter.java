package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.NoticeSearchPort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PostgreSQL 전문 검색(ADR-0006). 질문을 단어로 쪼개 OR 질의(to_tsquery)로 만들고, 원문과 학생 언어 번역을
 * 'simple' 구성으로 색인해 **실제로 매칭된** 문서만 ts_rank 합으로 정렬한다(ts_rank 는 매칭 여부를 강제하지 않으므로 @@ 를 같이 건다).
 * 한국어·일본어·중국어 질문은 띄어쓰기 단위 매칭이 약해 글자 묶음(bigram) 점수(KeywordNoticeSearchAdapter)로 보강한다.
 * 자료가 수백 건 수준이라 식 인덱스 없이 질의 시점에 to_tsvector 를 계산한다. 운영에서 {@code ovlo.search.provider=postgres}.
 */
@Component
@ConditionalOnProperty(name = "ovlo.search.provider", havingValue = "postgres")
public class PostgresFtsNoticeSearchAdapter implements NoticeSearchPort {

    private static final String SQL = """
            SELECT id FROM (
              SELECT n.id,
                     CASE WHEN to_tsvector('simple', n.title || ' ' || n.body) @@ q
                          THEN ts_rank(to_tsvector('simple', n.title || ' ' || n.body), q) ELSE 0 END AS r_src,
                     COALESCE((SELECT MAX(ts_rank(to_tsvector('simple', t.title || ' ' || t.body), q))
                                 FROM office_notice_translation t
                                WHERE t.notice_id = n.id AND t.lang = ?
                                  AND to_tsvector('simple', t.title || ' ' || t.body) @@ q), 0) AS r_tr
                FROM office_notice n, to_tsquery('simple', ?) q
               WHERE n.office_id = ?
                 AND n.status = 'APPROVED'
                 AND n.effective_from <= ?
                 AND n.expires_at >= ?
            ) s
            WHERE r_src > 0 OR r_tr > 0
            ORDER BY (r_src + r_tr) DESC, id DESC
            LIMIT ?
            """;

    /** 'simple' 구성은 불용어를 빼지 않는다 — 어느 문서에나 있는 기능어가 후보를 채우지 않게 조금만 뺀다. */
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "your", "with", "this", "that", "from", "have", "has",
            "what", "when", "where", "which", "who", "how", "does", "did", "can", "could", "will", "would", "should",
            "là", "và", "của", "các", "cho", "với", "der", "die", "das", "und", "ist", "les", "des", "une", "est", "pour");

    private final JdbcTemplate jdbc;
    private final LoadNoticePort loadNoticePort;
    private final KeywordNoticeSearchAdapter cjkFallback;

    public PostgresFtsNoticeSearchAdapter(JdbcTemplate jdbc, LoadNoticePort loadNoticePort) {
        this.jdbc = jdbc;
        this.loadNoticePort = loadNoticePort;
        this.cjkFallback = new KeywordNoticeSearchAdapter(loadNoticePort);
    }

    @Override
    public List<Notice> findCandidates(Long officeId, NoticeLang lang, String question, LocalDate today, int limit) {
        Map<Long, Notice> merged = new LinkedHashMap<>();
        String tsquery = toOrQuery(question);
        if (!tsquery.isEmpty()) {
            List<Long> ids = jdbc.queryForList(SQL, Long.class, lang.code(), tsquery, officeId, today, today, limit);
            for (Long id : ids) {
                loadNoticePort.findById(id).ifPresent(n -> merged.put(n.getId(), n));
            }
        }
        if (merged.size() < limit && containsCjk(question)) {
            for (Notice n : cjkFallback.findCandidates(officeId, lang, question, today, limit)) {
                merged.putIfAbsent(n.getId(), n);
                if (merged.size() >= limit) break;
            }
        }
        return new ArrayList<>(merged.values());
    }

    /** "when does the dormitory application close" → "dormitory | application | close". 길이 3 미만·불용어·따옴표 제거. */
    static String toOrQuery(String question) {
        if (question == null) return "";
        List<String> terms = new ArrayList<>();
        for (String raw : question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (raw.length() < 3 || STOPWORDS.contains(raw) || containsCjk(raw)) continue;
            if (!terms.contains(raw)) terms.add(raw);
        }
        return terms.stream().map(t -> "'" + t.replace("'", "''") + "'").collect(Collectors.joining(" | "));
    }

    static boolean containsCjk(String s) {
        return s != null && s.chars().anyMatch(ch -> {
            Character.UnicodeScript sc = Character.UnicodeScript.of(ch);
            return sc == Character.UnicodeScript.HANGUL || sc == Character.UnicodeScript.HAN
                    || sc == Character.UnicodeScript.HIRAGANA || sc == Character.UnicodeScript.KATAKANA;
        });
    }

    @SuppressWarnings("unused")
    private static Optional<Notice> none() {
        return Optional.empty();
    }
}
