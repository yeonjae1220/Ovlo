package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.util.List;

/**
 * 근거 기반 답변 생성(설계 §5.2, §8). 후보 자료만 넣고, 학생 식별 정보는 넣지 않는다.
 * 모델이 지시를 어겼는지는 호출자(GroundedAnswerValidator)가 다시 검사한다.
 * 구현: FakeAnswerAdapter(기본) / AnthropicLlmAdapter(ovlo.llm.provider=anthropic).
 */
public interface AnswerGenerationPort {

    /** index 는 1부터. 프롬프트의 [index] 인용 번호와 같다. */
    record Candidate(int index, Long noticeId, String title, String body, String sourceUrl) {}

    record Request(String question, NoticeLang lang, List<Candidate> candidates) {}

    /** refused 면 안전 분류기가 응답을 거부한 것 — text 는 비어 있다. */
    record Result(String text, String modelId, long inputTokens, long outputTokens, boolean refused) {}

    Result answer(Request request);
}
