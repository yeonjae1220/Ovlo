package me.yeonjae.ovlo.domain.member.model;

import me.yeonjae.ovlo.domain.university.model.UniversityId;

import java.time.LocalDate;
import java.util.Objects;

public record UniversityExperience(UniversityId universityId, LocalDate startDate, LocalDate endDate) {

    public UniversityExperience {
        Objects.requireNonNull(universityId, "대학 ID는 null일 수 없습니다");
        Objects.requireNonNull(startDate, "시작일은 null일 수 없습니다");
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다");
        }
    }

    public boolean isOngoing() {
        return endDate == null;
    }
}
