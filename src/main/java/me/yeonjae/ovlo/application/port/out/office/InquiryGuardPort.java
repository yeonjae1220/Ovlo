package me.yeonjae.ovlo.application.port.out.office;

import java.util.List;

/** Separate decisions; raw screening/verification text is never shown to students. Fail closed. */
public interface InquiryGuardPort {
    record Verdict(boolean allowed, String modelId, long inputTokens, long outputTokens) {}
    Verdict screen(String question, List<AnswerGenerationPort.Turn> history);
    Verdict verify(AnswerGenerationPort.Request request, String answer);
}
