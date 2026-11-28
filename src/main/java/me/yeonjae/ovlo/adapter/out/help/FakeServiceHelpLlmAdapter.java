package me.yeonjae.ovlo.adapter.out.help;

import me.yeonjae.ovlo.adapter.out.office.KeywordNoticeSearchAdapter;
import me.yeonjae.ovlo.application.port.out.help.ServiceHelpLlmPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 결정적 가짜 도움말 답변기(기본, 로컬·테스트). 질문과 단어가 겹치는 첫 문서의 첫 문장을 [N] 과 함께 돌려준다.
 * 운영은 ovlo.llm.provider=anthropic 이고 OfficePilotConfigGuard 가 fake 로 뜨는 것을 막는다.
 */
@Component
@ConditionalOnProperty(name = "ovlo.llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeServiceHelpLlmAdapter implements ServiceHelpLlmPort {

    public static final String MODEL_ID = "fake-help";

    @Override
    public Verdict screenHelp(String question) {
        return new Verdict(true, MODEL_ID, 0, 0);
    }

    @Override
    public Answer answerHelp(Request request) {
        Set<String> tokens = KeywordNoticeSearchAdapter.tokens(request.question());
        for (Reference reference : request.references()) {
            if (KeywordNoticeSearchAdapter.score(tokens, reference.title() + "\n" + reference.body()) > 0) {
                String firstSentence = reference.body().split("(?<=[.!?。])\\s*", 2)[0].trim();
                return new Answer(firstSentence + " [" + reference.index() + "]", MODEL_ID, 0, 0, false);
            }
        }
        return new Answer(GroundedAnswerValidator.NO_GROUNDS_MARKER, MODEL_ID, 0, 0, false);
    }

    @Override
    public Verdict verifyHelp(Request request, String answer) {
        return new Verdict(true, MODEL_ID, 0, 0);
    }
}
