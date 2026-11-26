package me.yeonjae.ovlo.application.dto.command;

/** @param apple Apple 로그인 회원의 재인증 결과. 다른 회원은 null. */
public record WithdrawMemberCommand(Long memberId, AppleReauthorization apple) {

    public WithdrawMemberCommand(Long memberId) {
        this(memberId, null);
    }
}
