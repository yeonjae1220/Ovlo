package me.yeonjae.ovlo.application.port.in.verification;

/** 만료 기한이 지난 인증 자격을 정리하는 유스케이스(스케줄러가 호출). */
public interface ExpireStaleCredentialsUseCase {

    /**
     * TTL이 지난 활성 자격을 EXPIRED로 전환한다.
     *
     * @return 만료 처리된 자격 수
     */
    int expireStale();
}
