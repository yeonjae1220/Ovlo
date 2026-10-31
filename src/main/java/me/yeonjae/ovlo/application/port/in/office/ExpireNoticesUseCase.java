package me.yeonjae.ovlo.application.port.in.office;

/**
 * 만료 스케줄러가 부르는 유스케이스. 대상 목록을 돌며 **건별 트랜잭션**으로 처리해 한 건의 실패가
 * 나머지를 되돌리지 않게 한다(GLOBAL-PIT-128).
 */
public interface ExpireNoticesUseCase {
    /** expiresAt 이 지난 APPROVED 를 EXPIRED 로. 반환 = 바뀐 건수. */
    int expireDue();

    /** 만료 7일 전 담당자 알림. 반환 = 알린 건수. */
    int warnExpiringSoon();
}
