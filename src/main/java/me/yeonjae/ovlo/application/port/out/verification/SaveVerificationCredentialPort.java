package me.yeonjae.ovlo.application.port.out.verification;

import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;

public interface SaveVerificationCredentialPort {
    /** 동일 멤버·타입의 기존 활성 자격을 대체(upsert)하며 저장한다. UNIQUE(member_id, type). */
    VerificationCredential save(VerificationCredential credential);
}
