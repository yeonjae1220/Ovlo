package me.yeonjae.ovlo.application.port.out.calendar;
import me.yeonjae.ovlo.domain.calendar.model.*;
import java.util.*;
public interface CalendarMembershipPort {
    List<Long> subscriptions(long memberId);
    void subscribe(long memberId, long schoolId);
    void unsubscribe(long memberId, long schoolId);
    List<Long> managedSchools(long memberId);
    boolean manages(long memberId, long schoolId);
    void grant(long memberId, long schoolId);
    void revoke(long memberId, long schoolId);
    UniversityApplication saveApplication(UniversityApplication application);
    Optional<UniversityApplication> findApplication(long id);
    boolean hasOpenApplication(long memberId, long schoolId);
    List<UniversityApplication> applications(Long memberId, ApplicationStatus status, int page, int size);
}
