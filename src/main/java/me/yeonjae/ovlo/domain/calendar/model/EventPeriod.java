package me.yeonjae.ovlo.domain.calendar.model;

import java.time.*;

/** All-day endDate and timed endsAt are exclusive. Dates are never converted to UTC. */
public record EventPeriod(boolean allDay, LocalDate startDate, LocalDate endDate,
                          Instant startsAt, Instant endsAt, String timeZone) {
    public EventPeriod {
        try { ZoneId.of(timeZone); }
        catch (Exception e) { throw new IllegalArgumentException("올바른 시간대를 선택해 주세요"); }
        if (allDay) {
            if (startDate == null || endDate == null || !endDate.isAfter(startDate) || startsAt != null || endsAt != null)
                throw new IllegalArgumentException("종일 일정의 종료일은 시작일 이후여야 합니다 (종료일 미포함)");
        } else if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt) || startDate != null || endDate != null) {
            throw new IllegalArgumentException("종료 시각은 시작 시각 이후여야 합니다");
        }
    }
    public static EventPeriod allDay(LocalDate start, LocalDate end, String zone) {
        return new EventPeriod(true, start, end, null, null, zone);
    }
    public static EventPeriod timed(Instant start, Instant end, String zone) {
        return new EventPeriod(false, null, null, start, end, zone);
    }
}
