package me.yeonjae.ovlo.domain.member.model;

import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniversityExperienceTest {

    private static final UniversityId UNIVERSITY_ID = new UniversityId(1L);
    private static final LocalDate START = LocalDate.of(2024, 9, 1);
    private static final LocalDate END = LocalDate.of(2025, 2, 28);

    @Test
    @DisplayName("대학 ID, 시작일, 종료일로 UniversityExperience를 생성할 수 있다")
    void shouldCreate_whenValidInputs() {
        UniversityExperience exp = new UniversityExperience(UNIVERSITY_ID, START, END);
        assertThat(exp.universityId()).isEqualTo(UNIVERSITY_ID);
        assertThat(exp.startDate()).isEqualTo(START);
        assertThat(exp.endDate()).isEqualTo(END);
    }

    @Test
    @DisplayName("종료일 없이(진행 중) 생성할 수 있다")
    void shouldCreate_whenNoEndDate() {
        UniversityExperience exp = new UniversityExperience(UNIVERSITY_ID, START, null);
        assertThat(exp.isOngoing()).isTrue();
    }

    @Test
    @DisplayName("종료일이 있으면 isOngoing()은 false이다")
    void shouldNotBeOngoing_whenEndDatePresent() {
        UniversityExperience exp = new UniversityExperience(UNIVERSITY_ID, START, END);
        assertThat(exp.isOngoing()).isFalse();
    }

    @Test
    @DisplayName("종료일이 시작일과 같으면 예외가 발생한다")
    void shouldThrow_whenEndDateEqualsStartDate() {
        assertThatThrownBy(() -> new UniversityExperience(UNIVERSITY_ID, START, START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료일은 시작일 이후여야 합니다");
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전이면 예외가 발생한다")
    void shouldThrow_whenEndDateBeforeStartDate() {
        assertThatThrownBy(() -> new UniversityExperience(UNIVERSITY_ID, START, START.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료일은 시작일 이후여야 합니다");
    }

    @Test
    @DisplayName("null 대학 ID로 생성 시 예외가 발생한다")
    void shouldThrow_whenNullUniversityId() {
        assertThatThrownBy(() -> new UniversityExperience(null, START, END))
                .isInstanceOf(NullPointerException.class);
    }
}
