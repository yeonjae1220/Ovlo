package me.yeonjae.ovlo.application.port.out.verification;

import java.time.Instant;

/** 오래된 인증 자격을 일괄 만료시키는 out-port. */
public interface ExpireVerificationCredentialPort {

    /**
     * verifiedAt이 cutoff 이전인 활성(VERIFIED) 자격을 EXPIRED로 일괄 전환한다.
     *
     * @return 만료 처리된 자격 수
     */
    int expireVerifiedOlderThan(Instant cutoff);
}
