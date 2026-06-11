package me.yeonjae.ovlo.domain.university.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * University는 global_universities 카탈로그의 읽기 전용 복원 모델이다.
 * (외부 적재 데이터 → restore 로만 복원, 도메인 생성/변경 없음)
 */
class UniversityTest {

    private final CountryCode korea = new CountryCode("KR");

    @Test
    @DisplayName("ID·필드와 함께 복원할 수 있다")
    void shouldRestore() {
        University university = University.restore(
                new UniversityId(1L),
                "Seoul National University",
                "서울대학교",
                "대한민국",
                "South Korea",
                korea,
                "Seoul",
                new GeoLocation(37.4600, 126.9514),
                "https://www.snu.ac.kr",
                "snu.ac.kr");

        assertThat(university.getId()).isEqualTo(new UniversityId(1L));
        assertThat(university.getName()).isEqualTo("Seoul National University");
        assertThat(university.getLocalName()).isEqualTo("서울대학교");
        assertThat(university.getCountry()).isEqualTo("대한민국");
        assertThat(university.getCountryEn()).isEqualTo("South Korea");
        assertThat(university.getCountryCode()).isEqualTo(korea);
        assertThat(university.getCity()).isEqualTo("Seoul");
        assertThat(university.getWebsiteUrl()).isEqualTo("https://www.snu.ac.kr");
        assertThat(university.getDomain()).isEqualTo("snu.ac.kr");
    }

    @Test
    @DisplayName("좌표가 있으면 hasCoordinates 가 true 다")
    void hasCoordinates_true_whenGeoLocationPresent() {
        University university = University.restore(
                new UniversityId(1L), "SNU", null, null, null, korea, "Seoul",
                new GeoLocation(37.46, 126.95), null, "snu.ac.kr");

        assertThat(university.hasCoordinates()).isTrue();
    }

    @Test
    @DisplayName("좌표가 없으면 hasCoordinates 가 false 다 (10k 카탈로그 대부분)")
    void hasCoordinates_false_whenNoGeoLocation() {
        University university = University.restore(
                new UniversityId(2L), "Some Univ", null, null, null, korea, null,
                null, null, null);

        assertThat(university.hasCoordinates()).isFalse();
        assertThat(university.getGeoLocation()).isNull();
    }
}
