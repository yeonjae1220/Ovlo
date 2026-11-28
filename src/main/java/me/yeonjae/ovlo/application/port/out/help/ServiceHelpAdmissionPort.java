package me.yeonjae.ovlo.application.port.out.help;

import me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort;

/**
 * 도움말 AI 호출 예약. 국제처와 같은 전역 예산·회원 동시 1건 제한을 공유하되 일일 건수·예산은 따로 센다.
 * 유료 호출 전에 반드시 잡는다.
 */
public interface ServiceHelpAdmissionPort {
    InquiryAdmissionPort.Lease acquireServiceHelp(Long memberId);
}
