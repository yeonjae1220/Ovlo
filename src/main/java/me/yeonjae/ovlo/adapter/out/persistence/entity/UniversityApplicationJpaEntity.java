package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import me.yeonjae.ovlo.domain.calendar.model.*;
import java.time.Instant;

@Entity @Table(name = "university_application", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "university_id", "active_slot"}))
@Getter @Setter
public class UniversityApplicationJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(name = "member_id", nullable = false) private long memberId;
    @Column(name = "university_id", nullable = false) private long universityId;
    @Column(nullable = false, length = 200) private String department;
    @Column(nullable = false, length = 254) private String contactEmail;
    @Column(nullable = false, length = 2000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ApplicationStatus status;
    @Column(name = "active_slot") private Boolean activeSlot;
    @Column(length = 2000) private String reviewNote;
    private String reviewedBy;
    @Column(nullable = false) private Instant createdAt;
    private Instant reviewedAt;
    public UniversityApplication domain() {
        return UniversityApplication.restore(id, memberId, universityId, department, contactEmail, message, status, reviewNote, reviewedBy, createdAt, reviewedAt, version);
    }
}
