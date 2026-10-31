package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 결정적 가짜 답변기(기본, 로컬·테스트). 질문과 토큰이 겹치는 첫 후보의 첫 문장을 [N] 인용과 함께 돌려주고,
 * 겹치는 후보가 없으면 NO_GROUNDS. 운영은 ovlo.llm.provider=anthropic.
 */
@Component
@ConditionalOnProperty(name = "ovlo.llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAnswerAdapter implements AnswerGenerationPort {

    public static final String MODEL_ID = "fake-answerer";

    @Override
    public Result answer(Request request) {
        Set<String> q = KeywordNoticeSearchAdapter.tokens(request.question());
        for (Candidate c : request.candidates()) {
            if (KeywordNoticeSearchAdapter.score(q, c.title() + "\n" + c.body()) > 0) {
                String firstSentence = c.body().split("(?<=[.!?。])\\s*", 2)[0].trim();
                return new Result(firstSentence + " [" + c.index() + "]", MODEL_ID, 0, 0, false);
            }
        }
        return new Result(GroundedAnswerValidator.NO_GROUNDS_MARKER, MODEL_ID, 0, 0, false);
    }
}
