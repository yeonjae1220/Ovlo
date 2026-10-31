package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.RepeatedQuestion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 반복 문의 1차 묶기(설계 S5): 소문자·문장부호 제거·공백 정규화 뒤 완전 일치로 묶는다.
 * 의미 유사도(임베딩)는 ADR-0006 대로 파일럿 이후. 순수 함수 — 프레임워크 의존 없음.
 */
public final class InquiryClustering {

    private static final Pattern STRIP = Pattern.compile("[\\p{P}\\p{S}]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private InquiryClustering() {}

    /** questions 는 최신순. 2건 이상 묶인 그룹만, 건수 내림차순으로 top 개. sample 은 그룹의 첫(최신) 원문. */
    public static List<RepeatedQuestion> cluster(List<String> questions, int top) {
        Map<String, Group> groups = new LinkedHashMap<>();
        for (String q : questions) {
            if (q == null) continue;
            String key = normalize(q);
            if (key.isEmpty()) continue;
            groups.computeIfAbsent(key, k -> new Group(q.trim())).count++;
        }
        List<RepeatedQuestion> out = new ArrayList<>();
        for (Group g : groups.values()) {
            if (g.count >= 2) out.add(new RepeatedQuestion(g.sample, g.count));
        }
        out.sort(Comparator.comparingLong(RepeatedQuestion::count).reversed()
                .thenComparing(RepeatedQuestion::sample));
        return out.size() > top ? List.copyOf(out.subList(0, top)) : List.copyOf(out);
    }

    static String normalize(String q) {
        String s = STRIP.matcher(q.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return SPACES.matcher(s).replaceAll(" ").trim();
    }

    private static final class Group {
        final String sample;
        long count;
        Group(String sample) { this.sample = sample; }
    }
}
