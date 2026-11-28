package me.yeonjae.ovlo.application.port.out.help;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.util.List;

/**
 * 서비스 도움말 전용 분류·답변·검증. 국제처 프롬프트와 섞지 않는다.
 * 구현: FakeServiceHelpLlmAdapter(기본) / AnthropicLlmAdapter(ovlo.llm.provider=anthropic).
 */
public interface ServiceHelpLlmPort {

    /** index 는 1부터. 답변의 [index] 인용 번호와 같다. */
    record Reference(int index, String title, String body) {}

    record Request(String question, NoticeLang lang, List<Reference> references) {
        public Request { references = List.copyOf(references); }
    }

    record Verdict(boolean allowed, String modelId, long inputTokens, long outputTokens) {}

    /** refused 면 안전 분류기가 응답을 거부한 것 — text 는 비어 있다. */
    record Answer(String text, String modelId, long inputTokens, long outputTokens, boolean refused) {}

    Verdict screenHelp(String question);

    Answer answerHelp(Request request);

    Verdict verifyHelp(Request request, String answer);
}
