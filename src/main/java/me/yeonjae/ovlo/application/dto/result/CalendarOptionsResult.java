package me.yeonjae.ovlo.application.dto.result;
import java.util.List;
public record CalendarOptionsResult(String role, List<School> subscriptions, List<School> suggestions, List<School> managedSchools) {
    public record School(long id, String name) {}
}
