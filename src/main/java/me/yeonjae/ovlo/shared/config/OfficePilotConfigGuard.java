package me.yeonjae.ovlo.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 운영(prod) 기동 시 국제처 파일럿 설정을 검사한다(설계 §8 "기동 시 존재를 검사").
 * fake LLM·keyword 검색으로 운영에 뜨면 학생에게 가짜 답이 나가고 아무 로그도 남지 않으므로 기동을 막는다.
 * 의도적으로 낮춰 띄워야 할 때만 OVLO_OFFICE_ALLOW_DEGRADED=true 로 경고만 남기고 지나간다.
 */
@Component
@Profile("prod")
public class OfficePilotConfigGuard {

    private static final Logger log = LoggerFactory.getLogger(OfficePilotConfigGuard.class);

    public OfficePilotConfigGuard(@Value("${ovlo.llm.provider:fake}") String llmProvider,
                                  @Value("${ovlo.llm.anthropic.api-key:}") String apiKey,
                                  @Value("${ovlo.search.provider:keyword}") String searchProvider,
                                  @Value("${ovlo.office.allow-degraded:false}") boolean allowDegraded) {
        List<String> problems = check(llmProvider, apiKey, searchProvider);
        if (problems.isEmpty()) {
            log.info("[office] 파일럿 설정 확인: llm={} search={}", llmProvider, searchProvider);
            return;
        }
        if (allowDegraded) {
            log.warn("[office] 운영 설정이 낮춰져 있습니다(OVLO_OFFICE_ALLOW_DEGRADED=true): {}", problems);
            return;
        }
        throw new IllegalStateException("국제처 파일럿 운영 설정이 잘못됐습니다: " + String.join("; ", problems)
                + ". 의도한 것이면 OVLO_OFFICE_ALLOW_DEGRADED=true");
    }

    static List<String> check(String llmProvider, String apiKey, String searchProvider) {
        List<String> problems = new ArrayList<>();
        if (!"anthropic".equals(llmProvider)) {
            problems.add("OVLO_LLM_PROVIDER=" + llmProvider + " (anthropic 이어야 함)");
        } else if (apiKey == null || apiKey.isBlank()) {
            problems.add("ANTHROPIC_API_KEY 비어 있음");
        }
        if (!"postgres".equals(searchProvider)) {
            problems.add("OVLO_SEARCH_PROVIDER=" + searchProvider + " (postgres 여야 함)");
        }
        return problems;
    }
}
