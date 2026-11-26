package me.yeonjae.ovlo.application.dto.command;

/**
 * 관리자가 회원을 대신 탈퇴시키는 명령 — Apple 재인증을 할 수 없는 회원(Apple ID 분실·Apple 장애)의 대체 경로.
 *
 * @param reason    처리 근거(문의 번호 등). 감사 로그에 남는다.
 * @param adminName 처리한 관리자 계정 이름
 */
public record AdminWithdrawMemberCommand(Long memberId, String reason, String adminName) {
}
