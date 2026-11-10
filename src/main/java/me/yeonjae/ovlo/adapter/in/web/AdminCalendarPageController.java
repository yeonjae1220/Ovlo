package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.*;
import me.yeonjae.ovlo.application.dto.result.CalendarEventResult;
import me.yeonjae.ovlo.application.port.in.calendar.*;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
import me.yeonjae.ovlo.domain.calendar.exception.CalendarException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import java.time.*;
import java.util.*;

/** Uses the existing /admin session chain and CSRF protection, never a public actor parameter. */
@Controller @RequestMapping("/admin/calendar")
public class AdminCalendarPageController {
    private final CalendarUseCase commands;
    private final CalendarQuery queries;
    private final UniversityApplicationUseCase applications;
    private final SearchUniversityQuery universities;
    public AdminCalendarPageController(CalendarUseCase commands, CalendarQuery queries, UniversityApplicationUseCase applications, SearchUniversityQuery universities) {
        this.commands = commands; this.queries = queries; this.applications = applications; this.universities = universities;
    }
    @GetMapping public String page(Authentication auth, Model model,
            @RequestParam(required=false) Long schoolId, @RequestParam(defaultValue="") String q,
            @RequestParam(required=false) String month, @RequestParam(defaultValue="PENDING") String status,
            @RequestParam(defaultValue="0") int page, @RequestParam(required=false) Long edit) {
        var actor = CalendarActor.administrator(auth.getName());
        var ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        var events = schoolId == null ? List.<CalendarEventResult>of() : queries.management(actor, schoolId, ym.atDay(1), ym.plusMonths(1).atDay(1), "UTC");
        model.addAttribute("events", events); model.addAttribute("schoolId", schoolId); model.addAttribute("month", ym.toString());
        model.addAttribute("q", q); model.addAttribute("schools", q.isBlank() ? List.of() : universities.search(new SearchUniversityCommand(q, null, 0, 20)).content());
        model.addAttribute("applications", applications.list(actor, status, page)); model.addAttribute("status", status); model.addAttribute("page", page);
        model.addAttribute("reviewBefore", LocalDate.now().minusDays(14)); model.addAttribute("today", LocalDate.now());
        if (!model.containsAttribute("form")) model.addAttribute("form", form(events.stream().filter(e -> Objects.equals(e.id(), edit)).findFirst().orElse(null)));
        return "admin/calendar";
    }
    @PostMapping("/applications/{id}") public String review(Authentication auth, @PathVariable long id,
            @RequestParam String action, @RequestParam String note, @RequestParam long version, RedirectAttributes flash) {
        try { applications.review(CalendarActor.administrator(auth.getName()), id, action, note, version); flash.addFlashAttribute("success", "신청을 처리했습니다"); }
        catch (CalendarException | IllegalArgumentException | IllegalStateException e) { flash.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/calendar";
    }
    @PostMapping("/events") public String save(Authentication auth, @RequestParam Map<String,String> fields, RedirectAttributes flash) {
        long schoolId = Long.parseLong(fields.get("schoolId"));
        try {
            var c = command(fields);
            if (fields.getOrDefault("id", "").isBlank()) commands.createSchool(CalendarActor.administrator(auth.getName()), schoolId, c);
            else commands.updateSchool(CalendarActor.administrator(auth.getName()), Long.parseLong(fields.get("id")), c);
            flash.addFlashAttribute("success", "저장했습니다. 초안은 게시 버튼을 눌러 학생에게 공개하세요.");
        } catch (CalendarException | IllegalArgumentException | IllegalStateException | DateTimeException e) {
            flash.addFlashAttribute("error", e.getMessage()); flash.addFlashAttribute("form", fields);
        }
        flash.addAttribute("schoolId", schoolId); flash.addAttribute("month", fields.get("month"));
        return "redirect:/admin/calendar";
    }
    @PostMapping("/events/{id}/status") public String status(Authentication auth, @PathVariable long id, @RequestParam String action,
            @RequestParam long version, @RequestParam long schoolId, @RequestParam String month, RedirectAttributes flash) {
        try {
            if ("delete".equals(action)) commands.deleteDraft(CalendarActor.administrator(auth.getName()), id, version);
            else commands.changeStatus(CalendarActor.administrator(auth.getName()), id, action, version);
            flash.addFlashAttribute("success", "일정 상태를 변경했습니다");
        } catch (CalendarException | IllegalArgumentException | IllegalStateException e) { flash.addFlashAttribute("error", e.getMessage()); }
        flash.addAttribute("schoolId", schoolId); flash.addAttribute("month", month); return "redirect:/admin/calendar";
    }
    private SaveCalendarEventCommand command(Map<String,String> f) {
        boolean allDay = "true".equals(f.get("allDay")); String zone = f.get("timeZone");
        return new SaveCalendarEventCommand(f.get("title"), f.get("description"), f.get("category"), allDay,
                allDay ? LocalDate.parse(f.get("startDate")) : null, allDay ? LocalDate.parse(f.get("endDate")).plusDays(1) : null,
                allDay ? null : time(f.get("startsAt"), zone), allDay ? null : time(f.get("endsAt"), zone), zone,
                f.get("location"), f.get("actionUrl"), time(f.get("deadlineAt"), zone), f.get("audience"),
                f.get("sourceUrl"), f.getOrDefault("sourceCheckedOn", "").isBlank() ? null : LocalDate.parse(f.get("sourceCheckedOn")),
                f.getOrDefault("version", "").isBlank() ? null : Long.valueOf(f.get("version")));
    }
    private Instant time(String value, String zone) {
        if (value == null || value.isBlank()) return null;
        var local = LocalDateTime.parse(value); var zoneId = ZoneId.of(zone);
        var offsets = zoneId.getRules().getValidOffsets(local);
        if (offsets.size() != 1) throw new IllegalArgumentException("서머타임 전환 시각입니다. 명확한 시각을 선택해 주세요");
        return local.toInstant(offsets.getFirst());
    }
    private Map<String,String> form(CalendarEventResult e) {
        Map<String,String> f = new HashMap<>(); f.put("allDay", "true"); f.put("timeZone", "Asia/Seoul"); f.put("category", "ADMINISTRATION");
        if (e == null) return f;
        var d = e.details(); var p = d.period(); var zone = ZoneId.of(p.timeZone());
        f.put("id", e.id().toString()); f.put("version", Long.toString(e.version())); f.put("title", d.title()); f.put("description", d.description());
        f.put("category", d.category().name()); f.put("allDay", Boolean.toString(p.allDay())); f.put("timeZone", p.timeZone());
        f.put("startDate", p.allDay() ? p.startDate().toString() : ""); f.put("endDate", p.allDay() ? p.endDate().minusDays(1).toString() : "");
        f.put("startsAt", p.allDay() ? "" : p.startsAt().atZone(zone).toLocalDateTime().toString());
        f.put("endsAt", p.allDay() ? "" : p.endsAt().atZone(zone).toLocalDateTime().toString());
        f.put("location", d.location()); f.put("audience", d.audience()); f.put("actionUrl", d.actionUrl());
        f.put("deadlineAt", d.deadlineAt() == null ? "" : d.deadlineAt().atZone(zone).toLocalDateTime().toString());
        if (e.source() != null) { f.put("sourceUrl", e.source().url()); f.put("sourceCheckedOn", e.source().checkedOn().toString()); }
        return f;
    }
}
