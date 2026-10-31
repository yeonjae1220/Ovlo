package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.ConsultationActionCommand;
import me.yeonjae.ovlo.application.dto.result.ConsultationResult;

/** 담당자: 맡기(배정)·해결. 학생: 종료. 호출자 범위 검사는 어댑터가 먼저 한다. */
public interface ManageConsultationUseCase {
    /** actor 가 자신에게 배정한다. actor 는 그 Office 의 활동 담당자여야 한다. */
    ConsultationResult assign(ConsultationActionCommand command);
    ConsultationResult resolve(ConsultationActionCommand command);
    /** 학생 본인만. */
    ConsultationResult close(ConsultationActionCommand command);
    /** RESOLVED 뒤 7일 지난 상담을 CLOSED 로. 건별 트랜잭션. 반환 = 건수. */
    int closeDue();
}
