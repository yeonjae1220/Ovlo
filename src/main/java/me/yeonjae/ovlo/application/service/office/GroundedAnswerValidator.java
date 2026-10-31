package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort.Candidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * LLM 출력의 최종 게이트(ADR-0006 4항). 통과하지 못하면 호출자가 NO_GROUNDS 로 강등하고 기록한다.
 * - "NO_GROUNDS" 라고 답했으면 근거 없음
 * - [N] 인용이 하나도 없으면 근거 없음
 * - [N] 이 후보 범위(1..n) 밖이면 모델이 지시를 어긴 것 — 근거 없음(경고)
 * - URL 은 후보의 sourceUrl 만 남기고 나머지는 지운다
 */
@Component
public class GroundedAnswerValidator {

    public static final String NO_GROUNDS_MARKER = "NO_GROUNDS";

    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,3})]");
    private static final Pattern URL = Pattern.compile("https?://[^\\s\\])>\"']+");

    public record Validated(String text, List<Long> citedNoticeIds, List<Integer> citedIndices) {}

    public record Rejection(String reason) {}

    /** 왼쪽: 거부 사유, 오른쪽: 검증된 답변. */
    public record Outcome(Validated validated, Rejection rejection) {
        public boolean passed() { return validated != null; }
    }

    public Outcome validate(String rawText, List<Candidate> candidates) {
        if (rawText == null || rawText.isBlank()) {
            return reject("빈 응답");
        }
        String text = rawText.trim();
        if (text.toUpperCase().startsWith(NO_GROUNDS_MARKER)) {
            return reject("모델이 근거 없음을 답함");
        }
        Map<Integer, Candidate> byIndex = candidates.stream()
                .collect(Collectors.toMap(Candidate::index, Function.identity()));

        Set<Integer> indices = new LinkedHashSet<>();
        Matcher m = CITATION.matcher(text);
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            if (!byIndex.containsKey(idx)) {
                return reject("후보 범위 밖 인용 [" + idx + "]");
            }
            indices.add(idx);
        }
        if (indices.isEmpty()) {
            return reject("인용 없음");
        }

        Set<String> allowedUrls = candidates.stream()
                .map(Candidate::sourceUrl)
                .filter(u -> u != null && !u.isBlank())
                .collect(Collectors.toSet());
        String cleaned = URL.matcher(text).replaceAll(r -> allowedUrls.contains(r.group()) ? r.group() : "")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return reject("URL 제거 뒤 본문 없음");
        }

        List<Integer> orderedIndices = new ArrayList<>(indices);
        List<Long> ids = orderedIndices.stream().map(i -> byIndex.get(i).noticeId()).distinct().toList();
        return new Outcome(new Validated(cleaned, ids, orderedIndices), null);
    }

    private static Outcome reject(String reason) {
        return new Outcome(null, new Rejection(reason));
    }

    public static Optional<Validated> asOptional(Outcome outcome) {
        return Optional.ofNullable(outcome.validated());
    }
}
