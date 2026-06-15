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

    /**
     * 단일 자격을 EXPIRED로 전환한다(관리자 취소). memberId로 소유 범위를 한정하고
     * 취소자(revokedBy)·취소시각(revokedAt)을 감사용으로 기록한다.
     *
     * @return 영향 행 수(0이면 미존재/소유자 불일치/이미 만료)
     */
    int revokeByIdAndMemberId(Long credentialId, Long memberId, String revokedBy, Instant revokedAt);
}
