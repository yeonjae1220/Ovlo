package me.yeonjae.ovlo.application.dto.command;

/** actorMemberId: 배정·해결은 담당자, 종료는 학생. */
public record ConsultationActionCommand(Long officeId, Long consultationId, Long actorMemberId) {
}
