package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.AskQuestionCommand;
import me.yeonjae.ovlo.application.dto.result.AskResult;

/** 학생 질문 → 후보 검색 → 근거 기반 답변 → 인용 검증 → 기록. 회원 단위 요청 제한은 어댑터가 먼저 한다. */
public interface AskQuestionUseCase {
    AskResult ask(AskQuestionCommand command);
}
