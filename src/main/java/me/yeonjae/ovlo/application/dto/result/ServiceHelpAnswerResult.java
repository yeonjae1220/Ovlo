package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.help.model.HelpAnswerOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.util.List;

/** answerText 의 [N] 은 citations 의 index 와 같다. ANSWERED 가 아니면 answerText 는 null. */
public record ServiceHelpAnswerResult(HelpAnswerOutcome outcome, NoticeLang lang, String answerText,
                                      List<Citation> citations) {

    public ServiceHelpAnswerResult {
        citations = List.copyOf(citations);
    }

    public record Citation(int index, String articleId, String title) {}

    public static ServiceHelpAnswerResult withoutAnswer(HelpAnswerOutcome outcome, NoticeLang lang) {
        return new ServiceHelpAnswerResult(outcome, lang, null, List.of());
    }
}
