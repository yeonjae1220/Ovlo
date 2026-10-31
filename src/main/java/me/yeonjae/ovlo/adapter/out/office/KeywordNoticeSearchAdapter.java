package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.NoticeSearchPort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeTranslation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * DB 에 의존하지 않는 후보 검색(기본, 로컬·테스트·H2). 오늘 유효한 공지를 모두 읽어 질문과 겹치는 토큰 수로 점수를 매긴다.
 * 한국어·일본어·중국어처럼 띄어쓰기로 단어가 갈리지 않는 글은 글자 2개 묶음(bigram)으로 비교한다.
 * 운영(PostgreSQL)은 {@code ovlo.search.provider=postgres} 로 PostgresFtsNoticeSearchAdapter 를 쓴다(ADR-0006).
 */
@Component
@ConditionalOnProperty(name = "ovlo.search.provider", havingValue = "keyword", matchIfMissing = true)
public class KeywordNoticeSearchAdapter implements NoticeSearchPort {

    private final LoadNoticePort loadNoticePort;

    public KeywordNoticeSearchAdapter(LoadNoticePort loadNoticePort) {
        this.loadNoticePort = loadNoticePort;
    }

    @Override
    public List<Notice> findCandidates(Long officeId, NoticeLang lang, String question, LocalDate today, int limit) {
        Set<String> queryTokens = tokens(question);
        if (queryTokens.isEmpty()) return List.of();
        return loadNoticePort.findActiveForStudent(officeId, null, today).stream()
                .map(n -> new Scored(n, score(queryTokens, textOf(n, lang))))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.score).reversed()
                        .thenComparing(s -> s.notice.getId(), Comparator.reverseOrder()))
                .limit(limit)
                .map(s -> s.notice)
                .toList();
    }

    private record Scored(Notice notice, int score) {}

    /** 원문 + 학생 언어 번역(있으면)을 함께 본다 — 어느 쪽 표현으로 물어도 걸리게. */
    private static String textOf(Notice n, NoticeLang lang) {
        StringBuilder sb = new StringBuilder(n.getTitle()).append('\n').append(n.getBody());
        n.getTranslation(lang).filter(t -> !t.isStaleFor(n.getVersion()))
                .ifPresent(t -> sb.append('\n').append(t.getTitle()).append('\n').append(t.getBody()));
        return sb.toString();
    }

    static int score(Set<String> queryTokens, String text) {
        Set<String> textTokens = tokens(text);
        int hits = 0;
        for (String q : queryTokens) {
            if (textTokens.contains(q)) hits++;
        }
        return hits;
    }

    /** 라틴·키릴 문자 단어(2자 이상)와 CJK 글자 bigram. 숫자는 그대로 토큰. */
    static Set<String> tokens(String text) {
        Set<String> out = new HashSet<>();
        if (text == null) return out;
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder word = new StringBuilder();
        StringBuilder cjk = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (isCjk(ch)) {
                flushWord(word, out);
                cjk.append(ch);
            } else {
                flushCjk(cjk, out);
                if (Character.isLetterOrDigit(ch)) word.append(ch);
                else flushWord(word, out);
            }
        }
        flushWord(word, out);
        flushCjk(cjk, out);
        return out;
    }

    private static boolean isCjk(char ch) {
        Character.UnicodeScript s = Character.UnicodeScript.of(ch);
        return s == Character.UnicodeScript.HANGUL || s == Character.UnicodeScript.HAN
                || s == Character.UnicodeScript.HIRAGANA || s == Character.UnicodeScript.KATAKANA;
    }

    private static void flushWord(StringBuilder word, Set<String> out) {
        if (word.length() >= 2) out.add(word.toString());
        word.setLength(0);
    }

    private static void flushCjk(StringBuilder cjk, Set<String> out) {
        if (cjk.length() == 1) out.add(cjk.toString());
        for (int i = 0; i + 1 < cjk.length(); i++) out.add(cjk.substring(i, i + 2));
        cjk.setLength(0);
    }
}
