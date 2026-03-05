package me.yeonjae.ovlo.domain.member.model;

import java.util.Objects;

public record Major(String majorName, DegreeType degreeType, int gradeLevel) {

    public Major {
        Objects.requireNonNull(majorName, "전공명은 null일 수 없습니다");
        if (majorName.isBlank()) {
            throw new IllegalArgumentException("전공명은 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(degreeType, "학위 과정은 null일 수 없습니다");
        if (gradeLevel < 1) {
            throw new IllegalArgumentException("학년은 1 이상이어야 합니다: " + gradeLevel);
        }
    }

    public String displayGrade() {
        return degreeType.displayGrade(gradeLevel);
    }
}
