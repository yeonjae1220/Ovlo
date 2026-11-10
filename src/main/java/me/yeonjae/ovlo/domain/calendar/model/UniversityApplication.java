package me.yeonjae.ovlo.domain.calendar.model;

import java.time.Instant;
import me.yeonjae.ovlo.domain.calendar.exception.CalendarStateException;

public final class UniversityApplication {
    private final Long id;
    private final long memberId, universityId;
    private final String department, contactEmail, message;
    private ApplicationStatus status;
    private String reviewNote, reviewedBy;
    private final Instant createdAt;
    private Instant reviewedAt;
    private final long version;
    private UniversityApplication(Long id, long memberId, long universityId, String department, String contactEmail,
                                  String message, ApplicationStatus status, String reviewNote, String reviewedBy,
                                  Instant createdAt, Instant reviewedAt, long version) {
        if (memberId <= 0 || universityId <= 0 || department == null || department.isBlank() || department.length() > 200
                || contactEmail == null || contactEmail.length() > 254 || !contactEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || message == null || message.isBlank() || message.length() > 2000) throw new IllegalArgumentException("학교 담당자 신청 정보를 확인해 주세요");
        this.id = id; this.memberId = memberId; this.universityId = universityId; this.department = department;
        this.contactEmail = contactEmail; this.message = message; this.status = status; this.reviewNote = reviewNote;
        this.reviewedBy = reviewedBy; this.createdAt = createdAt; this.reviewedAt = reviewedAt; this.version = version;
    }
    public static UniversityApplication apply(long memberId, long universityId, String department, String contactEmail, String message) {
        return new UniversityApplication(null, memberId, universityId, department, contactEmail, message, ApplicationStatus.PENDING, null, null, Instant.now(), null, 0);
    }
    public static UniversityApplication restore(Long id, long memberId, long universityId, String department, String contactEmail,
            String message, ApplicationStatus status, String note, String reviewer, Instant created, Instant reviewed, long version) {
        return new UniversityApplication(id, memberId, universityId, department, contactEmail, message, status, note, reviewer, created, reviewed, version);
    }
    public void review(boolean approved, String note, String reviewer) {
        if (status != ApplicationStatus.PENDING) throw new CalendarStateException("대기 중인 신청만 심사할 수 있습니다");
        recordReview(note, reviewer); status = approved ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED;
    }
    public void revoke(String note, String reviewer) {
        if (status != ApplicationStatus.APPROVED) throw new CalendarStateException("승인된 신청만 권한을 해제할 수 있습니다");
        recordReview(note, reviewer); status = ApplicationStatus.REVOKED;
    }
    private void recordReview(String note, String reviewer) {
        if (note == null || note.isBlank() || note.length() > 2000 || reviewer == null || reviewer.isBlank())
            throw new IllegalArgumentException("검토 근거 또는 사유를 입력해 주세요");
        reviewNote = note; reviewedBy = reviewer; reviewedAt = Instant.now();
    }
    public Long id() { return id; } public long memberId() { return memberId; } public long universityId() { return universityId; }
    public String department() { return department; } public String contactEmail() { return contactEmail; } public String message() { return message; }
    public ApplicationStatus status() { return status; } public String reviewNote() { return reviewNote; } public String reviewedBy() { return reviewedBy; }
    public Instant createdAt() { return createdAt; } public Instant reviewedAt() { return reviewedAt; } public long version() { return version; }
}
