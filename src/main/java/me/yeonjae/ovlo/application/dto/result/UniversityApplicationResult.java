package me.yeonjae.ovlo.application.dto.result;
import me.yeonjae.ovlo.domain.calendar.model.UniversityApplication;
import java.time.Instant;
public record UniversityApplicationResult(Long id, long memberId, String applicantName, long universityId, String universityName,
        String department, String contactEmail, String message, String status, String reviewNote, Instant createdAt, long version) {
    public static UniversityApplicationResult from(UniversityApplication a, String memberName, String schoolName) {
        return new UniversityApplicationResult(a.id(), a.memberId(), memberName, a.universityId(), schoolName, a.department(), a.contactEmail(),
                a.message(), a.status().name(), a.reviewNote(), a.createdAt(), a.version());
    }
}
