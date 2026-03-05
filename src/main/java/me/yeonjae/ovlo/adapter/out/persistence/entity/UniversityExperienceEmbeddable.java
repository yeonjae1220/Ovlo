package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class UniversityExperienceEmbeddable {

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected UniversityExperienceEmbeddable() {}

    public UniversityExperienceEmbeddable(Long universityId, LocalDate startDate, LocalDate endDate) {
        this.universityId = universityId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getUniversityId() { return universityId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
