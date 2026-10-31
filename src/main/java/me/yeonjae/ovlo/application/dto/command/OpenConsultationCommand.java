package me.yeonjae.ovlo.application.dto.command;

/**
 * inquiryId 가 있으면 그 질문·자동 답변이 공유 맥락이 되고, 없으면 message 가 상담의 첫 메시지가 된다.
 * consentVersion 은 학생이 본 동의 문구 버전(ConsultationConsent).
 */
public record OpenConsultationCommand(Long officeId, Long memberId, Long inquiryId, String consentVersion, String message) {
}
