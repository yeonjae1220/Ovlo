package me.yeonjae.ovlo.domain.calendar.model;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class CalendarDomainTest {
    @Test void allDayDatesAreExclusiveAndDoNotDependOnViewerTimezone() {
        var period = EventPeriod.allDay(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 16), "Asia/Seoul");
        assertThat(period.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(period.endDate()).isEqualTo(LocalDate.of(2026, 3, 16));
        assertThatThrownBy(() -> EventPeriod.allDay(period.endDate(), period.startDate(), "Asia/Seoul"))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void timedPeriodMustHaveValidZoneAndPositiveDuration() {
        var start = Instant.parse("2026-03-01T10:00:00Z");
        assertThatThrownBy(() -> EventPeriod.timed(start, start, "Europe/Paris"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventPeriod.timed(start, start.plusSeconds(60), "unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void curatedEventRequiresSourceAndKeepsProvenanceWhenEdited() {
        assertThatThrownBy(() -> UniversityEvent.create(1, details(), EventOrigin.OVLO_CURATED, null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        var source = new EventSource("https://university.example.edu/notice/1", LocalDate.now());
        var event = UniversityEvent.create(1, details(), EventOrigin.OVLO_CURATED, source, 10);
        event.publish();
        event.edit(details(), source, 20);
        assertThat(event.origin()).isEqualTo(EventOrigin.OVLO_CURATED);
        assertThat(event.createdBy()).isEqualTo(10);
        assertThat(event.updatedBy()).isEqualTo(20);
        event.cancel();
        assertThatThrownBy(event::publish).isInstanceOf(IllegalStateException.class);
    }
    @Test void unsafeLinksAndFutureSourceChecksAreRejected() {
        assertThatThrownBy(() -> new EventSource("javascript:alert(1)", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventSource("https://example.edu", LocalDate.now().plusDays(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void applicationHasExplicitReviewLifecycle() {
        var application = UniversityApplication.apply(10, 1, "International Office", "staff@example.edu", "Calendar manager");
        assertThat(application.status()).isEqualTo(ApplicationStatus.PENDING);
        application.review(true, "Verified through school office", "admin@example.com");
        assertThat(application.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThatThrownBy(() -> application.review(false, "No", "admin"))
                .isInstanceOf(IllegalStateException.class);
        application.revoke("Staff changed", "admin");
        assertThat(application.status()).isEqualTo(ApplicationStatus.REVOKED);
    }
    private EventDetails details() {
        return new EventDetails("Visa application", "Bring documents", EventCategory.ADMINISTRATION,
                EventPeriod.allDay(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 16), "Asia/Seoul"),
                "International Office", "https://example.edu/apply", null, "Exchange students");
    }
}
