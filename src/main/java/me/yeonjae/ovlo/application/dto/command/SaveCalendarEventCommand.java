package me.yeonjae.ovlo.application.dto.command;

import java.time.*;
/** Boundary validation belongs to the web request; domain validation is performed by the service. */
public record SaveCalendarEventCommand(String title, String description, String category, boolean allDay,
        LocalDate startDate, LocalDate endDate, Instant startsAt, Instant endsAt, String timeZone,
        String location, String actionUrl, Instant deadlineAt, String audience,
        String sourceUrl, LocalDate sourceCheckedOn, Long version) {}
