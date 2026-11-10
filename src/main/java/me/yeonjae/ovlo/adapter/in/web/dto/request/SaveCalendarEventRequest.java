package me.yeonjae.ovlo.adapter.in.web.dto.request;
import jakarta.validation.constraints.*;
import me.yeonjae.ovlo.application.dto.command.SaveCalendarEventCommand;
import java.time.*;
public record SaveCalendarEventRequest(
        @NotBlank @Size(max=200) String title, @Size(max=10000) String description,
        @NotBlank @Pattern(regexp="ADMINISTRATION|ACADEMIC|EVENT|OTHER") String category, boolean allDay,
        LocalDate startDate, LocalDate endDate, Instant startsAt, Instant endsAt,
        @NotBlank @Size(max=100) String timeZone, @Size(max=300) String location, @Size(max=2000) String actionUrl,
        Instant deadlineAt, @Size(max=500) String audience, @Size(max=2000) String sourceUrl, LocalDate sourceCheckedOn, @PositiveOrZero Long version) {
    public SaveCalendarEventCommand command() {
        return new SaveCalendarEventCommand(title, description, category, allDay, startDate, endDate, startsAt, endsAt, timeZone,
                location, actionUrl, deadlineAt, audience, sourceUrl, sourceCheckedOn, version);
    }
}
