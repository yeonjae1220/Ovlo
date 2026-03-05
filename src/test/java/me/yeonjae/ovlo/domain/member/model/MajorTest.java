package me.yeonjae.ovlo.domain.member.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MajorTest {

    @Test
    @DisplayName("전공명, 학위 과정, 학년으로 Major를 생성할 수 있다")
    void shouldCreate_whenValidInputs() {
        Major major = new Major("Computer Science", DegreeType.BACHELOR, 3);
        assertThat(major.majorName()).isEqualTo("Computer Science");
        assertThat(major.degreeType()).isEqualTo(DegreeType.BACHELOR);
        assertThat(major.gradeLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("학위 과정 + 학년 표기를 반환한다 (예: B3, M1)")
    void shouldDisplayGrade() {
        assertThat(new Major("CS", DegreeType.BACHELOR, 3).displayGrade()).isEqualTo("B3");
        assertThat(new Major("CS", DegreeType.MASTER, 1).displayGrade()).isEqualTo("M1");
        assertThat(new Major("CS", DegreeType.DOCTOR, 2).displayGrade()).isEqualTo("D2");
    }

    @Test
    @DisplayName("같은 값의 Major는 동일하다")
    void shouldBeEqual_whenSameValues() {
        assertThat(new Major("CS", DegreeType.BACHELOR, 3))
                .isEqualTo(new Major("CS", DegreeType.BACHELOR, 3));
    }

    @Test
    @DisplayName("null 전공명으로 생성 시 예외가 발생한다")
    void shouldThrow_whenNullMajorName() {
        assertThatThrownBy(() -> new Major(null, DegreeType.BACHELOR, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("빈 전공명으로 생성 시 예외가 발생한다")
    void shouldThrow_whenBlankMajorName() {
        assertThatThrownBy(() -> new Major("  ", DegreeType.BACHELOR, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전공명은 빈 값일 수 없습니다");
    }

    @Test
    @DisplayName("0 이하의 학년으로 생성 시 예외가 발생한다")
    void shouldThrow_whenZeroOrNegativeGrade() {
        assertThatThrownBy(() -> new Major("CS", DegreeType.BACHELOR, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("학년은 1 이상이어야 합니다");
    }
}
