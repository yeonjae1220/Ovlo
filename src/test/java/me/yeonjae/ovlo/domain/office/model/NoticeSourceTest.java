package me.yeonjae.ovlo.domain.office.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NoticeSourceTest {
    @Test void webSourceNeedsAnApprovedScope() {
        assertThatThrownBy(() -> new NoticeSource(NoticeSource.Type.UNIVERSITY_WEB, "국제처", "공식 FAQ",
                "교환학생", "서울", LocalDate.of(2026, 11, 13), null, 1L, false, true, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void reviewPeriodIsBoundedByExpiry() {
        NoticeSource source = new NoticeSource(NoticeSource.Type.OFFICE_MATERIAL, "국제처", "안내문 1호",
                "교환학생", "서울", LocalDate.of(2026, 11, 13), null, 1L, true, true, null, List.of());
        assertThat(source.nextReview(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 30)))
                .isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(source.nextReview(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 15)))
                .isEqualTo(LocalDate.of(2026, 11, 15));
    }
    @Test void scopeMatchesOriginAndPathBoundary() {
        assertThat(NoticeSource.withinScope("https://u.ac.kr/office/faq?a=1", "https://u.ac.kr/office")).isTrue();
        assertThat(NoticeSource.withinScope("https://u.ac.kr/office-fake", "https://u.ac.kr/office")).isFalse();
        assertThat(NoticeSource.withinScope("https://u.ac.kr.evil.org/office", "https://u.ac.kr/office")).isFalse();
        assertThat(NoticeSource.withinScope("https://u.ac.kr/office/../private", "https://u.ac.kr/office")).isFalse();
    }
}
