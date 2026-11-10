package me.yeonjae.ovlo.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import me.yeonjae.ovlo.adapter.in.web.dto.request.SaveCalendarEventRequest;
import me.yeonjae.ovlo.application.dto.command.CalendarActor;
import me.yeonjae.ovlo.application.dto.result.*;
import me.yeonjae.ovlo.application.port.in.calendar.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/v1/calendar") @Validated
public class CalendarApiController {
    private final CalendarUseCase commands;
    private final CalendarQuery queries;
    private final UniversityApplicationUseCase applications;
    public CalendarApiController(CalendarUseCase commands, CalendarQuery queries, UniversityApplicationUseCase applications) {
        this.commands = commands; this.queries = queries; this.applications = applications;
    }
    @GetMapping public List<CalendarEventResult> calendar(@AuthenticationPrincipal Long memberId,
            @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam(defaultValue="UTC") String timeZone) {
        return queries.calendar(memberId, from, to, timeZone);
    }
    @GetMapping("/options") public CalendarOptionsResult options(@AuthenticationPrincipal Long memberId) { return queries.options(memberId); }
    @PutMapping("/subscriptions/{schoolId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal Long memberId, @PathVariable long schoolId) { commands.subscribe(memberId, schoolId, true); }
    @DeleteMapping("/subscriptions/{schoolId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@AuthenticationPrincipal Long memberId, @PathVariable long schoolId) { commands.subscribe(memberId, schoolId, false); }
    @PostMapping("/personal") @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventResult createPersonal(@AuthenticationPrincipal Long memberId, @Valid @RequestBody SaveCalendarEventRequest r) { return commands.createPersonal(memberId, r.command()); }
    @PutMapping("/personal/{id}")
    public CalendarEventResult updatePersonal(@AuthenticationPrincipal Long memberId, @PathVariable long id, @Valid @RequestBody SaveCalendarEventRequest r) { return commands.updatePersonal(memberId, id, r.command()); }
    @DeleteMapping("/personal/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePersonal(@AuthenticationPrincipal Long memberId, @PathVariable long id, @RequestParam long version) { commands.deletePersonal(memberId, id, version); }
    @GetMapping("/schools/{schoolId}/events")
    public List<CalendarEventResult> manage(@AuthenticationPrincipal Long memberId, @PathVariable long schoolId,
            @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam(defaultValue="UTC") String timeZone) {
        return queries.management(CalendarActor.member(memberId), schoolId, from, to, timeZone);
    }
    @PostMapping("/schools/{schoolId}/events") @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventResult createSchool(@AuthenticationPrincipal Long memberId, @PathVariable long schoolId, @Valid @RequestBody SaveCalendarEventRequest r) {
        return commands.createSchool(CalendarActor.member(memberId), schoolId, r.command());
    }
    @PutMapping("/school-events/{id}")
    public CalendarEventResult updateSchool(@AuthenticationPrincipal Long memberId, @PathVariable long id, @Valid @RequestBody SaveCalendarEventRequest r) {
        return commands.updateSchool(CalendarActor.member(memberId), id, r.command());
    }
    public record StatusRequest(@NotBlank @Pattern(regexp="publish|cancel") String action, @NotNull @PositiveOrZero Long version) {}
    @PostMapping("/school-events/{id}/status")
    public CalendarEventResult status(@AuthenticationPrincipal Long memberId, @PathVariable long id, @Valid @RequestBody StatusRequest r) {
        return commands.changeStatus(CalendarActor.member(memberId), id, r.action(), r.version());
    }
    @DeleteMapping("/school-events/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@AuthenticationPrincipal Long memberId, @PathVariable long id, @RequestParam long version) {
        commands.deleteDraft(CalendarActor.member(memberId), id, version);
    }
    public record ApplicationRequest(@NotNull @Positive Long universityId, @NotBlank @Size(max=200) String department,
            @NotBlank @Email @Size(max=254) String contactEmail, @NotBlank @Size(max=2000) String message) {}
    @PostMapping("/applications") @ResponseStatus(HttpStatus.CREATED)
    public UniversityApplicationResult apply(@AuthenticationPrincipal Long memberId, @Valid @RequestBody ApplicationRequest r) {
        return applications.apply(memberId, r.universityId(), r.department(), r.contactEmail(), r.message());
    }
    @GetMapping("/applications") public List<UniversityApplicationResult> mine(@AuthenticationPrincipal Long memberId) { return applications.mine(memberId); }
}
