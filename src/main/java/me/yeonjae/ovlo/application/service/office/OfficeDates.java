package me.yeonjae.ovlo.application.service.office;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** 국제처 파일럿의 날짜 정책. 전역 Clock이나 다른 도메인의 날짜 정책을 바꾸지 않는다. */
public final class OfficeDates {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private OfficeDates() {}
    public static LocalDate today(Clock clock) { return LocalDate.ofInstant(clock.instant(), ZONE); }
}
