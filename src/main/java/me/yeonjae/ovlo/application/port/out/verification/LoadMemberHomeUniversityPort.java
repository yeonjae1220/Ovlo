package me.yeonjae.ovlo.application.port.out.verification;

import java.util.Optional;

/**
 * 멤버의 본교 대학 ID를 조회하는 좁은 out-port (verification BC 전용).
 * TrustLevel 파생 시 "인증한 대학이 본교와 다른가(=파견)"를 판정하기 위해 사용한다.
 */
public interface LoadMemberHomeUniversityPort {

    /** 멤버의 본교 대학 ID. 본교 미설정(OAuth 미온보딩)이거나 멤버가 없으면 empty. */
    Optional<Long> findHomeUniversityId(Long memberId);
}
