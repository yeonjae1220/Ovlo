package me.yeonjae.ovlo.application.port.out.office;

public interface RevokeOfficeChatAccessPort {
    /** Remove staff from all consultations, preserving their own student consultations and message history. */
    void revoke(Long officeId, Long memberId);
}
