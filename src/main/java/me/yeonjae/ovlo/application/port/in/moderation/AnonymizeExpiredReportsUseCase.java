package me.yeonjae.ovlo.application.port.in.moderation;

/** 보유 기간(1년)이 지난 신고 기록을 익명화한다. @return 익명화한 건수 */
public interface AnonymizeExpiredReportsUseCase {
    int anonymizeExpired();
}
