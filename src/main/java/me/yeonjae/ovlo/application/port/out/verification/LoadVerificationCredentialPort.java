package me.yeonjae.ovlo.application.port.out.verification;

import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;

import java.util.List;

public interface LoadVerificationCredentialPort {

    List<VerificationCredential> findByMemberId(Long memberId);

    /** 다른 멤버가 이미 이 학교 이메일을 인증했는지 (학교 이메일 유일성). */
    boolean existsActiveByVerifiedEmailAndMemberIdNot(String verifiedEmail, Long memberId);
}
