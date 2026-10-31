package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand;
import me.yeonjae.ovlo.application.dto.result.ConsultationResult;

/** 학생 동의로 상담을 열고 GROUP 채팅방(학생 + 활동 담당자 전원)을 만든다. */
public interface OpenConsultationUseCase {
    ConsultationResult open(OpenConsultationCommand command);
}
